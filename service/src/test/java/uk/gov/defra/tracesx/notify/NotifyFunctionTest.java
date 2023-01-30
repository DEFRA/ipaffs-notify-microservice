package uk.gov.defra.tracesx.notify;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.stefanbirkner.systemlambda.SystemLambda.WithEnvironmentVariables;
import com.microsoft.azure.functions.ExecutionContext;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.defra.tracesx.notify.apiclient.TradePlatformApiClient;
import uk.gov.defra.tracesx.notify.apimodel.SuccessResponse;
import uk.gov.defra.tracesx.notify.email.apimodel.BatchEmailTemplate;
import uk.gov.defra.tracesx.notify.service.TradePlatformTokenGeneratorService;
import uk.gov.defra.tracesx.notify.sms.apimodel.BatchSmsTemplate;
import uk.gov.defra.tracesx.notify.utils.LogHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@ExtendWith(MockitoExtension.class)
public class NotifyFunctionTest {

  @Mock
  private ExecutionContext context;

  @Mock
  private TradePlatformApiClient tradePlatformApiClient;

  @Mock
  private WebClient webClient;

  @Spy
  private NotifyFunction notifyFunction;

  private LogHandler logHandler;
  private WithEnvironmentVariables environment;
  private String queueMessage;

  @BeforeEach
  public void setup() throws IOException {
    logHandler = new LogHandler();
    queueMessage = IOUtils.toString(
        LocalFunctionRunner.class.getResourceAsStream("/textQueueMessage.json"),
        StandardCharsets.UTF_8);
    environment = setUpEnvironmentVariables();
  }

  private void mockLogger() {
    Logger logger = Logger.getLogger("test logger");
    logger.setUseParentHandlers(false);
    logger.setLevel(Level.ALL);
    logger.addHandler(logHandler);
    when(context.getLogger()).thenReturn(logger);
  }

  @Test
  public void notifyTextOrEmail_LogsQueueMessage_WhenFunctionIsTriggered()
      throws Exception {
    mockLogger();
    environment = enableEmailOrTextNotification(environment, "false", "false");
    environment.execute(() -> notifyFunction.notifyTextOrEmail(queueMessage, context));
    logHandler.assertLogged(Level.INFO, "NotifyFunction starting");
    logHandler.assertLogged(Level.INFO,
        "Notification reference received from queue CHEDD-12345, with template ID 0b059aa6-1840-408c-aa1b-a53aa16b815d");
    logHandler.assertLogged(Level.INFO, "NotifyFunction stopping");
  }

  @Test
  public void notifyTextOrEmail_LogsMessageForText_WhenTextFeatureFlagIsDisabled()
      throws Exception {
    mockLogger();
    environment = enableEmailOrTextNotification(environment, "true", "false");
    environment.execute(() -> notifyFunction.notifyTextOrEmail(queueMessage, context));
    logHandler.assertLogged(Level.INFO,
        "Message not eligible for sending, message had 1 phone numbers for template "
        + "ID 0b059aa6-1840-408c-aa1b-a53aa16b815d and notification reference CHEDD-12345");
  }

  @Test
  public void notifyTextOrEmail_LogsMessageForEmail_WhenEmailFeatureFlagIsDisabled()
      throws Exception {
    mockLogger();
    queueMessage = IOUtils.toString(
        LocalFunctionRunner.class.getResourceAsStream("/emailQueueMessage.json"),
        StandardCharsets.UTF_8);
    environment = enableEmailOrTextNotification(environment, "false", "true");
    environment.execute(() -> notifyFunction.notifyTextOrEmail(queueMessage, context));
    logHandler.assertLogged(Level.INFO,
        "Message not eligible for sending, message had 1 emails for template "
            + "ID 2ef3e2ac-3f33-45a5-bb50-2d6cac147601 and notification reference CHEDD-12345");
  }

  @Test
  public void notifyTextOrEmail_InvokesTradeNotifyApi_WhenEmailFeatureFlagIsEnabled()
      throws Exception {
    mockLogger();
    doReturn(tradePlatformApiClient).when(notifyFunction).tradePlatformApiClient(any(), any());
    queueMessage = IOUtils.toString(
        LocalFunctionRunner.class.getResourceAsStream("/emailQueueMessage.json"),
        StandardCharsets.UTF_8);
    environment = enableEmailOrTextNotification(environment,
        "true",
        "false");
    when(tradePlatformApiClient.submitRequest(any(BatchEmailTemplate.class))).thenReturn(
        new SuccessResponse("1234", "SUBMITTED",
            "Your message has been received and ready for processing", "CHEDD-12345"));
    environment.execute(() -> notifyFunction.notifyTextOrEmail(queueMessage, context));
    assertEmailSentMessageLogged();
  }

  @Test
  public void notifyTextOrEmail_InvokesTradeNotifyApi_WhenTextFeatureFlagIsEnabled()
      throws Exception {
    mockLogger();
    doReturn(tradePlatformApiClient).when(notifyFunction).tradePlatformApiClient(any(), any());
    environment = enableEmailOrTextNotification(environment,
        "false",
        "true");
    when(tradePlatformApiClient.submitRequest(any(BatchSmsTemplate.class))).thenReturn(
        new SuccessResponse("1234", "SUBMITTED",
            "Your message has been received and ready for processing", "CHEDD-12345"));
    environment.execute(() -> notifyFunction.notifyTextOrEmail(queueMessage, context));
    assertTextSentMessageLogged();
  }

  @Test
  public void checkWebClientBuilder() {
    WebClient result = notifyFunction.getWebClientBuild(context.getLogger());
    assertThat(result).isNotNull();
  }

  @Test
  public void checkWebClientCreation() {
    WebClient result = notifyFunction.getWebClient();
    assertThat(result).isNotNull();
  }

