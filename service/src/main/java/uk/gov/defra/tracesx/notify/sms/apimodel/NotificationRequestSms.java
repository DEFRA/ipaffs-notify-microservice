package uk.gov.defra.tracesx.notify.sms.apimodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationRequestSms {

  private final String mobileNumber;
  private final Object content;
}
