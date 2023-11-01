package uk.gov.defra.tracesx.notify.apiclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.defra.tracesx.notify.apimodel.SuccessResponse;
import uk.gov.defra.tracesx.notify.email.apimodel.BatchEmailTemplate;
import uk.gov.defra.tracesx.notify.exception.NotifyException;
import uk.gov.defra.tracesx.notify.utils.WebServerUtils;

class TradePlatformApiClientTest {

  private TradePlatformApiClient service;

  private WebServerUtils webServerUtils;

  @Mock
  private Logger logger;

  @BeforeEach
  public void setUp() {
    var server = new MockWebServer();
    WebClient webClient = WebClient.builder().build();
    String tradeNotifyUrl = String.format("http://%s:%s", server.getHostName(), server.getPort());
    service = new TradePlatformApiClient(logger, webClient,
        "authToken", URI.create(tradeNotifyUrl), "subcriptionKey");
    webServerUtils = new WebServerUtils(server);
  }

  @Test
  void submitRequest_ReturnsOK_WhenCorrectPayloadIsSent() throws IOException {
    webServerUtils.enqueueOk(
        IOUtils.resourceToString("/trade-notify-responses/success_response.json",
            StandardCharsets.UTF_8));
    SuccessResponse successResponse = service.submitRequest(BatchEmailTemplate.builder().build());
    assertThat(successResponse.getCodeValue()).isEqualTo("SUBMITTED");
    assertThat(successResponse.getReference()).isEqualTo("5fcc4f88-ef9a-4ae1-924f-3b3364c5aa74");
    assertThat(successResponse.getCodeMessage()).isEqualTo(
        "Your message has been received and ready for processing");
    assertThat(successResponse.getYourreference()).isEqualTo("test-1");
  }

  @Test
  void submitRequest_ReturnsError_WhenInCorrectPayloadIsSent() {
    webServerUtils.enqueueBadRequest();
    BatchEmailTemplate template = BatchEmailTemplate.builder().reference("12345").build();
    assertThatThrownBy(() -> service.submitRequest(template))
        .isInstanceOf(NotifyException.class)
        .hasMessage("Failed to send to Trade Notify for reference -->>12345");
  }
}