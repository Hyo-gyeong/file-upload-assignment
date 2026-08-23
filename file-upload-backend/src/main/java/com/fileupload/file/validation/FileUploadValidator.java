package com.fileupload.file.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fileupload.common.exception.FileUploadException;
import com.fileupload.policy.repository.FileExtensionPolicyRepository;

@Component
public class FileUploadValidator {

    private static final long MAX_FILE_SIZE =
        100L * 1024 * 1024;

    private static final Pattern FILENAME_PATTERN =
        Pattern.compile("^[가-힣A-Za-z0-9 _.-]+$");

    private static final Pattern EXTENSION_PATTERN =
        Pattern.compile("^[a-z0-9]{1,20}$");

    private static final Set<String> COMPOUND_TAR_SUFFIXES =
        Set.of(
            ".tar.gz",
            ".tar.bz2",
            ".tar.xz",
            ".tar.zst"
        );

    private static final Set<String> PE_EXTENSIONS =
        Set.of("exe", "com", "scr");

    private final FileExtensionPolicyRepository policyRepository;

    private final Tika tika = new Tika();

    public FileUploadValidator(
        FileExtensionPolicyRepository policyRepository
    ) {
        this.policyRepository = policyRepository;
    }

    public ValidatedUpload validate(
        MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException(
                HttpStatus.BAD_REQUEST,
                "EMPTY_FILE",
                "비어 있는 파일은 업로드할 수 없습니다."
            );
        }

        String filename =
            validateAndNormalizeFilename(
                file.getOriginalFilename()
            );

        String extension =
            extractExtension(filename);

