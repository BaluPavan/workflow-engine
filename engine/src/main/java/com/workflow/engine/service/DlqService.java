package com.workflow.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.domain.DlqTaskResult;
import com.workflow.engine.kafka.dto.TaskResultMessage;
import com.workflow.engine.repository.DlqTaskResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqService {

    private final DlqTaskResultRepository dlqRepository;
    private final WorkflowOrchestrator workflowOrchestrator;
    private final ObjectMapper objectMapper;

    @Transactional
    public DlqTaskResult saveToDlq(String rawPayload, String errorReason, UUID workflowInstanceId, UUID stepInstanceId, String stepName) {
        log.warn("Moving task result to DLQ: reason={}, workflowId={}", errorReason, workflowInstanceId);
        DlqTaskResult dlq = new DlqTaskResult();
        dlq.setRawPayload(rawPayload);
        dlq.setErrorReason(errorReason);
        dlq.setWorkflowInstanceId(workflowInstanceId);
        dlq.setStepInstanceId(stepInstanceId);
        dlq.setStepName(stepName);
        dlq.setStatus("UNRESOLVED");
        return dlqRepository.save(dlq);
    }

    @Transactional(readOnly = true)
    public List<DlqTaskResult> getUnresolvedMessages() {
        return dlqRepository.findByStatusOrderByCreatedAtDesc("UNRESOLVED");
    }

    @Transactional
    public void reDriveMessage(UUID dlqId) {
        DlqTaskResult dlq = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ record not found: " + dlqId));

        if ("RESOLVED".equals(dlq.getStatus())) {
            log.info("DLQ message {} is already resolved", dlqId);
            return;
        }

        try {
            TaskResultMessage message = objectMapper.readValue(dlq.getRawPayload(), TaskResultMessage.class);
            workflowOrchestrator.handleTaskResult(message);
            dlq.setStatus("RESOLVED");
            dlq.setResolvedAt(LocalDateTime.now());
            dlqRepository.save(dlq);
            log.info("Successfully re-driven DLQ message {}", dlqId);
        } catch (Exception ex) {
            log.error("Failed to re-drive DLQ message {}", dlqId, ex);
            throw new IllegalStateException("Failed to re-drive DLQ message: " + ex.getMessage(), ex);
        }
    }
}
