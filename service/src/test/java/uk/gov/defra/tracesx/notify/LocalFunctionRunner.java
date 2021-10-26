package uk.gov.defra.tracesx.notify;

import com.microsoft.azure.functions.ExecutionContext;

import java.util.logging.Logger;

/**
 * This class is provided to facilitate running the NotifyFunction via the IDE (as opposed to
 * using Maven) without IDE plugins.
 */
public class LocalFunctionRunner {

  public static void main(String[] args) {
    ExecutionContext executionContext = new ExecutionContext() {
      private final Logger logger = Logger.getLogger("ExecutionContext");

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

    new NotifyFunction().notifyTextOrEmail("message recieved off the queue", executionContext);
  }
}
