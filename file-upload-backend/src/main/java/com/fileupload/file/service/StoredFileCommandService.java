package com.fileupload.file.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fileupload.file.domain.StoredFile;
import com.fileupload.file.repository.StoredFileRepository;

import jakarta.persistence.EntityManager;

@Service
public class StoredFileCommandService {

    private final StoredFileRepository repository;
    private final EntityManager entityManager;

    public StoredFileCommandService(
        StoredFileRepository repository,
        EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public StoredFile createPending(
        String originalFilename,
        String storageKey,
        String extension,
        String clientMimeType,
        String detectedMimeType,
        long sizeBytes,
        String sha256,
        Long uploadedBy
    ) {
        StoredFile storedFile =
            StoredFile.createPending(
                originalFilename,
                storageKey,
                extension,
                clientMimeType,
                detectedMimeType,
                sizeBytes,
                sha256,
                uploadedBy
            );

        StoredFile saved =
            repository.saveAndFlush(storedFile);

        entityManager.refresh(saved);

        return saved;
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markCompleted(
        UUID fileId
    ) {
        StoredFile file =
            repository.findById(fileId)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Stored file not found."
                    )
                );

        file.markCompleted();
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markFailed(
        UUID fileId,
        String failureCode
    ) {
        StoredFile file =
            repository.findById(fileId)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Stored file not found."
                    )
                );

        file.markFailed(failureCode);
    }
}