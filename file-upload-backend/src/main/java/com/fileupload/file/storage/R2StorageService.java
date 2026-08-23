package com.fileupload.file.storage;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class R2StorageService {

    private final S3Client s3Client;
    private final R2Properties properties;

    public R2StorageService(
        S3Client s3Client,
        R2Properties properties
    ) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public void upload(
        String storageKey,
        MultipartFile file,
        String detectedMimeType
    ) {

        PutObjectRequest request =
            PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .contentType(detectedMimeType)
                .contentDisposition("attachment")
                .build();

        ContentStreamProvider streamProvider =
            ContentStreamProvider.fromInputStreamSupplier(
                () -> {
                    try {
                        return file.getInputStream();
                    } catch (IOException exception) {
                        throw new UncheckedIOException(
                            exception
                        );
                    }
                }
            );

        s3Client.putObject(
            request,
            RequestBody.fromContentProvider(
                streamProvider,
                file.getSize(),
                detectedMimeType
            )
        );
    }
}