package apps.sarafrika.elimika.shared.storage.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    void init();

    String store(MultipartFile file);

    Resource load(String fileName);

    /**
     * Determines the MIME content type for a given file.
     *
     * @param fileName The name of the file
     * @return The MIME content type string
     */
    String getContentType(String fileName);

    /**
     * Gets the file extension from a filename.
     *
     * @param fileName The filename
     * @return The file extension (without the dot) or empty string if no extension
     */
    String getFileExtension(String fileName);

    /**
     * Checks if a file is an image based on its extension.
     *
     * @param fileName The filename to check
     * @return true if the file is an image, false otherwise
     */
    boolean isImage(String fileName);

    /**
     * Checks if a file is a document based on its extension.
     *
     * @param fileName The filename to check
     * @return true if the file is a document, false otherwise
     */
    boolean isDocument(String fileName);
}