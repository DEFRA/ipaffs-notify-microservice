package uk.gov.defra.tracesx.notify.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.APPLICATION_JSON;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.testcontainers.azure.ServiceBusEmulatorContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mockserver.MockServerContainer;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Builds the service image from the real Dockerfile and runs it the way the KEDA ScaledJob does
 * (UID/GID 1000), against a Service Bus emulator and a MockServer standing in for the Trade
 * Platform's token endpoint and Notify API. An email message is waiting when the job starts. It
 * must get a token, submit the email to the Notify API with it, and exit cleanly. Catches
 * packaging problems (entrypoint, classpath, file permissions, missing environment variables)
 * before the image is pushed.
 *
 * <p>The image is built with the {@code docker build} CLI (BuildKit), exactly as the pipeline
 * builds it. The CLI must use the same Docker daemon as Testcontainers.
 *
 * <p>Requires the packaged function in {@code target/azure-functions}, so it must run after
 * {@code mvn package}, which Failsafe does. Base images are pulled from {@code IMAGE_ACR}
 * (default docker.io), as in the pipeline's docker build.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImageSmokeIT {

  private static final String KUBERNETES_USER = "1000:1000";
  private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(10);
  private static final String MOCK_SERVER_ALIAS = "mockserver";
  private static final String SERVICE_BUS_ALIAS = "servicebus";
  private static final String NOTIFY_QUEUE = "notify";
  private static final String TOKEN_PATH = "/oauth2/token";
  // Batch email and SMS endpoints, e.g. "/Notify/Api/EMAIL (batch)"
  private static final String NOTIFY_API_PATH = "/Notify/Api/.*";
  private static final String ACCESS_TOKEN = "smoke-test-token";
  private static final String SUBSCRIPTION_KEY = "smoke-test-subscription-key";
  // The notification reference in emailQueueMessage.json
  private static final String REFERENCE_NUMBER = "CHEDD-12345";

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
          .withNetworkAliases(SERVICE_BUS_ALIAS)
          .withMsSqlServerContainer(sqlServer);
  private final MockServerContainer mockServer =
      new MockServerContainer(DockerImageName.parse("mockserver/mockserver")
          .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion()))
          .withNetwork(network)
          .withNetworkAliases(MOCK_SERVER_ALIAS);

  private final String imageTag = "notify-smoke-test:" + UUID.randomUUID();
  private final ToStringConsumer jobLogs = new ToStringConsumer();
  private MockServerClient mockServerClient;

  @BeforeAll
  @SuppressWarnings("resource")
  void runServiceImage() throws IOException, InterruptedException {
    Startables.deepStart(sqlServer, mockServer).join();
    serviceBus.start();
    buildImage();

    mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
    mockServerClient
        .when(request().withMethod("POST").withPath(TOKEN_PATH))
        .respond(response().withContentType(APPLICATION_JSON).withBody(
            "{\"token_type\":\"Bearer\",\"expires_in\":\"3599\",\"access_token\":\"%s\"}"
                .formatted(ACCESS_TOKEN)));
    mockServerClient
        .when(request().withMethod("POST").withPath(NOTIFY_API_PATH))
        .respond(response().withContentType(APPLICATION_JSON)
            .withBody(readResource("trade-notify-responses/success_response.json")));
    try (ServiceBusSenderClient sender = new ServiceBusClientBuilder()
        .connectionString(serviceBus.getConnectionString())
        .sender()
        .queueName(NOTIFY_QUEUE)
        .buildClient()) {
      sender.sendMessage(new ServiceBusMessage(readResource("emailQueueMessage.json")));
    }

    GenericContainer<?> job = new GenericContainer<>(DockerImageName.parse(imageTag))
        .withNetwork(network)
        .withCreateContainerCmdModifier(cmd -> cmd.withUser(KUBERNETES_USER))
        .withEnv(jobEnvironment())
        .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("service-image"))
        .withLogConsumer(jobLogs)
        // start() returns once the job has exited, and fails unless the exit code was 0
        .withStartupCheckStrategy(
            new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(2)));
    try {
      job.start();
    } finally {
      job.stop();
    }
  }

  @AfterAll
  void tearDown() throws IOException, InterruptedException {
    serviceBus.stop();
    sqlServer.stop();
    mockServer.stop();
    network.close();
    docker("rmi", "--force", imageTag);
  }

  @Test
  void jobSendsTheEmailAndFinishes() {
    assertThat(jobLogs.toUtf8String())
        .contains("Successfully sent message to Notify API")
        .contains("Finished Notify queue. processed=1");
  }

  @Test
  void jobSubmitsTheEmailWithTheTradePlatformToken() {
    HttpRequest[] submissions = mockServerClient.retrieveRecordedRequests(
        request().withMethod("POST").withPath(NOTIFY_API_PATH));

    assertThat(submissions).singleElement().satisfies(submission -> {
      assertThat(submission.getFirstHeader("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
      assertThat(submission.getFirstHeader("Ocp-Apim-Subscription-Key"))
          .isEqualTo(SUBSCRIPTION_KEY);
      assertThat(submission.getBodyAsString()).contains(REFERENCE_NUMBER);
    });
  }

  private Map<String, String> jobEnvironment() {
    String mockServerUrl = "http://%s:%d".formatted(MOCK_SERVER_ALIAS, MockServerContainer.PORT);
    // The emulator's connection string, but via its network alias and container port
    String serviceBusConnection = serviceBus.getConnectionString().replace(
        "%s:%d".formatted(serviceBus.getHost(), serviceBus.getMappedPort(5672)),
        "%s:%d".formatted(SERVICE_BUS_ALIAS, 5672));

    return Map.ofEntries(
        Map.entry("NOTIFY_QUEUE_NAME", NOTIFY_QUEUE),
        Map.entry("NOTIFY_QUEUE_CONNECTION_NAME", serviceBusConnection),
        Map.entry("ENABLE_EMAIL_NOTIFICATION", "true"),
        Map.entry("ENABLE_TEXT_NOTIFICATION", "true"),
        Map.entry("TRADE_PLATFORM_AUTH_URL", mockServerUrl + TOKEN_PATH),
        Map.entry("TRADE_PLATFORM_NOTIFY_URL", mockServerUrl),
        Map.entry("TRADE_PLATFORM_CLIENT_ID", "integration-test"),
        Map.entry("TRADE_PLATFORM_CLIENT_SECRET", "integration-test"),
        Map.entry("TRADE_PLATFORM_SCOPE", "integration-test"),
        Map.entry("TRADE_PLATFORM_SYSTEM_NAME", "IPAFFS"),
        Map.entry("TRADE_PLATFORM_SYSTEM_UNIQUE_ID", UUID.randomUUID().toString()),
        Map.entry("TRADE_PLATFORM_SUBSCRIPTION_KEY", SUBSCRIPTION_KEY),
        Map.entry("API_VERSION", "integration-test"),
        // Give up after one empty receive, so the job exits soon after processing the message
        Map.entry("KEDA_MAX_EMPTY_RECEIVES", "1"),
        Map.entry("KEDA_RECEIVE_WAIT_SECONDS", "5"));
  }

  private static String readResource(String name) {
    try (InputStream in = ImageSmokeIT.class.getClassLoader().getResourceAsStream(name)) {
      if (in == null) {
        throw new IllegalStateException("Not on the test classpath: " + name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read " + name, exception);
    }
  }

  private void buildImage() throws IOException, InterruptedException {
    String imageAcr = System.getenv().getOrDefault("IMAGE_ACR", "docker.io");
    DockerResult build = docker("build", "--progress=plain",
        "--build-arg", "IMAGE_ACR=" + imageAcr, "--tag", imageTag, ".");
    if (build.exitCode() != 0) {
      throw new IllegalStateException(
          "docker build failed (exit %d):%n%s".formatted(build.exitCode(), build.output()));
    }
    log.debug("docker build output:\n{}", build.output());
  }

  private static DockerResult docker(String... args) throws IOException, InterruptedException {
    List<String> command = new ArrayList<>(List.of("docker"));
    command.addAll(List.of(args));
    // Output goes to a file so the process can't block on a full pipe and the timeout still applies
    Path outputFile = Files.createTempFile("docker-", ".log");
    try {
      Process process = new ProcessBuilder(command)
          .directory(Path.of("").toAbsolutePath().toFile())
          .redirectErrorStream(true)
          .redirectOutput(outputFile.toFile())
          .start();
      if (!process.waitFor(BUILD_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new IllegalStateException("Timed out running " + command);
      }
      return new DockerResult(process.exitValue(), Files.readString(outputFile));
    } finally {
      Files.deleteIfExists(outputFile);
    }
  }

  private record DockerResult(int exitCode, String output) {
  }
}
