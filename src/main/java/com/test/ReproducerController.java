package com.test;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/test/")
public class ReproducerController {
  @Autowired
  private HttpBinClient httpBinClient;

  @GetMapping("sendGet")
  public void sendGet() {
    log.info("In Thread {}", Thread.currentThread().getName());
    printThreadDetails();
    httpBinClient.call();
    printThreadDetails();
  }

  @GetMapping("sendAsyncGet")
  public void sendAsyncGet() throws InterruptedException {
    log.info("In Thread {}", Thread.currentThread().getName());
    printThreadDetails();
    CompletableFuture.supplyAsync(
      () -> {
        log.info("In Thread {}", Thread.currentThread().getName());
        printThreadDetails();
        httpBinClient.call();
        return "CompletableFuture";
      }
    );
    Thread.sleep(20000); // Sleep http-nio-8080-exec-1
    log.info("Thread woke up {}", Thread.currentThread().getName());
  }

  private void printThreadDetails() {
    log.info("TCCL is {}", Thread.currentThread().getContextClassLoader());
    log.info("CL is {}", this.getClass().getClassLoader());
  }
}
