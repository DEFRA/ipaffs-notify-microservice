package uk.gov.defra.tracesx.notify.transformer;

import uk.gov.defra.tracesx.notify.apimodel.Template;
import uk.gov.defra.tracesx.notify.model.QueueMessage;

public interface MessageTransformer {

  Template transform(final QueueMessage queueMessage, final String systemName,
      final String systemUniqueId);
}
