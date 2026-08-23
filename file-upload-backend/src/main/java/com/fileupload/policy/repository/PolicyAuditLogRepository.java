package com.fileupload.policy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fileupload.policy.domain.PolicyAuditLog;

public interface PolicyAuditLogRepository
    extends JpaRepository<PolicyAuditLog, Long> {
}