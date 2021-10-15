package uk.gov.defra.tracesx.notify;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

import java.util.logging.Logger;

public class NotifyFunction {

  @FunctionName("NotifyFunction")
  public void notifyTextOrEmail(
      @ServiceBusQueueTrigger(name = "message",
          queueName = "%NOTIFY_QUEUE_NAME%",
          connection = "NOTIFY_QUEUE_CONNECTION_NAME")
          String message, final ExecutionContext context) {
    Logger logger = context.getLogger();
    logger.info("NotifyFunction starting");
    logger.info("Message recieved from queue -->>" + message);
    logger.info("NotifyFunction stopping");
  }
}