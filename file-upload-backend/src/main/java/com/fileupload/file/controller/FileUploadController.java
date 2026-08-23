package com.fileupload.file.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fileupload.auth.security.AuthenticatedUser;
import com.fileupload.file.dto.FileUploadResponse;
import com.fileupload.file.service.FileUploadService;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(
        FileUploadService fileUploadService
    ) {
        this.fileUploadService =
            fileUploadService;
    }

    @PostMapping(
        consumes =
            MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FileUploadResponse> upload(
        @RequestPart("file")
        MultipartFile file,

        @AuthenticationPrincipal
        AuthenticatedUser user
    ) {
        FileUploadResponse response =
            fileUploadService.upload(
                file,
                user.getId()
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
}