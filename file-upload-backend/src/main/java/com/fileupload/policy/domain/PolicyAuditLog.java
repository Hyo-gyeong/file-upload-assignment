package com.fileupload.policy.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "policy_audit_log")
public class PolicyAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "policy_id")
    private Long policyId;

    @Column(
        name = "extension_snapshot",
        nullable = false,
        length = 20
    )
    private String extensionSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PolicyAuditAction action;

    @Column(name = "before_blocked")
    private Boolean beforeBlocked;

    @Column(name = "after_blocked")
    private Boolean afterBlocked;

    @Column(length = 200)
    private String reason;

    @Column(
        name = "created_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    protected PolicyAuditLog() {
    }

    public PolicyAuditLog(
        long actorUserId,
        Long policyId,
        String extensionSnapshot,
        PolicyAuditAction action,
        Boolean beforeBlocked,
        Boolean afterBlocked,
        String reason
    ) {
        this.actorUserId = actorUserId;
        this.policyId = policyId;
        this.extensionSnapshot = extensionSnapshot;
        this.action = action;
        this.beforeBlocked = beforeBlocked;
        this.afterBlocked = afterBlocked;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public String getExtensionSnapshot() {
        return extensionSnapshot;
    }

    public PolicyAuditAction getAction() {
        return action;
    }

    public Boolean getBeforeBlocked() {
        return beforeBlocked;
    }

    public Boolean getAfterBlocked() {
        return afterBlocked;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}