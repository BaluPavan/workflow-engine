package com.workflow.engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "workflow.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowScheduler {

    private final WorkflowOrchestrator workflowOrchestrator;

    @Scheduled(fixedDelayString = "${workflow.scheduler.fixed-delay-ms:10000}")
    public void pollRetriesAndTimeouts() {
        workflowOrchestrator.processDueRetries();
        workflowOrchestrator.processTimeouts();
    }
}
