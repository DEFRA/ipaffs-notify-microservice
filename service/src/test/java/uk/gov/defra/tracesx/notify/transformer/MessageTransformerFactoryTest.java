package uk.gov.defra.tracesx.notify.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.defra.tracesx.notify.email.transformer.BatchEmailMessageTransformer;
import uk.gov.defra.tracesx.notify.model.MessageType;
import uk.gov.defra.tracesx.notify.sms.transformer.BatchSmsMessageTransformer;

public class MessageTransformerFactoryTest {

  @Test
  public void getTransformer_returnBatchEmailMessageTransformer_whenMessageTypeIsEmail(){
    MessageTransformer messageTransformer = MessageTransformerFactory.getTransformer(MessageType.EMAIL);
    assertThat(messageTransformer).isInstanceOf(BatchEmailMessageTransformer.class);
  }

  @Test
  public void getTransformer_returnBatchTextMessageTransformer_whenMessageTypeIsText(){
    MessageTransformer messageTransformer = MessageTransformerFactory.getTransformer(MessageType.TEXT);
    assertThat(messageTransformer).isInstanceOf(BatchSmsMessageTransformer.class);
  }
}