package uk.gov.defra.tracesx.notify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
@AllArgsConstructor
public class MessagePersonalisation {

  private final String referenceNumber;
  private final String bcpName;
}
