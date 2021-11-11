package uk.gov.defra.tracesx.notify.utils;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.function.Consumer;

public class WebServerUtils {

  private final MockWebServer mockWebServer;

  public WebServerUtils(MockWebServer mockWebServer) {
    this.mockWebServer = mockWebServer;
  }

  public void enqueueOk(String response) {
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setResponseCode(HttpStatus.OK.value())
            .setBody(response));
  }

  public void enqueueBadRequest() {
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setResponseCode(HttpStatus.BAD_REQUEST.value()));
  }

  public void enqueueBadRequest(String response) {
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setResponseCode(HttpStatus.BAD_REQUEST.value())
            .setBody(response));
  }
}
