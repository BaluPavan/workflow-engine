package com.workflow.worker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "workflow")
public class WorkerProperties {
    private Kafka kafka = new Kafka();
    private long processingDelayMs = 1000;

    @Data
    public static class Kafka {
        private Topic topic = new Topic();

        @Data
        public static class Topic {
            private String taskAssigned = "workflow.task.assigned";
            private String taskResult = "workflow.task.result";
        }
    }
}
