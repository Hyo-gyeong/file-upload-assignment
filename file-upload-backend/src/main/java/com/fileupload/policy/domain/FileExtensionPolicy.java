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
@Table(name = "file_extension_policy")
public class FileExtensionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String extension;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_kind", nullable = false, length = 10)
    private PolicyKind policyKind;

    @Column(nullable = false)
    private boolean blocked;

    // 불필요한 JOIN/N+1/양방향 관계를 만들지 않음
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(
        name = "created_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private OffsetDateTime updatedAt;

    protected FileExtensionPolicy() {
    }

    public static FileExtensionPolicy createCustom(
        String extension,
        long actorUserId
    ) {
        FileExtensionPolicy policy = new FileExtensionPolicy();

        policy.extension = extension;
        policy.policyKind = PolicyKind.CUSTOM;
        policy.blocked = true;
        policy.createdBy = actorUserId;
        policy.updatedBy = actorUserId;

        return policy;
    }

    public void changeFixedBlocked(
        boolean blocked,
        long actorUserId
    ) {
        if (policyKind != PolicyKind.FIXED) {
            throw new IllegalStateException(
                "Only fixed extension policy can be toggled."
            );
        }

        this.blocked = blocked;
        this.updatedBy = actorUserId;
    }

    public Long getId() {
        return id;
    }

    public String getExtension() {
        return extension;
    }

    public PolicyKind getPolicyKind() {
        return policyKind;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}