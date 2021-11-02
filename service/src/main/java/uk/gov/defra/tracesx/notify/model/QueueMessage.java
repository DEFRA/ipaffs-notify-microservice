package uk.gov.defra.tracesx.notify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


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
