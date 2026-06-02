package uk.gov.defra.tracesx.notify;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for running this service as a KEDA ScaledJob.
 */
public class KedaJobRunner {

  private static final Logger LOGGER = Logger.getLogger(KedaJobRunner.class.getName());

  private static final String QUEUE_NAME_ENV = "NOTIFY_QUEUE_NAME";
  private static final String QUEUE_CONNECTION_ENV = "NOTIFY_QUEUE_CONNECTION_NAME";

  private static final String MAX_MESSAGES_ENV = "KEDA_MAX_MESSAGES";
  private static final String RECEIVE_WAIT_SECONDS_ENV = "KEDA_RECEIVE_WAIT_SECONDS";
  private static final String MAX_EMPTY_RECEIVES_ENV = "KEDA_MAX_EMPTY_RECEIVES";

  public static void main(String[] args) {
    try {
      new KedaJobRunner().runOnce();
    } catch (Exception exception) {
      LOGGER.log(Level.SEVERE, "KEDA job execution failed", exception);
      System.exit(1);
    }
  }

  void runOnce() {
    String queueName = requiredEnv(QUEUE_NAME_ENV);
    String connectionString = requiredEnv(QUEUE_CONNECTION_ENV);

    int maxMessages = intEnv(MAX_MESSAGES_ENV, 100);
    int receiveWaitSeconds = intEnv(RECEIVE_WAIT_SECONDS_ENV, 10);
    int maxEmptyReceives = intEnv(MAX_EMPTY_RECEIVES_ENV, 3);

    LOGGER.info(() -> String.format(
        "Starting KEDA job. queueName=%s, maxMessages=%d, receiveWaitSeconds=%d, "
            + "maxEmptyReceives=%d",
        queueName, maxMessages, receiveWaitSeconds, maxEmptyReceives));

    NotifyFunction notifyFunction = new NotifyFunction();
    ExecutionContext executionContext = new KedaExecutionContext(LOGGER);

    try (ServiceBusReceiverClient receiver = new ServiceBusClientBuilder()
        .connectionString(connectionString)
        .receiver()
        .queueName(queueName)
        .buildClient()) {

      int processed = 0;
      int emptyReceives = 0;

      while (processed < maxMessages && emptyReceives < maxEmptyReceives) {
        List<ServiceBusReceivedMessage> messages = receiver
            .receiveMessages(1, Duration.ofSeconds(receiveWaitSeconds))
            .stream()
            .toList();

        if (messages.isEmpty()) {
          emptyReceives++;
          continue;
        }

        emptyReceives = 0;
        ServiceBusReceivedMessage message = messages.getFirst();
        LOGGER.info(() -> String.format("Processing Notify message with id %s",
            message.getMessageId()));

        processMessage(notifyFunction, executionContext, message);
        receiver.complete(message);
        processed++;
      }

      LOGGER.info(String.format("Finished Notify queue. processed=%d, emptyReceives=%d",
          processed, emptyReceives));
    }
  }

  private void processMessage(NotifyFunction notifyFunction, ExecutionContext executionContext,
      ServiceBusReceivedMessage message) {
    try {
      notifyFunction.notifyTextOrEmail(message.getBody().toString(), executionContext);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to process Notify message", exception);
    }
  }

  private static String requiredEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Environment variable %s is required".formatted(name));
    }
    return value;
  }

  private static int intEnv(String name, int defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }

  private record KedaExecutionContext(Logger logger) implements ExecutionContext {

    @Override
    public Logger getLogger() {
      return logger;
    }

    @Override
    public String getInvocationId() {
      return "keda-job";
    }

    @Override
    public String getFunctionName() {
      return "NotifyFunction";
    }
  }
}

