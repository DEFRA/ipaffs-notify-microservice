package uk.gov.defra.tracesx.notify.apiclient;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import uk.gov.defra.tracesx.notify.apimodel.SuccessResponse;
import uk.gov.defra.tracesx.notify.apimodel.Template;
import uk.gov.defra.tracesx.notify.exception.NotifyException;

import java.net.URI;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TradePlatformApiClient {

  private static final String SUBSCRIPTION_KEY_HEADER = "Ocp-Apim-Subscription-Key";
  private static final String AUTH_TOKEN_HEADER = "Authorization";

  private final Logger logger;
  private final WebClient webClient;
  private final String authenticationToken;
  private final URI tradeNotifyUrl;
  private final String tradePlatformSubscriptionKey;

  public TradePlatformApiClient(
      final Logger logger,
      final WebClient webClient,
      final String authenticationToken,
      final URI tradeNotifyUrl,
      final String tradePlatformSubscriptionKey) {
    this.logger = logger;
    this.webClient = webClient;
    this.authenticationToken = authenticationToken;
    this.tradeNotifyUrl = tradeNotifyUrl;
    this.tradePlatformSubscriptionKey = tradePlatformSubscriptionKey;
  }

  public SuccessResponse submitRequest(final Template template) {
    final URI uri =
        UriComponentsBuilder.fromUri(tradeNotifyUrl)
            .path(template.getEndpoint())
            .build()
            .encode()
            .toUri();
    final SuccessResponse successResponse = webClient
        .post()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .header(SUBSCRIPTION_KEY_HEADER, tradePlatformSubscriptionKey)
        .header(AUTH_TOKEN_HEADER, "Bearer " + authenticationToken)
        .bodyValue(template)
        .retrieve().bodyToMono(SuccessResponse.class)
        .doOnError(error -> logger.log(Level.SEVERE, error, () -> String.format(
            "Error sending to Trade for Notify for Notification reference %s with error %s ",
            template.getReference(), error)))
        .onErrorResume(e -> Mono.empty())
        .block();
    return Optional.ofNullable(successResponse)
        .orElseThrow(() -> new NotifyException(
            "Failed to send to Trade Notify for reference -->>" + template.getReference()));
  }
}
