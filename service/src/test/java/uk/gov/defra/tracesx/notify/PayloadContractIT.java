package uk.gov.defra.tracesx.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.APPLICATION_JSON;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.microsoft.azure.functions.ExecutionContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.testcontainers.azure.ServiceBusEmulatorContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mockserver.MockServerContainer;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Pins every request the service sends to the Trade Platform, byte for byte, so that a framework
 * or Jackson upgrade can't change them unnoticed. Runs both entry points against a MockServer
 * standing in for the token endpoint and the Notify API: the KEDA job (what the image runs), which
 * reads an email and a text message from a Service Bus emulator, and the Azure Function, given the
 * same two messages. Each entry point uses its own path prefix on the MockServer. The responses
 * include unknown fields.
 *
 * <p>The job is run in-process because {@link KedaJobRunner#runOnce()} is package-private. On a
 * mismatch the actual request is written to {@code target/payload-actual}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PayloadContractIT {

  private static final String PAYLOAD_DIR = "integration/payload/";
  private static final String NOTIFY_QUEUE = "notify";
  private static final String TOKEN_PATH = "/oauth2/token";
  // The batch endpoints, e.g. "/Notify/Api/EMAIL (batch)": matched by prefix, and pinned as sent
  private static final String NOTIFY_PATH = "/Notify/Api/";
  private static final String ACCESS_TOKEN = "payload-test-token";
  private static final String SUBSCRIPTION_KEY = "payload-test-subscription-key";
  private static final String EMAIL_MESSAGE = "emailQueueMessage.json";
  private static final String TEXT_MESSAGE = "textQueueMessage.json";

  private final Network network = Network.newNetwork();
  private final MSSQLServerContainer sqlServer =
      new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest")
          .acceptLicense()
          .withNetwork(network);
  private final ServiceBusEmulatorContainer serviceBus =
      new ServiceBusEmulatorContainer("mcr.microsoft.com/azure-messaging/servicebus-emulator")
          .acceptLicense()
          .withConfig(MountableFile.forClasspathResource("integration/service-bus-config.json"))
          .withNetwork(network)
          .withMsSqlServerContainer(sqlServer);
  private final MockServerContainer mockServer =
      new MockServerContainer(DockerImageName.parse("mockserver/mockserver")
          .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion()));
  private MockServerClient mockServerClient;

  @BeforeAll
  void runService() throws Exception {
    Startables.deepStart(sqlServer, mockServer).join();
    serviceBus.start();
    mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
    mockServerClient
        .when(request().withMethod("POST").withPath(".*" + TOKEN_PATH))
        .respond(response().withContentType(APPLICATION_JSON).withBody(
            "{\"token_type\":\"Bearer\",\"expires_in\":3599,\"ext_expires_in\":3599,"
                + "\"access_token\":\"" + ACCESS_TOKEN + "\",\"unknownField\":\"ignored\"}"));
    mockServerClient
        .when(request().withMethod("POST").withPath(".*" + NOTIFY_PATH + ".*"))
        .respond(response().withContentType(APPLICATION_JSON).withBody(
            "{\"reference\":\"5fcc4f88-ef9a-4ae1-924f-3b3364c5aa74\",\"codeValue\":\"SUBMITTED\","
                + "\"codeMessage\":\"Your message has been received and ready for processing\","
                + "\"yourreference\":\"CHEDD-12345\",\"unknownField\":\"ignored\"}"));

    try (ServiceBusSenderClient sender = new ServiceBusClientBuilder()
        .connectionString(serviceBus.getConnectionString())
        .sender()
        .queueName(NOTIFY_QUEUE)
        .buildClient()) {
      sender.sendMessage(new ServiceBusMessage(readResource(EMAIL_MESSAGE)));
      sender.sendMessage(new ServiceBusMessage(readResource(TEXT_MESSAGE)));
    }
    environment("keda").execute(() -> new KedaJobRunner().runOnce());

    NotifyFunction function = new NotifyFunction();
    ExecutionContext context = executionContext();
    environment("function").execute(() -> {
      function.notifyTextOrEmail(readResource(EMAIL_MESSAGE), context);
      function.notifyTextOrEmail(readResource(TEXT_MESSAGE), context);
    });
  }

  @AfterAll
  void tearDown() {
    serviceBus.stop();
    sqlServer.stop();
    mockServer.stop();
    network.close();
  }

  @ParameterizedTest
  @ValueSource(strings = {"keda", "function"})
  void tokenRequests(String entryPoint) {
    HttpRequest[] requests = recorded("/" + entryPoint + TOKEN_PATH);

    // One per message
    assertThat(requests).hasSize(2);
    for (HttpRequest tokenRequest : requests) {
      assertPayload("token_request_sent.txt", describe(entryPoint, tokenRequest));
    }
  }

  @ParameterizedTest
  @CsvSource({
      "keda, email",
      "keda, sms",
      "function, email",
      "function, sms"})
  void notifyRequest(String entryPoint, String type) {
    HttpRequest[] requests = recorded(
        "/" + entryPoint + NOTIFY_PATH + type.toUpperCase(Locale.ROOT) + ".*");

    assertThat(requests).hasSize(1);
    assertPayload(type + "_request_sent.txt", describe(entryPoint, requests[0]));
    assertThat(requests[0].getFirstHeader("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
    assertThat(requests[0].getFirstHeader("Ocp-Apim-Subscription-Key"))
        .isEqualTo(SUBSCRIPTION_KEY);
  }

  private EnvironmentVariables environment(String entryPoint) {
    String baseUrl = mockServer.getEndpoint() + "/" + entryPoint;

    return new EnvironmentVariables(Map.ofEntries(
        Map.entry("NOTIFY_QUEUE_NAME", NOTIFY_QUEUE),
        Map.entry("NOTIFY_QUEUE_CONNECTION_NAME", serviceBus.getConnectionString()),
        Map.entry("ENABLE_EMAIL_NOTIFICATION", "true"),
        Map.entry("ENABLE_TEXT_NOTIFICATION", "true"),
        Map.entry("TRADE_PLATFORM_AUTH_URL", baseUrl + TOKEN_PATH),
        Map.entry("TRADE_PLATFORM_NOTIFY_URL", baseUrl),
        Map.entry("TRADE_PLATFORM_CLIENT_ID", "payload-test-client"),
        Map.entry("TRADE_PLATFORM_CLIENT_SECRET", "payload-test-secret"),
        Map.entry("TRADE_PLATFORM_SCOPE", "payload-test-scope/.default"),
        Map.entry("TRADE_PLATFORM_SYSTEM_NAME", "IPAFFS"),
        Map.entry("TRADE_PLATFORM_SYSTEM_UNIQUE_ID", "3c1f4f0e-4bb6-4f49-a3a4-6e1f43a5a2a1"),
        Map.entry("TRADE_PLATFORM_SUBSCRIPTION_KEY", SUBSCRIPTION_KEY),
        Map.entry("API_VERSION", "payload-test"),
        // Give up after one empty receive, so the job exits soon after the two messages
        Map.entry("KEDA_MAX_EMPTY_RECEIVES", "1"),
        Map.entry("KEDA_RECEIVE_WAIT_SECONDS", "5")));
  }

  private HttpRequest[] recorded(String path) {
    return mockServerClient.retrieveRecordedRequests(
        request().withMethod("POST").withPath(path));
  }

  // The path without the entry point's prefix, the headers that describe the body, and the raw
  // body (getBodyAsString() pretty-prints JSON bodies)
  private static String describe(String entryPoint, HttpRequest recordedRequest) {
    String path = recordedRequest.getPath().getValue();
    assertThat(path).startsWith("/" + entryPoint + "/");
    return "%s %s%nContent-Type: %s%nAccept: %s%n%n%s".formatted(
        recordedRequest.getMethod().getValue(),
        path.substring(entryPoint.length() + 1),
        recordedRequest.getFirstHeader("Content-Type"),
        recordedRequest.getFirstHeader("Accept"),
        new String(recordedRequest.getBodyAsRawBytes(), StandardCharsets.UTF_8));
  }

  private static ExecutionContext executionContext() {
    Logger logger = Logger.getLogger("PayloadContractIT");
    return new ExecutionContext() {
      @Override
      public Logger getLogger() {
        return logger;
      }

      @Override
      public String getInvocationId() {
        return "";
      }

      @Override
      public String getFunctionName() {
        return "NotifyFunction";
      }
    };
  }

  private static void assertPayload(String expectedFile, String actual) {
    String expected = null;
    try (InputStream in = PayloadContractIT.class.getClassLoader()
        .getResourceAsStream(PAYLOAD_DIR + expectedFile)) {
      if (in != null) {
        expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read " + expectedFile, exception);
    }
    if (!actual.equals(expected)) {
      writeActual(expectedFile, actual);
    }
    assertThat(actual).as(expectedFile).isEqualTo(expected);
  }

  private static void writeActual(String name, String actual) {
    try {
      Path dir = Path.of("target", "payload-actual");
      Files.createDirectories(dir);
      Files.writeString(dir.resolve(name), actual);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to write the actual " + name, exception);
    }
  }

  private static String readResource(String name) {
    try (InputStream in = PayloadContractIT.class.getClassLoader().getResourceAsStream(name)) {
      if (in == null) {
        throw new IllegalStateException("Not on the test classpath: " + name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read " + name, exception);
    }
  }
}
