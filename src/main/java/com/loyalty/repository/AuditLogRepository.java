package com.loyalty.repository;

import com.loyalty.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityAndEntityIdOrderByCreatedAtDesc(String entity, Long entityId);
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
