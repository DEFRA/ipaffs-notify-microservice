package uk.gov.defra.tracesx.notify.email.apimodel;


import lombok.Builder;
import lombok.Getter;
import uk.gov.defra.tracesx.notify.apimodel.Template;

import java.util.List;

@Getter
public class BatchEmailTemplate extends Template {

  private static final String NOTIFY_API_EMAIL_BATCH = "/Notify/Api/EMAIL (batch)";

  private final List<NotificationRequestEmail> notificationRequestEmail;

  @Builder
  public BatchEmailTemplate(final String reference, final String systemUniqueId,
      final String templateId,
      final String systemName, final List<NotificationRequestEmail> notificationRequestEmail) {
    super(reference, systemUniqueId, templateId, systemName);
    this.notificationRequestEmail = notificationRequestEmail;
  }

  @Override
  public String getEndpoint() {
    return NOTIFY_API_EMAIL_BATCH;
  }
}
