package uk.gov.defra.tracesx.notify;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.logging.Logger;
import java.util.stream.Stream;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotifyProperties {

  private final String enableEmailNotification;
  private final String enableTextNotification;
  private final String tradePlatformAuthUrl;
  private final String tradePlatformNotifyUrl;
  private final String tradePlatformClientId;
  private final String tradePlatformClientSecret;
  private final String tradePlatformScope;
  private final String tradePlatformSystemName;
  private final String tradePlatformSystemUniqueId;
  private final String tradePlatformSubscriptionKey;

  public static NotifyProperties properties(Logger logger) {
    validateEnvironment(logger);

    return new NotifyProperties(
        EnvironmentVariables.ENABLE_EMAIL_NOTIFICATION.getValue(),
        EnvironmentVariables.ENABLE_TEXT_NOTIFICATION.getValue(),
        EnvironmentVariables.TRADE_PLATFORM_AUTH_URL.getValue(),
        EnvironmentVariables.TRADE_PLATFORM_NOTIFY_URL.getValue(),
        EnvironmentVariables.TRADE_PLATFORM_CLIENT_ID.getValue(),
        EnvironmentVariables.TRADE_PLATFORM_CLIENT_SECRET.getValue(),
        EnvironmentVariables.TRADE_PLATFORM_SCOPE.getValue(),
        EnvironmentVariables.TRADE_PLATFORM_SYSTEM_NAME.getValue(),
        EnvironmentVariables.TRADE_PLATFORM_SYSTEM_UNIQUE_ID.getValue(),
        EnvironmentVariables.TRADE_PLATFORM_SUBSCRIPTION_KEY.getValue());
  }

  private static void validateEnvironment(Logger logger) {
    var validityResult = new Object() {
      boolean valid = true;
    };

    Stream.of(EnvironmentVariables.values())
        .forEach(v -> {
          if (v.hasValue()) {
            logger.info(String.format("Environment variable %s is set", v.environmentVariableName));
          } else {
            validityResult.valid = false;
            logger.severe(String.format("Environment variable %s has not been set",
                v.environmentVariableName));
          }
        });

    if (!validityResult.valid) {
      throw new IllegalStateException("Application environment incorrectly configured");
    }
  }

  private enum EnvironmentVariables {
    PROTOCOL("PROTOCOL"),
    ENV_DOMAIN("ENV_DOMAIN"),
    ENABLE_EMAIL_NOTIFICATION("ENABLE_EMAIL_NOTIFICATION"),
    ENABLE_TEXT_NOTIFICATION("ENABLE_TEXT_NOTIFICATION"),
    TRADE_PLATFORM_AUTH_URL("TRADE_PLATFORM_AUTH_URL"),
    TRADE_PLATFORM_NOTIFY_URL("TRADE_PLATFORM_NOTIFY_URL"),
    TRADE_PLATFORM_CLIENT_ID("TRADE_PLATFORM_CLIENT_ID"),
    TRADE_PLATFORM_CLIENT_SECRET("TRADE_PLATFORM_CLIENT_SECRET"),
    TRADE_PLATFORM_SCOPE("TRADE_PLATFORM_SCOPE"),
    TRADE_PLATFORM_SYSTEM_NAME("TRADE_PLATFORM_SYSTEM_NAME"),
    TRADE_PLATFORM_SYSTEM_UNIQUE_ID("TRADE_PLATFORM_SYSTEM_UNIQUE_ID"),
    TRADE_PLATFORM_SUBSCRIPTION_KEY("TRADE_PLATFORM_SUBSCRIPTION_KEY");

    private final String environmentVariableName;

    EnvironmentVariables(String environmentVariableName) {
      this.environmentVariableName = environmentVariableName;
    }

    public String getValue() {
      return System.getenv(environmentVariableName);
    }

    public boolean hasValue() {
      return getValue() != null && !getValue().isEmpty();
    }
  }
}
