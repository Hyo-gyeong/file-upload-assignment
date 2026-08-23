package com.fileupload.policy.dto;

import java.time.OffsetDateTime;

import com.fileupload.policy.domain.FileExtensionPolicy;

public record FixedPolicyResponse(
    Long id,
    String extension,
    boolean blocked,
    OffsetDateTime updatedAt
) {

    public static FixedPolicyResponse from(
        FileExtensionPolicy policy
    ) {
        return new FixedPolicyResponse(
            policy.getId(),
            policy.getExtension(),
            policy.isBlocked(),
            policy.getUpdatedAt()
        );
    }
}