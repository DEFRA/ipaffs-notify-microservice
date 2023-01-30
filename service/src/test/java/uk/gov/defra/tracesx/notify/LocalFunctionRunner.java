package uk.gov.defra.tracesx.notify;

import com.microsoft.azure.functions.ExecutionContext;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * This class is provided to facilitate running the NotifyFunction via the IDE (as opposed to using
 * Maven) without IDE plugins.
 */
public class LocalFunctionRunner {

  public static void main(String[] args) throws IOException {
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

    new NotifyFunction().notifyTextOrEmail(
        IOUtils.toString(LocalFunctionRunner.class.getResourceAsStream("/emailQueueMessage.json"),
            StandardCharsets.UTF_8)
        , executionContext);
  }
}
