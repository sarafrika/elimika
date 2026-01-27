package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.shared.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AssignmentMediaValidationService {

    private static final Set<String> SUPPORTED_SUBMISSION_TYPES = Set.of(
            "TEXT",
            "DOCUMENT",
            "IMAGE",
            "AUDIO",
            "VIDEO",
            "URL"
    );

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;    // 10MB
    private static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_AUDIO_SIZE = 100 * 1024 * 1024;   // 100MB
    private static final long MAX_VIDEO_SIZE = 500 * 1024 * 1024;   // 500MB

    private final StorageService storageService;

    public void validateSubmissionTypes(String[] submissionTypes) {
        if (submissionTypes == null || submissionTypes.length == 0) {
            return;
        }

        for (String type : submissionTypes) {
            if (type == null || type.isBlank()) {
                continue;
            }

            if (mapToCanonicalTypes(type).isEmpty()) {
                throw new IllegalArgumentException(
                        "Unsupported submission type: " + type + ". Supported types: "
                                + String.join(", ", SUPPORTED_SUBMISSION_TYPES)
                );
            }
        }
    }

    public String[] normalizeSubmissionTypes(String[] submissionTypes) {
        if (submissionTypes == null) {
            return null;
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String type : submissionTypes) {
            if (type == null || type.isBlank()) {
                continue;
            }
            normalized.addAll(mapToCanonicalTypes(type));
        }

        return normalized.toArray(new String[0]);
    }

    public void validateSubmissionRequest(String[] submissionTypes, String content, String[] fileUrls) {
        Set<String> normalizedTypes = normalizeSubmissionTypeSet(submissionTypes);
        if (normalizedTypes.isEmpty()) {
            return;
        }

        boolean hasText = content != null && !content.isBlank();
        boolean hasFiles = fileUrls != null && Arrays.stream(fileUrls).anyMatch(url -> url != null && !url.isBlank());

        if (!hasText && !hasFiles) {
            throw new IllegalArgumentException("Submission must include text or file attachments.");
        }

        if (hasText && !normalizedTypes.contains("TEXT") && !normalizedTypes.contains("URL")) {
            throw new IllegalArgumentException("Text submissions are not allowed for this assignment.");
        }

        if (hasFiles) {
            for (String fileUrl : fileUrls) {
                if (fileUrl == null || fileUrl.isBlank()) {
                    continue;
                }

                String fileType = resolveFileTypeFromUrl(fileUrl);
                if (!normalizedTypes.contains(fileType)) {
                    throw new IllegalArgumentException(
                            "File type " + fileType + " is not allowed for this assignment."
                    );
                }
            }
        }
    }

    public void validateAssignmentAttachment(MultipartFile file) {
        validateFileNotEmpty(file);
        String fileType = resolveFileType(file);
        validateFileSize(file, fileType);
    }

    public void validateSubmissionAttachment(String[] submissionTypes, MultipartFile file) {
        validateFileNotEmpty(file);
        String fileType = resolveFileType(file);
        validateFileSize(file, fileType);

        Set<String> normalizedTypes = normalizeSubmissionTypeSet(submissionTypes);
        if (!normalizedTypes.isEmpty() && !normalizedTypes.contains(fileType)) {
            throw new IllegalArgumentException("File type " + fileType + " is not allowed for this assignment.");
        }
    }

    private Set<String> normalizeSubmissionTypeSet(String[] submissionTypes) {
        if (submissionTypes == null || submissionTypes.length == 0) {
            return Set.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String type : submissionTypes) {
            if (type == null || type.isBlank()) {
                continue;
            }
            normalized.addAll(mapToCanonicalTypes(type));
        }
        return normalized;
    }

    private Set<String> mapToCanonicalTypes(String rawType) {
        String normalized = rawType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TEXT", "TEXT_ENTRY" -> Set.of("TEXT");
            case "URL", "LINK", "WEB" -> Set.of("URL");
            case "DOCUMENT", "DOC", "DOCX", "PDF", "PPT", "PPTX", "XLS", "XLSX", "ODT", "ODS", "ODP" -> Set.of("DOCUMENT");
            case "IMAGE", "IMG", "PHOTO", "PICTURE", "PNG", "JPG", "JPEG", "GIF", "WEBP", "SVG" -> Set.of("IMAGE");
            case "AUDIO", "MP3", "WAV", "M4A", "FLAC", "AAC", "OGG", "OPUS" -> Set.of("AUDIO");
            case "VIDEO", "MP4", "MOV", "AVI", "WEBM", "MKV", "WMV", "FLV", "M4V" -> Set.of("VIDEO");
            case "FILE", "FILE_UPLOAD" -> Set.of("DOCUMENT", "IMAGE", "AUDIO", "VIDEO");
            default -> Set.of();
        };
    }

    private void validateFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
    }

    private String resolveFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Uploaded file must have a name");
        }
        return resolveFileType(originalFilename);
    }

    private String resolveFileTypeFromUrl(String fileUrl) {
        String sanitized = fileUrl;
        int queryIndex = sanitized.indexOf('?');
        if (queryIndex >= 0) {
            sanitized = sanitized.substring(0, queryIndex);
        }
        int hashIndex = sanitized.indexOf('#');
        if (hashIndex >= 0) {
            sanitized = sanitized.substring(0, hashIndex);
        }

        int lastSlash = sanitized.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? sanitized.substring(lastSlash + 1) : sanitized;
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("Unable to determine file type from URL: " + fileUrl);
        }

        return resolveFileType(fileName);
    }

    private String resolveFileType(String fileName) {
        if (storageService.isImage(fileName)) {
            return "IMAGE";
        }
        if (storageService.isVideo(fileName)) {
            return "VIDEO";
        }
        if (storageService.isAudio(fileName)) {
            return "AUDIO";
        }
        if (storageService.isDocument(fileName)) {
            return "DOCUMENT";
        }

        throw new IllegalArgumentException("Unsupported file type: " + fileName);
    }

    private void validateFileSize(MultipartFile file, String fileType) {
        long maxSize = switch (fileType) {
            case "IMAGE" -> MAX_IMAGE_SIZE;
            case "VIDEO" -> MAX_VIDEO_SIZE;
            case "AUDIO" -> MAX_AUDIO_SIZE;
            case "DOCUMENT" -> MAX_DOCUMENT_SIZE;
            default -> MAX_DOCUMENT_SIZE;
        };

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    String.format("%s file size cannot exceed %d MB", fileType, maxSize / (1024 * 1024))
            );
        }
    }
}
