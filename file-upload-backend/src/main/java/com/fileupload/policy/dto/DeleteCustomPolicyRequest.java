package com.fileupload.policy.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeleteCustomPolicyRequest(

    @NotNull
    OffsetDateTime expectedUpdatedAt,

    @Size(max = 200)
    String reason

) {
}