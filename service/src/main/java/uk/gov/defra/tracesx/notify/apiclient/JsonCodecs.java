package uk.gov.defra.tracesx.notify.apiclient;

import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON codecs for the service's WebClients. They keep the settings the clients had on Spring 6:
 * Jackson 2's defaults, as Spring's Jackson2ObjectMapperBuilder changed them, with its parameter
 * names module.
 */
public final class JsonCodecs {

  private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
      .configureForJackson2()
      .enable(MapperFeature.DETECT_PARAMETER_NAMES)
      .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .build();

  private JsonCodecs() {
  }

  public static void withJackson2Defaults(ClientCodecConfigurer configurer) {
    configurer.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(JSON_MAPPER));
    configurer.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(JSON_MAPPER));
  }
}