        /*
         * 정책 검사는 반드시 서버에서 한다.
         * 프론트엔드의 체크 상태는 신뢰하지 않는다.
         */
        policyRepository
            .findByExtension(extension)
            .filter(policy -> policy.isBlocked())
            .ifPresent(policy -> {
                throw new FileUploadException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOCKED_EXTENSION",
                    "." + extension
                        + " 확장자는 현재 업로드가 차단되어 있습니다."
                );
            });

        long size = file.getSize();

        /*
         * 정확히 100 MiB도 허용하지 않는다.
         */
        if (size >= MAX_FILE_SIZE) {
            throw new FileUploadException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                "파일 크기는 100 MiB 미만이어야 합니다."
            );
        }

        String clientMimeType =
            normalizeClientMime(file.getContentType());

        String detectedMimeType =
            detectMime(file);

        validateContent(
            file,
            extension,
            detectedMimeType
        );

        return new ValidatedUpload(
            filename,
            extension,
            clientMimeType,
            detectedMimeType,
            size
        );
    }

    private String validateAndNormalizeFilename(
        String originalFilename
    ) {
        if (originalFilename == null
            || originalFilename.isBlank()) {

            throw invalidFilename();
        }

        String filename =
            Normalizer.normalize(
                originalFilename,
                Normalizer.Form.NFC
            );

        if (filename.length() > 255) {
            throw invalidFilename();
        }

        if (filename.startsWith(".")
            || filename.startsWith("-")) {

            throw invalidFilename();
        }

        if (Character.isWhitespace(
            filename.charAt(0)
        )) {
            throw invalidFilename();
        }

        if (filename.contains("/")
            || filename.contains("\\")
            || filename.contains("..")) {

            throw invalidFilename();
        }

        boolean hasControlCharacter =
            filename.codePoints()
                .anyMatch(Character::isISOControl);

        if (hasControlCharacter) {
            throw invalidFilename();
        }

        if (!FILENAME_PATTERN
            .matcher(filename)
            .matches()) {

            throw invalidFilename();
        }

        return filename;
    }

    private String extractExtension(
        String filename
    ) {
        String lowercase =
            filename.toLowerCase(Locale.ROOT);

        boolean compoundTar =
            COMPOUND_TAR_SUFFIXES
                .stream()
                .anyMatch(lowercase::endsWith);

        long dotCount =
            lowercase.chars()
                .filter(character -> character == '.')
                .count();

        /*
         * 일반 파일은 점 하나만 허용.
         *
         * archive.tar.gz 같은 명시적 tar 복합 확장자만
         * 점 두 개를 허용한다.
         */
        if ((!compoundTar && dotCount != 1)
            || (compoundTar && dotCount != 2)) {

            throw new FileUploadException(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILENAME",
                "허용되지 않는 파일명 형식입니다."
            );
        }

        int lastDot =
            lowercase.lastIndexOf('.');

        if (lastDot <= 0
            || lastDot == lowercase.length() - 1) {

            throw invalidFilename();
        }

        String extension =
            lowercase.substring(lastDot + 1);

        if (!EXTENSION_PATTERN
            .matcher(extension)
            .matches()) {

            throw new FileUploadException(
                HttpStatus.BAD_REQUEST,
                "INVALID_EXTENSION",
                "유효하지 않은 파일 확장자입니다."
            );
        }

        return extension;
    }

    private String detectMime(
        MultipartFile file
    ) {
        try (
            InputStream inputStream =
                file.getInputStream()
        ) {
            /*
             * filename을 넘기지 않는다.
             *
             * report.jpg라는 이름 때문에 jpg라고 판단하는 것이 아니라
             * 실제 내용 기반 탐지를 우선한다.
             */
            return tika.detect(inputStream);

        } catch (IOException exception) {
            throw new FileUploadException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "FILE_READ_FAILED",
                "파일을 검사하는 중 오류가 발생했습니다."
            );
        }
    }

    private void validateContent(
        MultipartFile file,
        String extension,
        String detectedMime
    ) {
        byte[] prefix = readPrefix(file, 16);

        /*
         * Windows PE 실행파일의 MZ signature.
         *
         * exe 자체가 정책상 허용된 경우까지 강제로 막지는 않는다.
         * 대신 exe 내용을 jpg/txt 등으로 위장한 경우를 막는다.
         */
        if (isPeExecutable(prefix)
            && !PE_EXTENSIONS.contains(extension)) {

            throw contentMismatch();
        }

        switch (extension) {

            case "pdf" -> {
                if (!startsWith(
                    prefix,
                    "%PDF-".getBytes(
                        StandardCharsets.US_ASCII
                    )
                )) {
                    throw contentMismatch();
                }
            }

            case "png" -> {
                byte[] png = {
                    (byte) 0x89,
                    0x50,
                    0x4E,
                    0x47,
                    0x0D,
                    0x0A,
                    0x1A,
                    0x0A
                };

                if (!startsWith(prefix, png)) {
                    throw contentMismatch();
                }
            }

            case "jpg", "jpeg" -> {
                if (prefix.length < 3
                    || (prefix[0] & 0xFF) != 0xFF
                    || (prefix[1] & 0xFF) != 0xD8
                    || (prefix[2] & 0xFF) != 0xFF) {

                    throw contentMismatch();
                }
            }

            case "gif" -> {
                boolean gif87 =
                    startsWith(
                        prefix,
                        "GIF87a".getBytes(
                            StandardCharsets.US_ASCII
                        )
                    );

                boolean gif89 =
                    startsWith(
                        prefix,
                        "GIF89a".getBytes(
                            StandardCharsets.US_ASCII
                        )
                    );

                if (!gif87 && !gif89) {
                    throw contentMismatch();
                }
            }

            case "zip" -> {
                if (!isZip(prefix)) {
                    throw contentMismatch();
                }
            }

            case "gz" -> {
                if (prefix.length < 2
                    || (prefix[0] & 0xFF) != 0x1F
                    || (prefix[1] & 0xFF) != 0x8B) {

                    throw contentMismatch();
                }
            }

            case "txt", "csv",
                 "js", "bat", "cmd" -> {

                if (!isTextLikeMime(detectedMime)) {
                    throw contentMismatch();
                }
            }

            default -> {
                /*
                 * 모든 확장자에 가짜 magic rule을 만들지 않는다.
                 * 확실히 검증 가능한 타입만 강제한다.
                 */
            }
        }
    }

    private byte[] readPrefix(
        MultipartFile file,
        int maxLength
    ) {
        try (
            InputStream inputStream =
                file.getInputStream()
        ) {
            return inputStream
                .readNBytes(maxLength);

        } catch (IOException exception) {
            throw new FileUploadException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "FILE_READ_FAILED",
                "파일을 검사하는 중 오류가 발생했습니다."
            );
        }
    }

    private boolean isPeExecutable(
        byte[] prefix
    ) {
        return prefix.length >= 2
            && prefix[0] == 'M'
            && prefix[1] == 'Z';
    }

    private boolean isZip(
        byte[] prefix
    ) {
        if (prefix.length < 4) {
            return false;
        }

        return prefix[0] == 'P'
            && prefix[1] == 'K'
            && (
                (
                    prefix[2] == 0x03
                    && prefix[3] == 0x04
                )
                ||
                (
                    prefix[2] == 0x05
                    && prefix[3] == 0x06
                )
                ||
                (
                    prefix[2] == 0x07
                    && prefix[3] == 0x08
                )
            );
    }

    private boolean startsWith(
        byte[] actual,
        byte[] expected
    ) {
        if (actual.length < expected.length) {
            return false;
        }

        for (int index = 0;
             index < expected.length;
             index++) {

            if (actual[index] != expected[index]) {
                return false;
            }
        }

        return true;
    }

    private boolean isTextLikeMime(
        String mime
    ) {
        return mime.startsWith("text/")
            || mime.equals("application/json")
            || mime.equals(
                "application/javascript"
            )
            || mime.equals(
                "application/xml"
            );
    }

    private String normalizeClientMime(
        String clientMime
    ) {
        if (clientMime == null
            || clientMime.isBlank()) {

            return null;
        }

        return clientMime.length() <= 150
            ? clientMime
            : clientMime.substring(0, 150);
    }

    private FileUploadException invalidFilename() {
        return new FileUploadException(
            HttpStatus.BAD_REQUEST,
            "INVALID_FILENAME",
            "허용되지 않는 파일명입니다."
        );
    }

    private FileUploadException contentMismatch() {
        return new FileUploadException(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "FILE_CONTENT_MISMATCH",
            "파일 확장자와 실제 파일 형식이 일치하지 않습니다."
        );
    }
}