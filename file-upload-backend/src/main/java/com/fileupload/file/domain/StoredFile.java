package com.fileupload.file.domain;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "files")
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
        name = "original_filename",
        nullable = false,
        length = 255
    )
    private String originalFilename;

    @Column(
        name = "storage_key",
        nullable = false,
        length = 512
    )
    private String storageKey;

    @Column(nullable = false, length = 20)
    private String extension;

    @Column(name = "client_mime_type", length = 150)
    private String clientMimeType;

    @Column(
        name = "detected_mime_type",
        nullable = false,
        length = 150
    )
    private String detectedMimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        nullable = false,
        length = 64,
        columnDefinition = "char(64)"
    )
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileStatus status;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    // metadata는 PostgreSQL JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        nullable = false,
        columnDefinition = "jsonb"
    )
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Column(
        name = "created_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private OffsetDateTime updatedAt;

    protected StoredFile() {
    }

    public StoredFile(
        String originalFilename,
        String storageKey,
        String extension,
        String clientMimeType,
        String detectedMimeType,
        long sizeBytes,
        String sha256,
        long uploadedBy
    ) {
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.extension = extension;
        this.clientMimeType = clientMimeType;
        this.detectedMimeType = detectedMimeType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.uploadedBy = uploadedBy;
        this.status = FileStatus.PENDING;
    }

    public void markCompleted() {
        this.status = FileStatus.COMPLETED;
        this.failureCode = null;
    }

    public void markFailed(String failureCode) {
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException(
                "failureCode must not be blank."
            );
        }

        this.status = FileStatus.FAILED;
        this.failureCode = failureCode;
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getExtension() {
        return extension;
    }

    public String getClientMimeType() {
        return clientMimeType;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public FileStatus getStatus() {
        return status;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public static StoredFile createPending(
	    String originalFilename,
	    String storageKey,
	    String extension,
	    String clientMimeType,
	    String detectedMimeType,
	    long sizeBytes,
	    String sha256,
	    Long uploadedBy
	) {
	    StoredFile file = new StoredFile();

	    file.originalFilename = originalFilename;
	    file.storageKey = storageKey;
	    file.extension = extension;
	    file.clientMimeType = clientMimeType;
	    file.detectedMimeType = detectedMimeType;
	    file.sizeBytes = sizeBytes;
	    file.sha256 = sha256;
	    file.status = FileStatus.PENDING;
	    file.failureCode = null;
	    file.uploadedBy = uploadedBy;
	    file.metadata = new LinkedHashMap<>();

	    return file;
	}
}