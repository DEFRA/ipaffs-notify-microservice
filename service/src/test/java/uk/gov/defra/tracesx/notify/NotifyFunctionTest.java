package uk.gov.defra.tracesx.notify;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
import uk.gov.defra.tracesx.notify.apiclient.TradePlatformApiClient;
import uk.gov.defra.tracesx.notify.apimodel.SuccessResponse;
import uk.gov.defra.tracesx.notify.email.apimodel.BatchEmailTemplate;
import uk.gov.defra.tracesx.notify.sms.apimodel.BatchSmsTemplate;
import uk.gov.defra.tracesx.notify.utils.LogHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@ExtendWith(MockitoExtension.class)
public class NotifyFunctionTest {

  @Mock
  private ExecutionContext context;

  @Mock
  private TradePlatformApiClient tradePlatformApiClient;

  @Spy
  private NotifyFunction notifyFunction;

  private LogHandler logHandler;
  private WithEnvironmentVariables environment;
  private String queueMessage;

  @BeforeEach
  public void setup() throws IOException {
    logHandler = new LogHandler();
    Logger logger = Logger.getLogger("test logger");
    logger.setUseParentHandlers(false);
    logger.setLevel(Level.ALL);
    logger.addHandler(logHandler);
    when(context.getLogger()).thenReturn(logger);

    queueMessage = IOUtils.toString(
        LocalFunctionRunner.class.getResourceAsStream("/textQueueMessage.json"),
        StandardCharsets.UTF_8);
    environment = setUpEnvironmentVariables();

  }

  @Test
  public void notifyTextOrEmail_LogsQueueMessage_WhenFunctionIsTriggered()
      throws Exception {
    environment = enableEmailOrTextNotification(environment, "false", "false");
    environment.execute(() -> notifyFunction.notifyTextOrEmail(queueMessage, context));
    logHandler.assertLogged(Level.INFO, "NotifyFunction starting");
    logHandler.assertLogged(Level.INFO,
        "Message received from queue -->>" + queueMessage);
    logHandler.assertLogged(Level.INFO, "NotifyFunction stopping");
  }

  @Test
  public void notifyTextOrEmail_LogsMessageForText_WhenTextFeatureFlagIsDisabled()
      throws Exception {
    environment = enableEmailOrTextNotification(environment, "true", "false");
    environment.execute(() -> notifyFunction.notifyTextOrEmail(queueMessage, context));
    assertLogMessage("Text set to LOG Only: Number: 07885484684, "
        + "Template Id: 0b059aa6-1840-408c-aa1b-a53aa16b815d, "
        + "Content: MessagePersonalisation(referenceNumber=CHEDD-12345, bcpName=Leamington) ");
  }

  @Test
  public void notifyTextOrEmail_LogsMessageForEmail_WhenEmailFeatureFlagIsDisabled()
      throws Exception {
    queueMessage = IOUtils.toString(
        LocalFunctionRunner.class.getResourceAsStream("/emailQueueMessage.json"),
        StandardCharsets.UTF_8);
    environment = enableEmailOrTextNotification(environment, "false", "true");
    environment.execute(() -> notifyFunction.notifyTextOrEmail(queueMessage, context));
    assertLogMessage(
        "Email set to LOG Only: EMAIL: s.chandran@kainos.com, Template Id: 2ef3e2ac-3f33-45a5-bb50-2d6cac147601, "
            + "Content: MessagePersonalisation(referenceNumber=CHEDD-12345, bcpName=Leamington) ");
  }

  @Test
  public void notifyTextOrEmail_InvokesTradeNotifyApi_WhenEmailFeatureFlagIsEnabled()
      throws Exception {
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

  private void assertLogMessage(String s) {
    logHandler.assertLogged(Level.INFO, "NotifyFunction starting");
    logHandler.assertLogged(Level.INFO,
        "Message received from queue -->>" + queueMessage);
    logHandler.assertLogged(Level.INFO,
        s);
    logHandler.assertLogged(Level.INFO, "NotifyFunction stopping");
  }
}
