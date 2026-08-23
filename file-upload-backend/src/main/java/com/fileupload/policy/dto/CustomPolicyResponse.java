package com.fileupload.policy.dto;

import java.time.OffsetDateTime;

import com.fileupload.policy.domain.FileExtensionPolicy;

public record CustomPolicyResponse(
    Long id,
    String extension,
    boolean blocked,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

    public static CustomPolicyResponse from(
        FileExtensionPolicy policy
    ) {
        return new CustomPolicyResponse(
            policy.getId(),
            policy.getExtension(),
            policy.isBlocked(),
            policy.getCreatedAt(),
            policy.getUpdatedAt()
        );
    }
}