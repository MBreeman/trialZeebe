import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.client.api.worker.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    static Logger log = LoggerFactory.getLogger("Main");
    public static void main(String[] args) {
        final ZeebeClient client = ZeebeClient.newClientBuilder()
                .brokerContactPoint("127.0.0.1:26500")
                .build();
        log.info("Opened connection: {}", client.getConfiguration());

        final DeploymentEvent deploymentEvent = client.newDeployCommand()
                .addResourceFromClasspath("order-process.bpmn")
                .send()
                .join();

        final int version = deploymentEvent.getWorkflows().get(0).getVersion();
        log.info("Workflow deployed. Version: {}", version);

        final String processId = deploymentEvent.getWorkflows().get(0).getBpmnProcessId();
        log.info("Workflow bpmnProcessId: {}", processId);


        final WorkflowInstanceEvent wfInstance = client.newCreateInstanceCommand()
                .bpmnProcessId(processId)
                .latestVersion()
                .send()
                .join();

        final long workFlowInstanceKey = wfInstance.getWorkflowInstanceKey();
        log.info("Workflow instance created. Key: {}", workFlowInstanceKey);

        final JobWorker jobWorker = client.newWorker()
                .jobType("payment-service")
                .handler((jobClient, job) ->
                {
                    System.out.println("Collect money");

                    // ...

                    jobClient.newCompleteCommand(job.getKey())
                            .send()
                            .join();
                })
                .open();


//        client.close();
//        log.info("Closed connection.");
    }
}