  @Test
  public void logResponseCreation() {
    ExchangeFilterFunction result = notifyFunction.logResponse(context.getLogger());
    assertThat(result).isNotNull();
  }

  @Test
  public void logBodyCreation_whenStatusCodeIsClientError() {
    mockLogger();
    ClientResponse testResponse = mock(ClientResponse.class);
    when(testResponse.statusCode()).thenReturn(HttpStatus.NOT_FOUND);
    when(testResponse.bodyToMono(String.class)).thenReturn(Mono.just("not found"));

    Mono<ClientResponse> result = notifyFunction.logBody(testResponse, context.getLogger());
    StepVerifier.create(result)
        .assertNext(data -> assertThat(testResponse).isEqualTo(data)).verifyComplete();
  }

  @Test
  public void logBodyCreation_whenStatusCodeIsNotClientError() {
    ClientResponse testResponse = mock(ClientResponse.class);
    when(testResponse.statusCode()).thenReturn(HttpStatus.OK);
    Mono<ClientResponse> result = notifyFunction.logBody(testResponse, context.getLogger());
    StepVerifier.create(result)
        .assertNext(data -> assertThat(testResponse).isEqualTo(data)).verifyComplete();
  }

  @Test
  public void createTradePlatformTokenGeneratorObject() {
    doReturn(webClient).when(notifyFunction).getWebClient();

    NotifyProperties properties = mock(NotifyProperties.class);
    when(properties.getTradePlatformAuthUrl()).thenReturn("platform url");
    when(properties.getTradePlatformClientId()).thenReturn("platform client");
    when(properties.getTradePlatformClientSecret()).thenReturn("platform secret");
    when(properties.getTradePlatformScope()).thenReturn("platform scope");

    TradePlatformTokenGeneratorService expected = new TradePlatformTokenGeneratorService(
        context.getLogger(),
        webClient,
        properties.getTradePlatformAuthUrl(),
        properties.getTradePlatformClientId(),
        properties.getTradePlatformClientSecret(),
        properties.getTradePlatformScope());

    TradePlatformTokenGeneratorService result = notifyFunction.tradePlatformTokenGeneratorService(
        properties, context.getLogger());
    assertThat(result)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  @Test
  public void createTradePlatformApiClientObject() {
    doReturn(webClient).when(notifyFunction).getWebClientBuild(any());
    TradePlatformTokenGeneratorService tradePlatformTokenGeneratorService = mock(
        TradePlatformTokenGeneratorService.class);
    doReturn(tradePlatformTokenGeneratorService).when(notifyFunction)
        .tradePlatformTokenGeneratorService(any(), any());
    when(tradePlatformTokenGeneratorService.generateToken()).thenReturn("output");

    NotifyProperties properties = mock(NotifyProperties.class);
    when(properties.getTradePlatformNotifyUrl()).thenReturn("unittest.com");

    TradePlatformApiClient expected = new TradePlatformApiClient(
        context.getLogger(),
        webClient,
        "output",
        URI.create(properties.getTradePlatformNotifyUrl()),
        properties.getTradePlatformSubscriptionKey()
    );

    TradePlatformApiClient result = notifyFunction.tradePlatformApiClient(
        properties, context.getLogger());
    assertThat(result)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  private WithEnvironmentVariables enableEmailOrTextNotification(
      final WithEnvironmentVariables environment,
      final String enableEmailNotification,
      final String enableTextNotification) {
    return environment.and("ENABLE_EMAIL_NOTIFICATION", enableEmailNotification).
        and("ENABLE_TEXT_NOTIFICATION", enableTextNotification);
  }

  private void assertTextSentMessageLogged() {
    logHandler.assertLogged(Level.INFO, "NotifyFunction starting");
    verify(tradePlatformApiClient).submitRequest(any(BatchSmsTemplate.class));
    logHandler.assertLogged(Level.INFO,
        "Successfully sent message to Notify API (reference 1234) for"
            + " processing of notification -->> CHEDD-12345");
    logHandler.assertLogged(Level.INFO, "NotifyFunction stopping");
  }

  private void assertEmailSentMessageLogged() {
    logHandler.assertLogged(Level.INFO, "NotifyFunction starting");
    verify(tradePlatformApiClient).submitRequest(any(BatchEmailTemplate.class));
    logHandler.assertLogged(Level.INFO,
        "Successfully sent message to Notify API (reference 1234) for"
            + " processing of notification -->> CHEDD-12345");
    logHandler.assertLogged(Level.INFO, "NotifyFunction stopping");
  }

  private WithEnvironmentVariables setUpEnvironmentVariables() {
    return withEnvironmentVariable("PROTOCOL", "https")
        .and("ENV_DOMAIN", "-test")
        .and("TRADE_PLATFORM_AUTH_URL", "TRADE_PLATFORM_AUTH_URL")
        .and("TRADE_PLATFORM_NOTIFY_URL", "TRADE_PLATFORM_NOTIFY_URL")
        .and("TRADE_PLATFORM_CLIENT_ID", "TRADE_PLATFORM_CLIENT_ID")
        .and("TRADE_PLATFORM_CLIENT_SECRET", "TRADE_PLATFORM_CLIENT_SECRET")
        .and("TRADE_PLATFORM_SCOPE", "TRADE_PLATFORM_SCOPE")
        .and("TRADE_PLATFORM_SYSTEM_NAME", "TRADE_PLATFORM_SYSTEM_NAME")
        .and("TRADE_PLATFORM_SYSTEM_UNIQUE_ID", "TRADE_PLATFORM_SYSTEM_UNIQUE_ID")
        .and("TRADE_PLATFORM_SUBSCRIPTION_KEY", "TRADE_PLATFORM_SUBSCRIPTION_KEY");
  }
}
