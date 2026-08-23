package com.fileupload.file.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fileupload.file.domain.StoredFile;

public interface StoredFileRepository
    extends JpaRepository<StoredFile, UUID> {
}