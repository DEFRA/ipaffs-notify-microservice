package uk.gov.defra.tracesx.notify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.defra.tracesx.notify.exception.NotifyException;
import uk.gov.defra.tracesx.notify.utils.WebServerUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@ExtendWith(MockitoExtension.class)
class TradePlatformTokenGeneratorServiceTest {

  private static final String CLIENT_ID = "client_id";
  private static final String CLIENT_SECRET = "client_secret";
  private static final String TOKEN_SCOPE = "scope";

  private static final String EXPECTED_TOKEN_VALUE = "valid-access-token";

  private WebServerUtils webServerUtils;

  private TradePlatformTokenGeneratorService service;

  @Mock
  private Logger logger;

  @BeforeEach
  public void setUp() {
    var server = new MockWebServer();
    WebClient webClient = WebClient.builder().build();
    String authUrl = String.format("http://%s:%s", server.getHostName(), server.getPort());
    service = new TradePlatformTokenGeneratorService(logger, webClient,
        authUrl, CLIENT_ID, CLIENT_SECRET, TOKEN_SCOPE);
    webServerUtils = new WebServerUtils(server);
  }

  @Test
  void generateToken_returnsValidToken_whenInvoked() throws IOException {
    webServerUtils.enqueueOk(
        IOUtils.resourceToString("/auth-token-generation-fixtures/valid-token.json",
            StandardCharsets.UTF_8));
    String result = service.generateToken();
    assertThat(result).isEqualTo(EXPECTED_TOKEN_VALUE);
  }

  @Test
  void generateToken_throwsException_whenBadRequestGetsReturnedByTrade() {
    webServerUtils.enqueueBadRequest("bad-request");
    assertThatThrownBy(service::generateToken).isInstanceOf(NotifyException.class)
        .hasMessage("Failed to retrieve Trade Platform auth token");
  }
}
