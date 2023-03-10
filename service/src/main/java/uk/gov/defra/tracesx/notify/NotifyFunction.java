package uk.gov.defra.tracesx.notify;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.defra.tracesx.notify.apiclient.TradePlatformApiClient;
import uk.gov.defra.tracesx.notify.apimodel.SuccessResponse;
import uk.gov.defra.tracesx.notify.apimodel.Template;
import uk.gov.defra.tracesx.notify.model.MessageType;
import uk.gov.defra.tracesx.notify.model.QueueMessage;
import uk.gov.defra.tracesx.notify.service.TradePlatformTokenGeneratorService;
import uk.gov.defra.tracesx.notify.transformer.MessageTransformer;
import uk.gov.defra.tracesx.notify.transformer.MessageTransformerFactory;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotifyFunction {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @FunctionName("NotifyFunction")
  public void notifyTextOrEmail(
      @ServiceBusQueueTrigger(name = "message",
          queueName = "%NOTIFY_QUEUE_NAME%",
          connection = "NOTIFY_QUEUE_CONNECTION_NAME")
      String message,
      final ExecutionContext context) throws JsonProcessingException {

    Logger logger = context.getLogger();
    logger.info("NotifyFunction starting");

    final NotifyProperties notifyProperties = NotifyProperties.properties(logger);

    logger.info(() -> String.format(
        "Started IPAFFS service notify-microservice %s",
        notifyProperties.getVersion()));

    QueueMessage queueMessage = objectMapper.readValue(message, QueueMessage.class);
    logger.info(() -> String.format(
        "Notification reference received from queue %s, with template ID %s",
        queueMessage.getMessagePersonalisation().getReferenceNumber(),
        queueMessage.getMessageTemplateId()
    ));

    if (isEligibleForNotification(notifyProperties, queueMessage)) {
      submitForNotification(logger, notifyProperties, queueMessage);
    } else {
      logNonSubmittedMessage(logger, queueMessage);
    }
    logger.info("NotifyFunction stopping");
  }

  private void logNonSubmittedMessage(Logger logger, QueueMessage queueMessage) {
    logMessage(logger, queueMessage, queueMessage.getEmails(), "emails");
    logMessage(logger, queueMessage, queueMessage.getPhoneNumbers(), "phone numbers");
  }

  private static void logMessage(Logger logger, QueueMessage queueMessage,
      List<String> contactsDetails,
      String type) {
    if (contactsDetails != null) {
      logger.info(() -> String.format(
          "Message not eligible for sending, message had %d %s for template "
              + "ID %s and notification reference %s",
          contactsDetails.size(),
          type,
          queueMessage.getMessageTemplateId(),
          queueMessage.getMessagePersonalisation().getReferenceNumber()
      ));
    }
  }

  private boolean isEligibleForNotification(final NotifyProperties notifyProperties,
      final QueueMessage queueMessage) {
    return isEligibleForMessageType(MessageType.EMAIL, queueMessage,
        notifyProperties.getEnableEmailNotification())
        || isEligibleForMessageType(MessageType.TEXT, queueMessage,
        notifyProperties.getEnableTextNotification());
  }

  private boolean isEligibleForMessageType(final MessageType messageType,
      final QueueMessage queueMessage,
      final String enableTextNotificationViaTradePlatform) {
    return queueMessage.getMessageType().equals(messageType) && parseBoolean(
        enableTextNotificationViaTradePlatform);
  }

  private void submitForNotification(final Logger logger, final NotifyProperties notifyProperties,
      final QueueMessage queueMessage) {
    final TradePlatformApiClient tradePlatformApiClient = tradePlatformApiClient(notifyProperties,
        logger);
    final MessageTransformer messageTransformer = MessageTransformerFactory.getTransformer(
        queueMessage.getMessageType());
    final Template template = messageTransformer.transform(queueMessage,
        notifyProperties.getTradePlatformSystemName(),
        notifyProperties.getTradePlatformSystemUniqueId());
    SuccessResponse successResponse = tradePlatformApiClient.submitRequest(template);
    logger.info(() -> String.format("Successfully sent message to Notify API (reference %s) for"
            + " processing of notification -->> %s", successResponse.getReference(),
        template.getReference()));
  }

  protected TradePlatformApiClient tradePlatformApiClient(
      final NotifyProperties properties, final Logger logger) {
    final WebClient webClient = getWebClientBuild(logger);
    final String token = tradePlatformTokenGeneratorService(properties, logger).generateToken();
    return new TradePlatformApiClient(
        logger,
        webClient,
        token,
        URI.create(properties.getTradePlatformNotifyUrl()),
        properties.getTradePlatformSubscriptionKey()
    );
  }

  protected WebClient getWebClientBuild(Logger logger) {
    return WebClient.builder()
        .filter(logResponse(logger))
        .build();
  }

  protected TradePlatformTokenGeneratorService tradePlatformTokenGeneratorService(
      final NotifyProperties properties, final Logger logger) {
    final WebClient webClient = getWebClient();
    return new TradePlatformTokenGeneratorService(
        logger,
        webClient,
        properties.getTradePlatformAuthUrl(),
        properties.getTradePlatformClientId(),
        properties.getTradePlatformClientSecret(),
        properties.getTradePlatformScope());
  }

  protected WebClient getWebClient() {
    return WebClient.create();
  }

  protected ExchangeFilterFunction logResponse(final Logger logger) {
    return ExchangeFilterFunction.ofResponseProcessor(response -> logBody(response, logger));
  }

  protected Mono<ClientResponse> logBody(final ClientResponse response,
      final Logger logger) {
    if (response.statusCode().is4xxClientError()) {
      return response.bodyToMono(String.class)
          .flatMap(body -> {
            logger.log(Level.SEVERE, () -> format(
                "Error sending to Trade for Notify with body %s ", body));
            return Mono.just(response);
          });
    }
    return Mono.just(response);
  }
}
