package uk.gov.defra.tracesx.notify.service;

import java.net.URI;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import uk.gov.defra.tracesx.notify.exception.NotifyException;
import uk.gov.defra.tracesx.notify.model.Token;

public class TradePlatformTokenGeneratorService {

  private static final String GRANT_TYPE_KEY = "grant_type";
  private static final String GRANT_TYPE_VALUE = "client_credentials";
  private static final String CLIENT_ID_KEY = "client_id";
  private static final String CLIENT_SECRET_KEY = "client_secret";
  private static final String RISK_SCOPE_KEY = "scope";

  private static final String EXCEPTION_MESSAGE =
      "Failed to retrieve Trade Platform auth token";

  private final String authUrl;
  private final String clientId;
  private final String clientSecret;
  private final String riskScope;
  private final WebClient webClient;
  private final Logger logger;

  public TradePlatformTokenGeneratorService(Logger logger, WebClient webClient,
      String authUrl, String clientId, String clientSecret, String riskScope) {
    this.logger = logger;
    this.authUrl = authUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.riskScope = riskScope;
    this.webClient = webClient;
  }

  public String generateToken() {
    var uri =
        UriComponentsBuilder.fromUri(URI.create(authUrl))
            .build()
            .encode()
            .toUri();

    var response = webClient.post()
        .uri(uri)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .body(BodyInserters.fromFormData(GRANT_TYPE_KEY, GRANT_TYPE_VALUE)
            .with(CLIENT_ID_KEY, clientId)
            .with(CLIENT_SECRET_KEY, clientSecret)
            .with(RISK_SCOPE_KEY, riskScope))
        .retrieve()
        .bodyToMono(Token.class)
        .doOnError(error -> logger.log(Level.SEVERE, "failed to retrieve auth token", error))
        .onErrorResume(error -> Mono.empty())
        .block();

    return Optional.ofNullable(response)
        .map(Token::getAccessToken)
        .orElseThrow(() -> new NotifyException(EXCEPTION_MESSAGE));
  }
}
