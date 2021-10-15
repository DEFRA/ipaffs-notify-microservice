package uk.gov.defra.tracesx.notify;

import static org.mockito.Mockito.when;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.defra.tracesx.notify.utils.LogHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

@ExtendWith(MockitoExtension.class)
public class NotifyFunctionTest {

  @Mock
  private ExecutionContext context;

  private LogHandler logHandler;

  @BeforeEach
  public void setup() {
    logHandler = new LogHandler();
    Logger logger = Logger.getLogger("test logger");
    logger.setUseParentHandlers(false);
    logger.setLevel(Level.ALL);
    logger.addHandler(logHandler);
    when(context.getLogger()).thenReturn(logger);
  }

  @Test
  public void notifyFunction_logsQueueMessage_whenFunctionIsTriggered() {
    final NotifyFunction notifyFunction = new NotifyFunction();
    notifyFunction.notifyTextOrEmail("emailAddressToBeSent:test@test.com", context);
    logHandler.assertLogged(Level.INFO, "NotifyFunction starting");
    logHandler.assertLogged(Level.INFO, "Message recieved from queue -->>emailAddressToBeSent:test@test.com");
    logHandler.assertLogged(Level.INFO, "NotifyFunction stopping");
  }
}
