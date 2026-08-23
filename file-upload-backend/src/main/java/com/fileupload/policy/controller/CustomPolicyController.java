package com.fileupload.policy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fileupload.auth.security.AuthenticatedUser;
import com.fileupload.policy.dto.CreateCustomPolicyRequest;
import com.fileupload.policy.dto.CustomPolicyPageResponse;
import com.fileupload.policy.dto.CustomPolicyResponse;
import com.fileupload.policy.dto.DeleteCustomPolicyRequest;
import com.fileupload.policy.service.CustomPolicyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/policies/custom")
public class CustomPolicyController {

    private final CustomPolicyService customPolicyService;

    public CustomPolicyController(
        CustomPolicyService customPolicyService
    ) {
        this.customPolicyService = customPolicyService;
    }

    @GetMapping
    public CustomPolicyPageResponse findAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String initial
    ) {
        return customPolicyService.findAll(
            page,
            size,
            search,
            initial
        );
    }

    @PostMapping
    public ResponseEntity<CustomPolicyResponse> create(
        @Valid @RequestBody
        CreateCustomPolicyRequest request,
        @AuthenticationPrincipal
        AuthenticatedUser user
    ) {
        CustomPolicyResponse response =
            customPolicyService.create(
                request,
                user.getId()
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> delete(
        @PathVariable Long policyId,
        @Valid @RequestBody
        DeleteCustomPolicyRequest request,
        @AuthenticationPrincipal
        AuthenticatedUser user
    ) {
        customPolicyService.delete(
            policyId,
            request,
            user.getId()
        );

        return ResponseEntity.noContent().build();
    }
}