package com.fileupload.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
        PolicyNotFoundException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                new ApiErrorResponse(
                    "POLICY_NOT_FOUND",
                    "확장자 정책을 찾을 수 없습니다."
                )
            );
    }

    @ExceptionHandler(PolicyConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
        PolicyConflictException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                new ApiErrorResponse(
                    "POLICY_CONFLICT",
                    "다른 관리자가 먼저 이 정책을 변경했습니다. "
                        + "최신 내용을 불러온 후 다시 시도해주세요."
                )
            );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
        MethodArgumentNotValidException exception
    ) {
        return ResponseEntity
            .badRequest()
            .body(
                new ApiErrorResponse(
                    "INVALID_REQUEST",
                    "요청 값을 확인해주세요."
                )
            );
    }
    
    @ExceptionHandler(InvalidExtensionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidExtension(
        InvalidExtensionException exception
    ) {
        return ResponseEntity
            .badRequest()
            .body(
                new ApiErrorResponse(
                    "INVALID_EXTENSION",
                    "확장자는 영문 소문자와 숫자만 사용할 수 있으며 최대 20자입니다."
                )
            );
    }

    @ExceptionHandler(DuplicateExtensionException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateExtension(
        DuplicateExtensionException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                new ApiErrorResponse(
                    "EXTENSION_ALREADY_EXISTS",
                    "이미 등록된 확장자입니다."
                )
            );
    }

    @ExceptionHandler(CustomExtensionLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomExtensionLimit(
        CustomExtensionLimitException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                new ApiErrorResponse(
                    "CUSTOM_EXTENSION_LIMIT_REACHED",
                    "커스텀 확장자는 최대 200개까지 등록할 수 있습니다."
                )
            );
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
        IllegalArgumentException exception
    ) {
        return ResponseEntity
            .badRequest()
            .body(
                new ApiErrorResponse(
                    "INVALID_REQUEST",
                    "요청 값을 확인해주세요."
                )
            );
    }
    
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleFileUpload(
        FileUploadException exception
    ) {
        return ResponseEntity
            .status(exception.getStatus())
            .body(
                new ApiErrorResponse(
                    exception.getCode(),
                    exception.getUserMessage()
                )
            );
    }

    @ExceptionHandler(
        MaxUploadSizeExceededException.class
    )
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
        MaxUploadSizeExceededException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(
                new ApiErrorResponse(
                    "FILE_TOO_LARGE",
                    "파일 크기는 100 MiB 미만이어야 합니다."
                )
            );
    }
}