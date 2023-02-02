package uk.gov.defra.tracesx.notify;

import lombok.Builder;
import lombok.Getter;

import java.util.logging.Logger;

@Getter
@Builder
public class NotifyProperties {

  private final String enableEmailNotification;
  private final String enableTextNotification;
  private final String tradePlatformAuthUrl;
  private final String tradePlatformClientId;
  private final String tradePlatformClientSecret;
  private final String tradePlatformNotifyUrl;
  private final String tradePlatformScope;
  private final String tradePlatformSubscriptionKey;
  private final String tradePlatformSystemName;
  private final String tradePlatformSystemUniqueId;
  private final String version;

  public static NotifyProperties properties(Logger logger) {
    validateEnvironment(logger);
    return NotifyProperties.builder()
        .enableEmailNotification(EnvironmentVariable.ENABLE_EMAIL_NOTIFICATION.getValue())
        .enableTextNotification(EnvironmentVariable.ENABLE_TEXT_NOTIFICATION.getValue())
        .tradePlatformAuthUrl(EnvironmentVariable.TRADE_PLATFORM_AUTH_URL.getValue())
        .tradePlatformClientId(EnvironmentVariable.TRADE_PLATFORM_CLIENT_ID.getValue())
        .tradePlatformClientSecret(EnvironmentVariable.TRADE_PLATFORM_CLIENT_SECRET.getValue())
        .tradePlatformNotifyUrl(EnvironmentVariable.TRADE_PLATFORM_NOTIFY_URL.getValue())
        .tradePlatformScope(EnvironmentVariable.TRADE_PLATFORM_SCOPE.getValue())
        .tradePlatformSubscriptionKey(
            EnvironmentVariable.TRADE_PLATFORM_SUBSCRIPTION_KEY.getValue())
        .tradePlatformSystemName(EnvironmentVariable.TRADE_PLATFORM_SYSTEM_NAME.getValue())
        .tradePlatformSystemUniqueId(EnvironmentVariable.TRADE_PLATFORM_SYSTEM_UNIQUE_ID.getValue())
        .version(EnvironmentVariable.API_VERSION.getValue())
        .build();
  }

  private static void validateEnvironment(Logger logger) {
    boolean valid = true;

    for (EnvironmentVariable variable : EnvironmentVariable.values()) {
      if (variable.hasValue()) {
        logger.info(() -> String.format("Environment variable %s is set", variable.name()));
      } else {
        logger.severe(() -> String.format("Environment variable %s is not set", variable.name()));
        valid = false;
      }
    }

    if (!valid) {
      throw new IllegalStateException("Application environment incorrectly configured");
    }
  }

  private enum EnvironmentVariable {
    ENABLE_EMAIL_NOTIFICATION,
    ENABLE_TEXT_NOTIFICATION,
    TRADE_PLATFORM_AUTH_URL,
    TRADE_PLATFORM_NOTIFY_URL,
    TRADE_PLATFORM_CLIENT_ID,
    TRADE_PLATFORM_CLIENT_SECRET,
    TRADE_PLATFORM_SCOPE,
    TRADE_PLATFORM_SYSTEM_NAME,
    TRADE_PLATFORM_SYSTEM_UNIQUE_ID,
    TRADE_PLATFORM_SUBSCRIPTION_KEY,
    API_VERSION;

    public String getValue() {
      return System.getenv(name());
    }

    public boolean hasValue() {
      return getValue() != null && !getValue().isEmpty();
    }
  }
}
