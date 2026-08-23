package com.fileupload.policy.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fileupload.common.exception.PolicyConflictException;
import com.fileupload.common.exception.PolicyNotFoundException;
import com.fileupload.policy.domain.FileExtensionPolicy;
import com.fileupload.policy.domain.PolicyAuditAction;
import com.fileupload.policy.domain.PolicyAuditLog;
import com.fileupload.policy.domain.PolicyKind;
import com.fileupload.policy.dto.FixedPolicyResponse;
import com.fileupload.policy.dto.UpdateFixedPolicyRequest;
import com.fileupload.policy.repository.FileExtensionPolicyRepository;
import com.fileupload.policy.repository.PolicyAuditLogRepository;

@Service
public class FixedPolicyService {

    private final FileExtensionPolicyRepository policyRepository;
    private final PolicyAuditLogRepository auditLogRepository;

    public FixedPolicyService(
        FileExtensionPolicyRepository policyRepository,
        PolicyAuditLogRepository auditLogRepository
    ) {
        this.policyRepository = policyRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<FixedPolicyResponse> findAll() {

        return policyRepository
            .findAllByPolicyKindOrderByIdAsc(PolicyKind.FIXED)
            .stream()
            .map(FixedPolicyResponse::from)
            .toList();
    }

    @Transactional
    public FixedPolicyResponse update(
        Long policyId,
        UpdateFixedPolicyRequest request,
        Long actorUserId
    ) {
        FileExtensionPolicy current =
            policyRepository.findById(policyId)
                .filter(policy ->
                    policy.getPolicyKind() == PolicyKind.FIXED
                )
                .orElseThrow(PolicyNotFoundException::new);

        assertExpectedUpdatedAt(
            current.getUpdatedAt(),
            request.expectedUpdatedAt()
        );

        /*
         * 같은 상태를 다시 요청한 경우 불필요한 UPDATE와
         * 감사 로그 생성을 하지 않는다.
         */
        if (current.isBlocked() == request.blocked()) {
            return FixedPolicyResponse.from(current);
        }

        int updatedCount =
            policyRepository.updateBlockedIfUnchanged(
                policyId,
                PolicyKind.FIXED,
                request.blocked(),
                actorUserId,
                request.expectedUpdatedAt()
            );

        /*
         * SELECT 이후 UPDATE 사이에 다른 관리자가 변경했으면
         * updated_at 조건이 맞지 않아 0건이 갱신된다.
         */
        if (updatedCount != 1) {
            throw new PolicyConflictException();
        }

        PolicyAuditAction action =
            request.blocked()
                ? PolicyAuditAction.FIXED_BLOCK
                : PolicyAuditAction.FIXED_UNBLOCK;

        String reason = normalizeReason(request.reason());

        auditLogRepository.save(
            new PolicyAuditLog(
                actorUserId,
                current.getId(),
                current.getExtension(),
                action,
                current.isBlocked(),
                request.blocked(),
                reason
            )
        );

        FileExtensionPolicy updated =
            policyRepository.findById(policyId)
                .orElseThrow(PolicyNotFoundException::new);

        return FixedPolicyResponse.from(updated);
    }

    private void assertExpectedUpdatedAt(
        OffsetDateTime actual,
        OffsetDateTime expected
    ) {
        if (!actual.toInstant().equals(expected.toInstant())) {
            throw new PolicyConflictException();
        }
    }

    private String normalizeReason(String reason) {

        if (reason == null) {
            return null;
        }

        String normalized = reason.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }
}