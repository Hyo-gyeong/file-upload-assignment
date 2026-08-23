package com.fileupload.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fileupload.common.exception.FileUploadException;
import com.fileupload.file.domain.StoredFile;
import com.fileupload.file.dto.FileUploadResponse;
import com.fileupload.file.storage.R2StorageService;
import com.fileupload.file.validation.FileUploadValidator;
import com.fileupload.file.validation.ValidatedUpload;

@Service
public class FileUploadService {

    private static final Logger log =
        LoggerFactory.getLogger(
            FileUploadService.class
        );

    private final FileUploadValidator validator;
    private final StoredFileCommandService commandService;
    private final R2StorageService storageService;

    public FileUploadService(
        FileUploadValidator validator,
        StoredFileCommandService commandService,
        R2StorageService storageService
    ) {
        this.validator = validator;
        this.commandService = commandService;
        this.storageService = storageService;
    }

    public FileUploadResponse upload(
        MultipartFile multipartFile,
        Long userId
    ) {
        ValidatedUpload validated =
            validator.validate(multipartFile);

        String sha256 =
            calculateSha256(multipartFile);

        String storageKey =
            createStorageKey();

        StoredFile pending =
            commandService.createPending(
                validated.originalFilename(),
                storageKey,
                validated.extension(),
                validated.clientMimeType(),
                validated.detectedMimeType(),
                validated.sizeBytes(),
                sha256,
                userId
            );

        try {
            storageService.upload(
                storageKey,
                multipartFile,
                validated.detectedMimeType()
            );

        } catch (Exception exception) {

            log.error(
                "R2 upload failed. fileId={}, storageKey={}",
                pending.getId(),
                storageKey,
                exception
            );

            try {
                commandService.markFailed(
                    pending.getId(),
                    "R2_UPLOAD_FAILED"
                );
            } catch (Exception dbException) {

                log.error(
                    "Failed to mark upload as FAILED. "
                        + "fileId={}, storageKey={}",
                    pending.getId(),
                    storageKey,
                    dbException
                );
            }

            throw new FileUploadException(
                HttpStatus.BAD_GATEWAY,
                "FILE_STORAGE_FAILED",
                "파일 저장 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            );
        }

        try {
            commandService.markCompleted(
                pending.getId()
            );

        } catch (Exception exception) {

            /*
             * 여기까지 왔다면 R2 object는 이미 존재한다.
             *
             * DB 완료 처리만 실패한 orphan 후보이므로
             * fileId/storageKey를 로그에 남긴다.
             */
            log.error(
                "R2 upload succeeded but DB completion failed. "
                    + "orphanCandidate=true, "
                    + "fileId={}, storageKey={}",
                pending.getId(),
                storageKey,
                exception
            );

            throw new FileUploadException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "UPLOAD_FINALIZATION_FAILED",
                "파일 업로드 처리 중 오류가 발생했습니다."
            );
        }

        log.info(
            "File upload completed. "
                + "fileId={}, extension={}, sizeBytes={}",
            pending.getId(),
            validated.extension(),
            validated.sizeBytes()
        );

        return new FileUploadResponse(
            pending.getId(),
            validated.originalFilename(),
            validated.extension(),
            validated.detectedMimeType(),
            validated.sizeBytes(),
            sha256,
            "COMPLETED"
        );
    }

    private String calculateSha256(
        MultipartFile file
    ) {
        try {
            MessageDigest digest =
                MessageDigest.getInstance(
                    "SHA-256"
                );

            try (
                InputStream inputStream =
                    file.getInputStream()
            ) {
                byte[] buffer =
                    new byte[8192];

                int read;

                while (
                    (read =
                        inputStream.read(buffer))
                        != -1
                ) {
                    digest.update(
                        buffer,
                        0,
                        read
                    );
                }
            }

            return HexFormat.of()
                .formatHex(digest.digest());

        } catch (
            IOException
            | NoSuchAlgorithmException exception
        ) {
            throw new FileUploadException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "HASH_CALCULATION_FAILED",
                "파일을 처리하는 중 오류가 발생했습니다."
            );
        }
    }

    private String createStorageKey() {

        LocalDate today =
            LocalDate.now(ZoneOffset.UTC);

        return "uploads/"
            + today.getYear()
            + "/"
            + String.format(
                "%02d",
                today.getMonthValue()
            )
            + "/"
            + UUID.randomUUID();
    }
}