package apps.sarafrika.elimika.coursecreator.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for validating course creator document files before upload.
 * Enforces file type and size constraints for course creator credentials and certificates.
 */
@Service
@RequiredArgsConstructor
public class CourseCreatorDocumentValidationService {

    // File size limit in bytes - 50MB for PDF documents
    private static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024;

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    /**
     * Validates a course creator document file.
     * Checks that the file is not empty, is a PDF, and does not exceed the size limit.
     *
     * @param file the multipart file to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateDocument(MultipartFile file) {
        validateFileNotEmpty(file);
        validatePdfType(file);
        validateFileSize(file);
    }

    private void validateFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file cannot be empty");
        }
    }

    private void validatePdfType(MultipartFile file) {
        String contentType = file.getContentType();
        if (!PDF_CONTENT_TYPE.equals(contentType)) {
            throw new IllegalArgumentException("Only PDF files are allowed for course creator documents");
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Document file size cannot exceed %d MB", MAX_DOCUMENT_SIZE / (1024 * 1024))
            );
        }
    }
}
