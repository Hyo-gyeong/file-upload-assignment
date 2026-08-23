package com.fileupload.policy.service;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fileupload.common.exception.CustomExtensionLimitException;
import com.fileupload.common.exception.DuplicateExtensionException;
import com.fileupload.common.exception.InvalidExtensionException;
import com.fileupload.common.exception.PolicyConflictException;
import com.fileupload.common.exception.PolicyNotFoundException;
import com.fileupload.policy.domain.FileExtensionPolicy;
import com.fileupload.policy.domain.PolicyAuditAction;
import com.fileupload.policy.domain.PolicyAuditLog;
import com.fileupload.policy.domain.PolicyKind;
import com.fileupload.policy.dto.CreateCustomPolicyRequest;
import com.fileupload.policy.dto.CustomPolicyPageResponse;
import com.fileupload.policy.dto.CustomPolicyResponse;
import com.fileupload.policy.dto.DeleteCustomPolicyRequest;
import com.fileupload.policy.repository.FileExtensionPolicyRepository;
import com.fileupload.policy.repository.PolicyAuditLogRepository;

import jakarta.persistence.EntityManager;

@Service
public class CustomPolicyService {

    private static final int MAX_CUSTOM_COUNT = 200;
    private static final int MAX_PAGE_SIZE = 50;

    private static final Pattern EXTENSION_PATTERN =
        Pattern.compile("^[a-z0-9]{1,20}$");

    private static final Pattern INITIAL_PATTERN =
        Pattern.compile("^[a-z0-9]$");

    private final FileExtensionPolicyRepository policyRepository;
    private final PolicyAuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    public CustomPolicyService(
        FileExtensionPolicyRepository policyRepository,
        PolicyAuditLogRepository auditLogRepository,
        EntityManager entityManager
    ) {
        this.policyRepository = policyRepository;
        this.auditLogRepository = auditLogRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public CustomPolicyPageResponse findAll(
        int page,
        int size,
        String search,
        String initial
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                "Invalid pagination."
            );
        }

