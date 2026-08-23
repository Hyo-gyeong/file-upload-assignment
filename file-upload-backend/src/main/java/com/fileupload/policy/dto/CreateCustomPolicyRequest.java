package com.fileupload.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomPolicyRequest(

    @NotBlank
    @Size(max = 64)
    String extension,

    @NotBlank
    @Size(max = 200)
    String reason

) {
}