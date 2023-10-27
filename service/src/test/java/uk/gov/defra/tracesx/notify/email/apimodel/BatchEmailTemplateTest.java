package uk.gov.defra.tracesx.notify.email.apimodel;

import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class BatchEmailTemplateTest {

  @Test
  void getEndpoint_ReturnsCorrectEndpoint_WhenInvoked() {
    final BatchEmailTemplate batchEmailTemplate = new BatchEmailTemplate("ref", "uniqueId",
        "templateId", "IPAFFS", Collections.emptyList());
    Assertions.assertThat(batchEmailTemplate.getEndpoint()).isEqualTo("/Notify/Api/EMAIL (batch)");
  }
}