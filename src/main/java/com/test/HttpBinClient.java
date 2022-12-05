package com.test;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class HttpBinClient {
  @Autowired
  private WebClient httpBinWebClientConfig;

  public void call() {
    log.info("In Thread {}", Thread.currentThread().getName());
    var response = httpBinWebClientConfig
      .get()
      .uri("get")
      .retrieve()
      .bodyToMono(JsonNode.class)
      .doFinally(e -> reproduceError())
      .block();
    try {
      Thread.sleep(10000); // Sleep http-nio-8080-exec-1 or ForkJoinPool.commonPool-worker-3
      log.info("Thread woke up {}", Thread.currentThread().getName());
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    log.info("In Thread {}", Thread.currentThread().getName());
    log.info("Received response {}", response);
  }

  private Mono<String> reproduceError() {
    printThreadDetails();
    log.info("In Thread {}", Thread.currentThread().getName());
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    try {
      Class clazz = tccl.loadClass("com.sun.xml.bind.v2.ContextFactory");
      log.info("!!!! Success !!!! loaded class {}", clazz);
    } catch (Exception e) {
      log.error("!!!! Error !!!!", e);
    }
    return Mono.just("Executed Web call");
  }

  private void printThreadDetails() {
    log.info("TCCL is {}", Thread.currentThread().getContextClassLoader());
    log.info("CL is {}", this.getClass().getClassLoader());
  }
}
