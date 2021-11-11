package uk.gov.defra.tracesx.notify.apimodel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SuccessResponse {

  private final String reference;
  private final String codeValue;
  private final String codeMessage;
  private final String yourreference;
}
