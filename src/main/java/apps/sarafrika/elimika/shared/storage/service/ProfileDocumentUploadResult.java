package apps.sarafrika.elimika.shared.storage.service;

public record ProfileDocumentUploadResult(
        String originalFilename,
        String storedFilename,
        String filePath,
        Long fileSizeBytes,
        String mimeType,
        String resolvedTitle
) {
}
