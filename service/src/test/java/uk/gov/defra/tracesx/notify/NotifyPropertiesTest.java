package uk.gov.defra.tracesx.notify;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.stefanbirkner.systemlambda.SystemLambda.WithEnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.defra.tracesx.notify.utils.LogHandler;

import java.util.logging.Logger;

class NotifyPropertiesTest {

  private LogHandler logHandler;
  private Logger logger;

  private NotifyProperties properties;

  @BeforeEach
  void before() {
    logger = Logger.getLogger("TestLogger");
    logHandler = LogHandler.configure(logger);
  }

  @Test
  void properties_ValidatesEnvironment_WhenRequiredVariablesArePresent() throws Exception {
    withRequiredEnvironmentVariables()
        .execute(() -> properties = NotifyProperties.properties(logger));

    logHandler.assertLogged(INFO, "Environment variable ENABLE_EMAIL_NOTIFICATION is set");
    logHandler.assertLogged(INFO, "Environment variable ENABLE_TEXT_NOTIFICATION is set");
    logHandler.assertLogged(INFO, "Environment variable TRADE_PLATFORM_AUTH_URL is set");
    logHandler.assertLogged(INFO, "Environment variable TRADE_PLATFORM_NOTIFY_URL is set");
    logHandler.assertLogged(INFO, "Environment variable TRADE_PLATFORM_CLIENT_ID is set");
    logHandler.assertLogged(INFO, "Environment variable TRADE_PLATFORM_CLIENT_SECRET is set");
    logHandler.assertLogged(INFO, "Environment variable TRADE_PLATFORM_SCOPE is set");
    logHandler.assertLogged(INFO, "Environment variable TRADE_PLATFORM_SYSTEM_NAME is set");
    logHandler.assertLogged(INFO, "Environment variable TRADE_PLATFORM_SYSTEM_UNIQUE_ID is set");
    logHandler.assertLogged(INFO, "Environment variable TRADE_PLATFORM_SUBSCRIPTION_KEY is set");
    logHandler.assertLogged(INFO, "Environment variable API_VERSION is set");
  }

  @Test
  void properties_ReturnsCorrectProperties_WhenRequiredVariablesArePresent() throws Exception {
    withRequiredEnvironmentVariables()
        .execute(() -> properties = NotifyProperties.properties(logger));

    assertThat(properties.getEnableEmailNotification()).isEqualTo("enableEmailNotification");
    assertThat(properties.getEnableTextNotification()).isEqualTo("enableTextNotification");
    assertThat(properties.getTradePlatformAuthUrl()).isEqualTo("tradePlatformAuthUrl");
    assertThat(properties.getTradePlatformNotifyUrl()).isEqualTo("tradePlatformNotifyUrl");
    assertThat(properties.getTradePlatformClientId()).isEqualTo("tradePlatformClientId");
    assertThat(properties.getTradePlatformClientSecret()).isEqualTo("tradePlatformClientSecret");
    assertThat(properties.getTradePlatformScope()).isEqualTo("tradePlatformScope");
    assertThat(properties.getTradePlatformSystemName()).isEqualTo("tradePlatformSystemName");
    assertThat(properties.getTradePlatformSystemUniqueId()).isEqualTo(
        "tradePlatformSystemUniqueId");
    assertThat(properties.getTradePlatformSubscriptionKey()).isEqualTo(
        "tradePlatformSubscriptionKey");
    assertThat(properties.getVersion()).isEqualTo("1.2.3");
  }

  @Test
  void properties_LogsErrorsAndThrows_WhenRequiredVariablesAreNotPresent() {
    assertThatThrownBy(() -> NotifyProperties.properties(logger))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Application environment incorrectly configured");

    logHandler.assertLogged(SEVERE, "Environment variable ENABLE_EMAIL_NOTIFICATION is not set");
    logHandler.assertLogged(SEVERE, "Environment variable ENABLE_TEXT_NOTIFICATION is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_AUTH_URL is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_NOTIFY_URL is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_CLIENT_ID is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_CLIENT_SECRET is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_SCOPE is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_SYSTEM_NAME is not set");
    logHandler.assertLogged(SEVERE,
        "Environment variable TRADE_PLATFORM_SYSTEM_UNIQUE_ID is not set");
    logHandler.assertLogged(SEVERE,
        "Environment variable TRADE_PLATFORM_SUBSCRIPTION_KEY is not set");
    logHandler.assertLogged(SEVERE, "Environment variable API_VERSION is not set");
  }

  @Test
  void properties_LogsErrorsAndThrows_WhenRequiredVariablesAreEmpty() throws Exception {
    withEnvironmentVariable("ENABLE_EMAIL_NOTIFICATION", "")
        .and("ENABLE_TEXT_NOTIFICATION", "")
        .and("TRADE_PLATFORM_AUTH_URL", "")
        .and("TRADE_PLATFORM_NOTIFY_URL", "")
        .and("TRADE_PLATFORM_CLIENT_ID", "")
        .and("TRADE_PLATFORM_CLIENT_SECRET", "")
        .and("TRADE_PLATFORM_SCOPE", "")
        .and("TRADE_PLATFORM_SYSTEM_NAME", "")
        .and("TRADE_PLATFORM_SYSTEM_UNIQUE_ID", "")
        .and("TRADE_PLATFORM_SUBSCRIPTION_KEY", "")
        .and("API_VERSION", "")
        .execute(() ->
            assertThatThrownBy(() -> NotifyProperties.properties(logger))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Application environment incorrectly configured"));

    logHandler.assertLogged(SEVERE, "Environment variable ENABLE_EMAIL_NOTIFICATION is not set");
    logHandler.assertLogged(SEVERE, "Environment variable ENABLE_TEXT_NOTIFICATION is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_AUTH_URL is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_NOTIFY_URL is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_CLIENT_ID is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_CLIENT_SECRET is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_SCOPE is not set");
    logHandler.assertLogged(SEVERE, "Environment variable TRADE_PLATFORM_SYSTEM_NAME is not set");
    logHandler.assertLogged(SEVERE,
        "Environment variable TRADE_PLATFORM_SYSTEM_UNIQUE_ID is not set");
    logHandler.assertLogged(SEVERE,
        "Environment variable TRADE_PLATFORM_SUBSCRIPTION_KEY is not set");
    logHandler.assertLogged(SEVERE, "Environment variable API_VERSION is not set");
  }

  private WithEnvironmentVariables withRequiredEnvironmentVariables() {
    return withEnvironmentVariable("ENABLE_EMAIL_NOTIFICATION", "enableEmailNotification")
        .and("ENABLE_TEXT_NOTIFICATION", "enableTextNotification")
        .and("TRADE_PLATFORM_AUTH_URL", "tradePlatformAuthUrl")
        .and("TRADE_PLATFORM_NOTIFY_URL", "tradePlatformNotifyUrl")
        .and("TRADE_PLATFORM_CLIENT_ID", "tradePlatformClientId")
        .and("TRADE_PLATFORM_CLIENT_SECRET", "tradePlatformClientSecret")
        .and("TRADE_PLATFORM_SCOPE", "tradePlatformScope")
        .and("TRADE_PLATFORM_SYSTEM_NAME", "tradePlatformSystemName")
        .and("TRADE_PLATFORM_SYSTEM_UNIQUE_ID", "tradePlatformSystemUniqueId")
        .and("TRADE_PLATFORM_SUBSCRIPTION_KEY", "tradePlatformSubscriptionKey")
        .and("API_VERSION", "1.2.3");
  }
}
