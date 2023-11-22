package uk.gov.defra.tracesx.notify.sms.apimodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class BatchSmsTemplateTest {

  private static final String NOTIFY_API_SMS_BATCH = "/Notify/Api/SMS (batch)";

  @Test
  void getEndpoint_returnsCorrectEndpoint_whenInvoked() {
    final BatchSmsTemplate batchEmailTemplate = new BatchSmsTemplate("ref", "uniqueId",
        "templateId", "IPAFFS", Collections.emptyList());
    assertThat(batchEmailTemplate.getEndpoint()).isEqualTo(NOTIFY_API_SMS_BATCH);
  }
}