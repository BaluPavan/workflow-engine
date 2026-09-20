package com.workflow.engine.repository;

import com.workflow.engine.domain.DlqTaskResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DlqTaskResultRepository extends JpaRepository<DlqTaskResult, UUID> {
    List<DlqTaskResult> findByStatusOrderByCreatedAtDesc(String status);
}
