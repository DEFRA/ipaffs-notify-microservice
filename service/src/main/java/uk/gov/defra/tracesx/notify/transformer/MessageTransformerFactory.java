package uk.gov.defra.tracesx.notify.transformer;

import lombok.experimental.UtilityClass;
import uk.gov.defra.tracesx.notify.email.transformer.BatchEmailMessageTransformer;
import uk.gov.defra.tracesx.notify.exception.NotifyException;
import uk.gov.defra.tracesx.notify.model.MessageType;
import uk.gov.defra.tracesx.notify.sms.transformer.BatchSmsMessageTransformer;

@UtilityClass
public class MessageTransformerFactory {

  public static MessageTransformer getTransformer(final MessageType messageType) {
    switch (messageType) {
      case EMAIL:
        return new BatchEmailMessageTransformer();
      case TEXT:
        return new BatchSmsMessageTransformer();
      default:
        throw new NotifyException(
            "No associated Message transformer exists for message type -->>" + messageType);
    }
  }
}
