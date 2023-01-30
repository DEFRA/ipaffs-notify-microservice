package uk.gov.defra.tracesx.notify.sms.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import uk.gov.defra.tracesx.notify.apimodel.Template;
import uk.gov.defra.tracesx.notify.model.MessagePersonalisation;
import uk.gov.defra.tracesx.notify.model.MessageType;
import uk.gov.defra.tracesx.notify.model.QueueMessage;
import uk.gov.defra.tracesx.notify.sms.apimodel.BatchSmsTemplate;

import java.util.UUID;

public class BatchSmsMessageTransformerTest {

  @Test
  public void transform_returnsCorrectTemplate_whenQueueMessageIsText() {
    final QueueMessage queueMessage = buildQueueMessage();
    final BatchSmsMessageTransformer messageTransformer = new BatchSmsMessageTransformer();
    Template transform = messageTransformer.transform(queueMessage, "IPAFFS", "111111");
    assertQueueMessageIsCorrectlyTransformed(queueMessage, transform);
  }

  private void assertQueueMessageIsCorrectlyTransformed(QueueMessage queueMessage,
      Template transform) {
    assertThat(transform).isInstanceOf(BatchSmsTemplate.class);
    assertThat(transform.getEndpoint()).isEqualTo("/Notify/Api/SMS (batch)");
    assertThat(transform.getTemplateId()).isEqualTo(queueMessage.getMessageTemplateId());
    assertThat(transform.getReference()).isEqualTo(
        queueMessage.getMessagePersonalisation().getReferenceNumber());
    assertThat(transform.getSystemName()).isEqualTo("IPAFFS");
    assertThat(transform.getSystemUniqueId()).isEqualTo("111111");
    assertThat(((BatchSmsTemplate) transform).getNotificationRequestSms().size()).isEqualTo(2);
    assertThat(((BatchSmsTemplate) transform).getNotificationRequestSms().get(0)
        .getMobileNumber()).isEqualTo("12345678");
    assertThat(((BatchSmsTemplate) transform).getNotificationRequestSms().get(1)
        .getMobileNumber()).isEqualTo("87654321");
    assertThat(((BatchSmsTemplate) transform).getNotificationRequestSms().get(0)
        .getContent()).isInstanceOf(MessagePersonalisation.class);
    assertThat(((BatchSmsTemplate) transform).getNotificationRequestSms().get(1)
        .getContent()).isInstanceOf(MessagePersonalisation.class);
  }

  private QueueMessage buildQueueMessage() {
    UUID messageTemplateId = UUID.randomUUID();
    final QueueMessage queueMessage = QueueMessage.builder().
        messageType(MessageType.TEXT)
        .messagePersonalisation(
            MessagePersonalisation.builder().referenceNumber("CHEDD-12345").bcpName("Wales")
                .build())
        .phoneNumbers(Lists.newArrayList("12345678", "87654321")).
        messageTemplateId(messageTemplateId.toString()).build();
    return queueMessage;
  }
}