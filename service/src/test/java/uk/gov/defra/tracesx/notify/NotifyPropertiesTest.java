package uk.gov.defra.tracesx.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import uk.gov.defra.tracesx.notify.NotifyProperties.NotifyPropertiesBuilder;

import java.util.logging.Logger;

class NotifyPropertiesTest {


  @Test
  void builder_ReturnsProperties() {
    NotifyProperties notifyProperties = new NotifyPropertiesBuilder()
        .enableEmailNotification("email")
        .enableTextNotification("text")
        .tradePlatformAuthUrl("auth url")
        .tradePlatformNotifyUrl("notify url")
        .tradePlatformClientId("client id")
        .tradePlatformClientSecret("client secret")
        .tradePlatformScope("platform scope")
        .tradePlatformSystemName("system name")
        .tradePlatformSystemUniqueId("id")
        .tradePlatformSubscriptionKey("sub key")
        .build();

    assertThat(notifyProperties.getEnableEmailNotification()).isEqualTo("email");
    assertThat(notifyProperties.getEnableTextNotification()).isEqualTo("text");
    assertThat(notifyProperties.getTradePlatformAuthUrl()).isEqualTo("auth url");
    assertThat(notifyProperties.getTradePlatformNotifyUrl()).isEqualTo("notify url");
    assertThat(notifyProperties.getTradePlatformClientId()).isEqualTo("client id");
    assertThat(notifyProperties.getTradePlatformClientSecret()).isEqualTo("client secret");
    assertThat(notifyProperties.getTradePlatformScope()).isEqualTo("platform scope");
    assertThat(notifyProperties.getTradePlatformSystemName()).isEqualTo("system name");
    assertThat(notifyProperties.getTradePlatformSystemUniqueId()).isEqualTo("id");
    assertThat(notifyProperties.getTradePlatformSubscriptionKey()).isEqualTo("sub key");
  }

  @Test
  void throwException_whenEnvironmentVariablesAreNotSet() {
    assertThatThrownBy(() -> NotifyProperties.properties(mock(Logger.class)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Application environment incorrectly configured");
  }
}