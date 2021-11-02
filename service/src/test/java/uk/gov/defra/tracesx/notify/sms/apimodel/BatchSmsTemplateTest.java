package uk.gov.defra.tracesx.notify.sms.apimodel;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

public class BatchSmsTemplateTest {

  private static final String NOTIFY_API_SMS_BATCH = "/Notify/Api/SMS (batch)";

  @Test
  public void getEndpoint_returnsCorrectEndpoint_whenInvoked() {
    final BatchSmsTemplate batchEmailTemplate = new BatchSmsTemplate("ref", "uniqueId",
        "templateId", "IPAFFS", Lists.emptyList());
    assertThat(batchEmailTemplate.getEndpoint()).isEqualTo(NOTIFY_API_SMS_BATCH);
  }
}