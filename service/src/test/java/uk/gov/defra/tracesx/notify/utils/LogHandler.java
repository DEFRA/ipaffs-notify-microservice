package uk.gov.defra.tracesx.notify.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LogHandler extends Handler {

  private final List<LogRecord> logRecords = new ArrayList<>();

  public static LogHandler configure(Logger logger) {
    LogHandler logHandler = new LogHandler();
    logHandler.setLevel(Level.ALL);
    logger.addHandler(logHandler);
    logger.setLevel(Level.ALL);
    return logHandler;
  }

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
            prettyPrintLogs())
        .anySatisfy(logRecord -> {
          assertThat(logRecord.getLevel()).isEqualTo(level);
          assertThat(logRecord.getMessage()).isEqualTo(message);
        });
  }

  public void assertLogged(Level level, String message, String exceptionMessage) {
    assertThat(logRecords)
        .withFailMessage("The log '[%s] %s %s' was not found.\nActual logs:\n%s", level, message,
            exceptionMessage, prettyPrintLogs())
        .anySatisfy(logRecord -> {
          assertThat(logRecord.getLevel()).isEqualTo(level);
          assertThat(logRecord.getMessage()).isEqualTo(message);
          assertThat(logRecord.getThrown()).hasMessage(exceptionMessage);
        });
  }

  public void assertLogged(Level level, String message, Throwable cause) {
    assertThat(logRecords)
        .withFailMessage("The log '[%s] %s' cause '%s' was not found.\nActual logs:\n%s", level,
            message, cause, prettyPrintLogs())
        .anySatisfy(logRecord -> {
          assertThat(logRecord.getLevel()).isEqualTo(level);
          assertThat(logRecord.getMessage()).isEqualTo(message);
          assertThat(logRecord.getThrown().getClass()).isEqualTo(cause.getClass());
          assertThat(logRecord.getThrown().getMessage()).isEqualTo(cause.getMessage());
        });
  }


  public void assertNotLogged(Level level, String message) {
    assertThat(logRecords)
        .withFailMessage("The log '[%s] %s' was found, expected to be not logged.", level, message)
        .noneSatisfy(logRecord -> {
          assertThat(logRecord.getLevel()).isEqualTo(level);
          assertThat(logRecord.getMessage()).isEqualTo(message);
        });
  }

  private String prettyPrintLogs() {
    return logRecords.stream()
        .map(this::prettyPrintLog)
        .collect(Collectors.joining("\n"));
  }

  private String prettyPrintLog(LogRecord log) {
    return String.format("[%s] %s", log.getLevel(), log.getMessage());
  }
}
