package com.fileupload.policy.dto;

import java.util.List;

public record CustomPolicyPageResponse(
    List<CustomPolicyResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    long totalCustomCount
) {
}