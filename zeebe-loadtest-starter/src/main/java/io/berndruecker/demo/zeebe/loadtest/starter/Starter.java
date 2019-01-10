package io.berndruecker.demo.zeebe.loadtest.starter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.berndruecker.demo.zeebe.loadtest.starter.copypaste.MeasurementCollector;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.commands.Workflow;

@Component
public class Starter {

  @Autowired
  private ZeebeClient zeebeClient;

  @Autowired
  private MeasurementCollector measure;

  @Value("${loadtest.payloadFileUrl:#{null}}")
  private String payloadFileUrl = null;
  
  @Value("${loadtest.bpmnProcess:sample-load-generation-workflow}")
  private String bpmnProcessId="sample-load-generation-workflow";
  
  @Value("${loadtest.bpmnProcessFileUrl:#{null}}")
  private String bpmnProcessFileUrl = null;

  @PostConstruct
  public void go() throws Exception {
    deployWorkflowDefinition();
    
    String payload = "{\"hello\":\"world\"}"; // default
    if (payloadFileUrl != null) {
      payload = readFromUrl(payloadFileUrl);
    }

    measure.start();
    while (true) {
      startInstance(payload);
      measure.increment();
    }
  }
  
  private void deployWorkflowDefinition() throws Exception {
    // check if workflow is already deployed:
    List<Workflow> workflows = zeebeClient.workflowClient() //
        .newWorkflowRequest() //
        .send().join() //
        .getWorkflows();
    
    boolean workflowDeployed = workflows.stream()
        .anyMatch(workflow -> workflow.getBpmnProcessId().equals(bpmnProcessId));
    
    if (!workflowDeployed) {
      if (bpmnProcessFileUrl!=null) { // deploy model from URL
        zeebeClient.workflowClient().newDeployCommand() //
          .addResourceStream(new URL(bpmnProcessFileUrl).openStream(), bpmnProcessId + ".bpmn") //
          .send().join();        
      } else { // deploy default model from classpath
        zeebeClient.workflowClient().newDeployCommand() //
          .addResourceFromClasspath("sample-load-generation-workflow.bpmn") //
          .send().join();        
      }
      System.out.println("...deployed workflow definition successfully...");
    }
    
  }

  private void startInstance(String payload) {
    try {
      zeebeClient.workflowClient().newCreateInstanceCommand() //
          .bpmnProcessId(bpmnProcessId) //
          .latestVersion() //
          .payload(payload) //
          .send().join();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static String readFromUrl(String url) throws Exception {
    InputStream is = new URL(url).openStream();
    try {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

      StringBuilder sb = new StringBuilder();
      int cp;
      while ((cp = rd.read()) != -1) {
        sb.append((char) cp);
      }
      return sb.toString();
    } finally {
      is.close();
    }
  }

}
