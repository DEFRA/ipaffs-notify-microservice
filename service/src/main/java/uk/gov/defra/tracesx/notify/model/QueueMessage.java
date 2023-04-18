package uk.gov.defra.tracesx.notify.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueMessage {

  private MessageType messageType;
  private List<String> emails;
  private List<String> phoneNumbers;
  private String messageTemplateId;
  private MessagePersonalisation messagePersonalisation;
}