        String normalizedSearch = normalizeSearch(search);
        String normalizedInitial = normalizeInitial(initial);

        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by("extension").ascending()
        );

        Page<FileExtensionPolicy> result =
            policyRepository.searchByPolicyKind(
                PolicyKind.CUSTOM,
                normalizedSearch,
                normalizedInitial,
                pageable
            );

        long totalCustomCount =
            policyRepository.countByPolicyKind(PolicyKind.CUSTOM);

        return new CustomPolicyPageResponse(
            result.getContent()
                .stream()
                .map(CustomPolicyResponse::from)
                .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            totalCustomCount
        );
    }

    @Transactional
    public CustomPolicyResponse create(
        CreateCustomPolicyRequest request,
        Long actorUserId
    ) {
        String extension =
            normalizeExtension(request.extension());

        String reason =
            normalizeRequiredReason(request.reason());

        /*
         * 빠른 UX용 사전 검사.
         * 최종 중복 보장은 DB UNIQUE 제약조건이 담당한다.
         */
        if (policyRepository.existsByExtension(extension)) {
            throw new DuplicateExtensionException();
        }

        /*
         * 빠른 UX용 사전 검사.
         * 동시 요청에서 201번째 insert를 막는 최종 방어선은
         * PostgreSQL trigger + advisory lock이다.
         */
        if (policyRepository.countByPolicyKind(PolicyKind.CUSTOM)
            >= MAX_CUSTOM_COUNT) {

            throw new CustomExtensionLimitException();
        }

        FileExtensionPolicy policy =
            FileExtensionPolicy.createCustom(
                extension,
                actorUserId
            );

        try {
            policyRepository.saveAndFlush(policy);

        } catch (DataIntegrityViolationException exception) {

            throw translateInsertException(exception);
        }

        /*
         * created_at / updated_at은 DB default로 생성되므로
         * INSERT 후 실제 DB 값을 Entity에 다시 반영한다.
         */
        entityManager.refresh(policy);

        auditLogRepository.save(
            new PolicyAuditLog(
                actorUserId,
                policy.getId(),
                policy.getExtension(),
                PolicyAuditAction.CUSTOM_ADD,
                null,
                true,
                reason
            )
        );

        return CustomPolicyResponse.from(policy);
    }

    @Transactional
    public void delete(
        Long policyId,
        DeleteCustomPolicyRequest request,
        Long actorUserId
    ) {
        FileExtensionPolicy current =
            policyRepository.findById(policyId)
                .filter(policy ->
                    policy.getPolicyKind() == PolicyKind.CUSTOM
                )
                .orElseThrow(PolicyNotFoundException::new);

        assertExpectedUpdatedAt(
            current.getUpdatedAt(),
            request.expectedUpdatedAt()
        );

        String reason =
            normalizeOptionalReason(request.reason());

        /*
         * 먼저 audit를 INSERT한다.
         *
         * 이후 DELETE가 충돌하면 transaction 전체가 rollback되므로
         * audit도 남지 않는다.
         *
         * DELETE 성공 시 FK ON DELETE SET NULL에 의해
         * audit.policy_id는 null이 되고 extension_snapshot은 유지된다.
         */
        auditLogRepository.saveAndFlush(
            new PolicyAuditLog(
                actorUserId,
                current.getId(),
                current.getExtension(),
                PolicyAuditAction.CUSTOM_DELETE,
                true,
                null,
                reason
            )
        );

        int deletedCount =
            policyRepository.deleteIfUnchanged(
                policyId,
                PolicyKind.CUSTOM,
                request.expectedUpdatedAt()
            );

        if (deletedCount != 1) {
            throw new PolicyConflictException();
        }
    }

    private String normalizeExtension(String rawExtension) {

        String extension =
            rawExtension.trim();

        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }

        extension =
            extension.toLowerCase(Locale.ROOT);

        if (!EXTENSION_PATTERN.matcher(extension).matches()) {
            throw new InvalidExtensionException();
        }

        return extension;
    }

    private String normalizeSearch(String search) {

        if (search == null || search.isBlank()) {
            return "";
        }

        String normalized =
            search.trim().toLowerCase(Locale.ROOT);

        if (!EXTENSION_PATTERN.matcher(normalized).matches()) {
            throw new InvalidExtensionException();
        }

        return normalized;
    }

    private String normalizeInitial(String initial) {

        if (initial == null || initial.isBlank()) {
            return "";
        }

        String normalized =
            initial.trim().toLowerCase(Locale.ROOT);

        if (!INITIAL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidExtensionException();
        }

        return normalized;
    }

    private String normalizeRequiredReason(String reason) {

        String normalized =
            reason == null ? "" : reason.trim();

        if (normalized.isEmpty()
            || normalized.length() > 200) {

            throw new IllegalArgumentException(
                "Invalid audit reason."
            );
        }

        return normalized;
    }

    private String normalizeOptionalReason(String reason) {

        if (reason == null) {
            return null;
        }

        String normalized = reason.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }

    private void assertExpectedUpdatedAt(
        OffsetDateTime actual,
        OffsetDateTime expected
    ) {
        if (!actual.toInstant().equals(expected.toInstant())) {
            throw new PolicyConflictException();
        }
    }

    private RuntimeException translateInsertException(
        DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {

            if (cause instanceof SQLException sqlException) {

                if ("23505".equals(sqlException.getSQLState())) {
                    return new DuplicateExtensionException();
                }

                /*
                 * V1 custom-extension limit trigger에서
                 * ERRCODE 23514로 발생시킨다.
                 */
                if ("23514".equals(sqlException.getSQLState())
                    && sqlException.getMessage() != null
                    && sqlException.getMessage().contains(
                        "custom extension limit exceeded"
                    )) {

                    return new CustomExtensionLimitException();
                }
            }

            cause = cause.getCause();
        }

        return exception;
    }
}