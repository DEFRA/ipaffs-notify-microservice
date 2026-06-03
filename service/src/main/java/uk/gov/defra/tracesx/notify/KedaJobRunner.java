package uk.gov.defra.tracesx.notify;

import static java.lang.Boolean.parseBoolean;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.defra.tracesx.notify.apiclient.TradePlatformApiClient;
import uk.gov.defra.tracesx.notify.apimodel.SuccessResponse;
import uk.gov.defra.tracesx.notify.apimodel.Template;
import uk.gov.defra.tracesx.notify.model.MessageType;
import uk.gov.defra.tracesx.notify.model.QueueMessage;
import uk.gov.defra.tracesx.notify.service.TradePlatformTokenGeneratorService;
import uk.gov.defra.tracesx.notify.transformer.MessageTransformer;
import uk.gov.defra.tracesx.notify.transformer.MessageTransformerFactory;

/**
 * Entry point for running this service as a KEDA ScaledJob.
 */
public class KedaJobRunner {

  private static final Logger LOGGER = Logger.getLogger(KedaJobRunner.class.getName());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String QUEUE_NAME_ENV = "NOTIFY_QUEUE_NAME";
  private static final String QUEUE_CONNECTION_ENV = "NOTIFY_QUEUE_CONNECTION_NAME";

  private static final String MAX_MESSAGES_ENV = "KEDA_MAX_MESSAGES";
  private static final String RECEIVE_WAIT_SECONDS_ENV = "KEDA_RECEIVE_WAIT_SECONDS";
  private static final String MAX_EMPTY_RECEIVES_ENV = "KEDA_MAX_EMPTY_RECEIVES";

  public static void main(String[] args) {
    try {
      new KedaJobRunner().runOnce();
    } catch (Exception exception) {
      LOGGER.log(Level.SEVERE, "KEDA job execution failed", exception);
      System.exit(1);
    }
  }

  void runOnce() {
    String queueName = requiredEnv(QUEUE_NAME_ENV);
    String connectionString = requiredEnv(QUEUE_CONNECTION_ENV);

    int maxMessages = intEnv(MAX_MESSAGES_ENV, 100);
    int receiveWaitSeconds = intEnv(RECEIVE_WAIT_SECONDS_ENV, 10);
    int maxEmptyReceives = intEnv(MAX_EMPTY_RECEIVES_ENV, 3);

    LOGGER.info(() -> String.format(
        "Starting KEDA job. queueName=%s, maxMessages=%d, receiveWaitSeconds=%d, "
            + "maxEmptyReceives=%d",
        queueName, maxMessages, receiveWaitSeconds, maxEmptyReceives));

    try (ServiceBusReceiverClient receiver = new ServiceBusClientBuilder()
        .connectionString(connectionString)
        .receiver()
        .queueName(queueName)
        .buildClient()) {

      int processed = 0;
      int emptyReceives = 0;

      while (processed < maxMessages && emptyReceives < maxEmptyReceives) {
        List<ServiceBusReceivedMessage> messages = receiver
            .receiveMessages(1, Duration.ofSeconds(receiveWaitSeconds))
            .stream()
            .toList();

        if (messages.isEmpty()) {
          emptyReceives++;
          continue;
        }

        emptyReceives = 0;
        ServiceBusReceivedMessage message = messages.getFirst();
        LOGGER.info(() -> String.format("Processing Notify message with id %s",
            message.getMessageId()));

        processMessage(message);
        receiver.complete(message);
        processed++;
      }

      LOGGER.info(String.format("Finished Notify queue. processed=%d, emptyReceives=%d",
          processed, emptyReceives));
    }
  }

  private void processMessage(ServiceBusReceivedMessage message) {
    try {
      NotifyProperties notifyProperties = NotifyProperties.properties(LOGGER);
      QueueMessage queueMessage = OBJECT_MAPPER.readValue(message.getBody().toString(),
          QueueMessage.class);
      LOGGER.info(() -> String.format(
          "Notification reference received from queue %s, with template ID %s",
          queueMessage.getMessagePersonalisation().getReferenceNumber(),
          queueMessage.getMessageTemplateId()));

      if (isEligibleForNotification(notifyProperties, queueMessage)) {
        submitForNotification(notifyProperties, queueMessage);
      } else {
        logNonSubmittedMessage(queueMessage);
      }
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to process Notify message", exception);
    }
  }

  private void logNonSubmittedMessage(QueueMessage queueMessage) {
    logMessage(queueMessage, queueMessage.getEmails(), "emails");
    logMessage(queueMessage, queueMessage.getPhoneNumbers(), "phone numbers");
  }

  private static void logMessage(QueueMessage queueMessage, List<String> contactsDetails,
      String type) {
    if (contactsDetails != null) {
      LOGGER.info(() -> String.format(
          "Message not eligible for sending, message had %d %s for template "
              + "ID %s and notification reference %s",
          contactsDetails.size(),
          type,
          queueMessage.getMessageTemplateId(),
          queueMessage.getMessagePersonalisation().getReferenceNumber()));
    }
  }

  private boolean isEligibleForNotification(NotifyProperties notifyProperties,
      QueueMessage queueMessage) {
    return isEligibleForMessageType(MessageType.EMAIL, queueMessage,
        notifyProperties.getEnableEmailNotification())
        || isEligibleForMessageType(MessageType.TEXT, queueMessage,
        notifyProperties.getEnableTextNotification());
  }

  private boolean isEligibleForMessageType(MessageType messageType, QueueMessage queueMessage,
      String enableTextNotificationViaTradePlatform) {
    return queueMessage.getMessageType().equals(messageType) && parseBoolean(
        enableTextNotificationViaTradePlatform);
  }

  private void submitForNotification(NotifyProperties notifyProperties, QueueMessage queueMessage) {
    TradePlatformApiClient tradePlatformApiClient = tradePlatformApiClient(notifyProperties);
    MessageTransformer messageTransformer = MessageTransformerFactory.getTransformer(
        queueMessage.getMessageType());
    Template template = messageTransformer.transform(queueMessage,
        notifyProperties.getTradePlatformSystemName(),
        notifyProperties.getTradePlatformSystemUniqueId());
    SuccessResponse successResponse = tradePlatformApiClient.submitRequest(template);
    LOGGER.info(() -> String.format("Successfully sent message to Notify API (reference %s) for"
            + " processing of notification -->> %s", successResponse.getReference(),
        template.getReference()));
  }

  private TradePlatformApiClient tradePlatformApiClient(NotifyProperties properties) {
    String token = tradePlatformTokenGeneratorService(properties).generateToken();
    return new TradePlatformApiClient(
        LOGGER,
        WebClient.create(),
        token,
        URI.create(properties.getTradePlatformNotifyUrl()),
        properties.getTradePlatformSubscriptionKey());
  }

  private TradePlatformTokenGeneratorService tradePlatformTokenGeneratorService(
      NotifyProperties properties) {
    return new TradePlatformTokenGeneratorService(
        LOGGER,
        WebClient.create(),
        properties.getTradePlatformAuthUrl(),
        properties.getTradePlatformClientId(),
        properties.getTradePlatformClientSecret(),
        properties.getTradePlatformScope());
  }

  private static String requiredEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Environment variable %s is required".formatted(name));
    }
    return value;
  }

  private static int intEnv(String name, int defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }

}
