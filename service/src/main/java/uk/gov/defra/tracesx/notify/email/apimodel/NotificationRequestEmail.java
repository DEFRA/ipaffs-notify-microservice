package uk.gov.defra.tracesx.notify.email.apimodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationRequestEmail {

  private final String emailAddress;
  private final Object content;
}
