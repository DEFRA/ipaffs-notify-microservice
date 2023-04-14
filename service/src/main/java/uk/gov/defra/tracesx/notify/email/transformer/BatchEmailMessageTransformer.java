package uk.gov.defra.tracesx.notify.email.transformer;

import java.util.List;
import java.util.stream.Collectors;
import uk.gov.defra.tracesx.notify.apimodel.Template;
import uk.gov.defra.tracesx.notify.email.apimodel.BatchEmailTemplate;
import uk.gov.defra.tracesx.notify.email.apimodel.NotificationRequestEmail;
import uk.gov.defra.tracesx.notify.model.QueueMessage;
import uk.gov.defra.tracesx.notify.transformer.MessageTransformer;

public class BatchEmailMessageTransformer implements MessageTransformer {

  @Override
  public Template transform(final QueueMessage queueMessage, final String systemName,
      final String systemUniqueId) {
    final List<NotificationRequestEmail> notificationRequestEmailList = queueMessage.getEmails()
        .stream().map(email ->
            NotificationRequestEmail.builder()
                .emailAddress(email)
                .content(queueMessage.getMessagePersonalisation())
                .build()
        ).collect(Collectors.toList());
    return BatchEmailTemplate.builder()
        .reference(queueMessage.getMessagePersonalisation().getReferenceNumber())
        .systemUniqueId(systemUniqueId)
        .templateId(queueMessage.getMessageTemplateId())
        .systemName(systemName)
        .notificationRequestEmail(notificationRequestEmailList)
        .build();
  }
}
