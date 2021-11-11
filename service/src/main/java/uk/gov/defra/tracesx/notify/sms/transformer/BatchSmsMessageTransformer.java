package uk.gov.defra.tracesx.notify.sms.transformer;

import uk.gov.defra.tracesx.notify.apimodel.Template;
import uk.gov.defra.tracesx.notify.model.QueueMessage;
import uk.gov.defra.tracesx.notify.sms.apimodel.BatchSmsTemplate;
import uk.gov.defra.tracesx.notify.sms.apimodel.NotificationRequestSms;
import uk.gov.defra.tracesx.notify.transformer.MessageTransformer;

import java.util.List;
import java.util.stream.Collectors;

public class BatchSmsMessageTransformer implements MessageTransformer {

  @Override
  public Template transform(final QueueMessage queueMessage, final String systemName,
      final String systemUniqueId) {
    final List<NotificationRequestSms> notificationRequestSmsList = queueMessage.getPhoneNumbers()
        .stream().map(phoneNumber ->
            NotificationRequestSms.builder()
                .mobileNumber(phoneNumber)
                .content(queueMessage.getMessagePersonalisation())
                .build()
        ).collect(Collectors.toList());
    return BatchSmsTemplate.builder()
        .reference(queueMessage.getMessagePersonalisation().getReferenceNumber())
        .systemUniqueId(systemUniqueId)
        .templateId(queueMessage.getMessageTemplateId())
        .systemName(systemName)
        .notificationRequestSms(notificationRequestSmsList)
        .build();
  }
}
