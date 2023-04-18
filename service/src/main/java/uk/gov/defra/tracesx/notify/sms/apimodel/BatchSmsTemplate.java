package uk.gov.defra.tracesx.notify.sms.apimodel;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import uk.gov.defra.tracesx.notify.apimodel.Template;

@Getter
public class BatchSmsTemplate extends Template {

  private static final String NOTIFY_API_SMS_BATCH = "/Notify/Api/SMS (batch)";

  private final List<NotificationRequestSms> notificationRequestSms;

  @Builder
  public BatchSmsTemplate(final String reference, final String systemUniqueId,
      final String templateId,
      final String systemName, final List<NotificationRequestSms> notificationRequestSms) {
    super(reference, systemUniqueId, templateId, systemName);
    this.notificationRequestSms = notificationRequestSms;
  }

  @Override
  public String getEndpoint() {
    return NOTIFY_API_SMS_BATCH;
  }
}
