package com.test;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javax.net.ssl.SSLException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class HttpBinWebClientConfig {

  @Bean
  public WebClient webClientTest() throws SSLException {
    SslContext sslContext = SslContextBuilder
      .forClient()
      .trustManager(InsecureTrustManagerFactory.INSTANCE)
      .protocols("TLSv1.2")
      .build();

    return WebClient
      .builder()
      .baseUrl("https://httpbin.org/")
      .defaultHeaders(
        header -> {
          header.setContentType(MediaType.APPLICATION_JSON);
        }
      )
      .codecs(
        builder -> builder.defaultCodecs().maxInMemorySize(12 * 1024 * 1024)
      )
      .clientConnector(
        new ReactorClientHttpConnector(
          HttpClient
            .create()
            .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))
        )
      )
      .build();
  }
}
