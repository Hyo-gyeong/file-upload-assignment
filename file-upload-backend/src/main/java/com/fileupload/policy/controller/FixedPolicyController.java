package com.fileupload.policy.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fileupload.auth.security.AuthenticatedUser;
import com.fileupload.policy.dto.FixedPolicyResponse;
import com.fileupload.policy.dto.UpdateFixedPolicyRequest;
import com.fileupload.policy.service.FixedPolicyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/policies/fixed")
public class FixedPolicyController {

    private final FixedPolicyService fixedPolicyService;

    public FixedPolicyController(
        FixedPolicyService fixedPolicyService
    ) {
        this.fixedPolicyService = fixedPolicyService;
    }

    @GetMapping
    public List<FixedPolicyResponse> findAll() {
        return fixedPolicyService.findAll();
    }

    @PatchMapping("/{policyId}")
    public ResponseEntity<FixedPolicyResponse> update(
        @PathVariable Long policyId,
        @Valid @RequestBody UpdateFixedPolicyRequest request,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {

        FixedPolicyResponse response =
            fixedPolicyService.update(
                policyId,
                request,
                user.getId()
            );

        return ResponseEntity.ok(response);
    }
}