package uk.gov.defra.tracesx.notify.email.apimodel;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

public class BatchEmailTemplateTest {

  @Test
  public void getEndpoint_ReturnsCorrectEndpoint_WhenInvoked() {
    final BatchEmailTemplate batchEmailTemplate = new BatchEmailTemplate("ref", "uniqueId",
        "templateId", "IPAFFS", Lists.emptyList());
    Assertions.assertThat(batchEmailTemplate.getEndpoint()).isEqualTo("/Notify/Api/EMAIL (batch)");
  }
}