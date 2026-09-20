package com.workflow.engine.api;

import com.workflow.engine.domain.DlqTaskResult;
import com.workflow.engine.service.DlqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
public class DlqController {

    private final DlqService dlqService;

    @GetMapping
    public ResponseEntity<List<DlqTaskResult>> getUnresolvedDlqMessages() {
        return ResponseEntity.ok(dlqService.getUnresolvedMessages());
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, String>> retryDlqMessage(@PathVariable UUID id) {
        dlqService.reDriveMessage(id);
        return ResponseEntity.ok(Map.of("message", "Successfully re-driven DLQ message " + id));
    }
}
