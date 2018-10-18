package io.berndruecker.demo.zeebe.loadtest.starter;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import io.zeebe.client.ZeebeClient;

public class Starter {
  
  static AtomicInteger amount = new AtomicInteger();
  
  @Autowired
  private ZeebeClient zeebeClient;

  @PostConstruct
  public void go() {
    measureTime();
    while (true) {
      startInstance("{\"hello\":\"test\"}");
    }
  }

  private void startInstance(String payload) {
    zeebeClient.workflowClient()
        .newCreateInstanceCommand()
        .bpmnProcessId("simple-workflow")
        .latestVersion()
        .payload(payload)
        .send();
    amount.incrementAndGet();
  }

  private static void measureTime() {
    Timer t = new Timer();
    t.schedule(new TimerTask() {
      @Override
      public void run() {
        Date date = new Date();
        int andSet = amount.getAndSet(0);
        System.out.println(date.toString() + ": " + andSet);
      }
    }, 0, 10000);
  }
}
