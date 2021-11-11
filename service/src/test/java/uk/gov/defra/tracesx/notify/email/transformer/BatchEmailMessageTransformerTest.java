package uk.gov.defra.tracesx.notify.email.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import uk.gov.defra.tracesx.notify.apimodel.Template;
import uk.gov.defra.tracesx.notify.email.apimodel.BatchEmailTemplate;
import uk.gov.defra.tracesx.notify.model.MessagePersonalisation;
import uk.gov.defra.tracesx.notify.model.MessageType;
import uk.gov.defra.tracesx.notify.model.QueueMessage;

import java.util.UUID;

public class BatchEmailMessageTransformerTest {


  @Test
  public void transform_returnsCorrectTemplate_whenQueueMessageIsEmail() {
    final QueueMessage queueMessage = buildQueueMessage();
    final BatchEmailMessageTransformer messageTransformer = new BatchEmailMessageTransformer();
    Template transform = messageTransformer.transform(queueMessage, "IPAFFS", "111111");
    assertQueueMessageIsCorrectlyTransformed(queueMessage, transform);
  }

  private void assertQueueMessageIsCorrectlyTransformed(QueueMessage queueMessage,
      Template transform) {
    assertThat(transform).isInstanceOf(BatchEmailTemplate.class);
    assertThat(transform.getEndpoint()).isEqualTo("/Notify/Api/EMAIL (batch)");
    assertThat(transform.getTemplateId()).isEqualTo(queueMessage.getMessageTemplateId());
    assertThat(transform.getReference()).isEqualTo(
        queueMessage.getMessagePersonalisation().getReferenceNumber());
    assertThat(transform.getSystemName()).isEqualTo("IPAFFS");
    assertThat(transform.getSystemUniqueId()).isEqualTo("111111");
    assertThat(((BatchEmailTemplate) transform).getNotificationRequestEmail().size()).isEqualTo(2);
    assertThat(((BatchEmailTemplate) transform).getNotificationRequestEmail().get(0)
        .getEmailAddress()).isEqualTo("s.chandran@test.com");
    assertThat(((BatchEmailTemplate) transform).getNotificationRequestEmail().get(1)
        .getEmailAddress()).isEqualTo("s.c@gmail.com");
    assertThat(((BatchEmailTemplate) transform).getNotificationRequestEmail().get(0)
        .getContent()).isInstanceOf(MessagePersonalisation.class);
    assertThat(((BatchEmailTemplate) transform).getNotificationRequestEmail().get(1)
        .getContent()).isInstanceOf(MessagePersonalisation.class);
  }

  private QueueMessage buildQueueMessage() {
    final QueueMessage queueMessage = QueueMessage.builder().
        messageType(MessageType.TEXT)
        .messagePersonalisation(
            MessagePersonalisation.builder().referenceNumber("CHEDD-12345").bcpName("Wales")
                .build())
        .emails(Lists.newArrayList("s.chandran@test.com", "s.c@gmail.com")).
        messageTemplateId(UUID.randomUUID().toString()).build();
    return queueMessage;
  }
}