package uk.gov.defra.tracesx.notify.apimodel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class Template {

  private String reference;
  private String systemUniqueId;
  private String templateId;
  private String systemName;

  public abstract String getEndpoint();
}
