package uk.gov.defra.tracesx.notify.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class LogHandler extends Handler {

  private final List<LogRecord> logRecords = new ArrayList<>();

  @Override
  public void publish(LogRecord record) {
    logRecords.add(record);
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() throws SecurityException {
  }

  public void assertLogged(Level level, String message) {
    assertThat(logRecords)
        .withFailMessage("The log '[%s] %s' was not found.\nActual logs:\n%s", level, message,
            logRecords.stream()
                .map(this::prettyPrintLog)
                .collect(Collectors.joining("\n")))
        .anyMatch(logRecord -> logRecord.getLevel().equals(level)
            && logRecord.getMessage().equals(message));
  }

  private String prettyPrintLog(LogRecord log) {
    return String.format("[%s] %s", log.getLevel(), log.getMessage());
  }
}
