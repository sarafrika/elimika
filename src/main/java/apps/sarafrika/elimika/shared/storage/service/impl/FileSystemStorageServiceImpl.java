package apps.sarafrika.elimika.shared.storage.service.impl;

import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.config.exception.StorageException;
import apps.sarafrika.elimika.shared.storage.config.exception.StorageFileNotFoundException;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileSystemStorageServiceImpl implements StorageService {

    private final Path rootLocation;

    @Autowired
    public FileSystemStorageServiceImpl(StorageProperties properties) {
        if (properties.getLocation().trim().isEmpty()) {
            throw new StorageException("Storage location is not configured.");
        }

        this.rootLocation = Path.of(properties.getLocation());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Failed to initialize storage.", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store file. File is empty.");
            }

            String originalFilename = file.getOriginalFilename();

            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";

            String uniqueFileName = UUID.randomUUID() + extension;

            Path targetLocation = rootLocation.resolve(uniqueFileName).normalize().toAbsolutePath();

            if (!targetLocation.getParent().equals(rootLocation.toAbsolutePath())) {
                throw new StorageException("File is not stored in the configured location.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

                return uniqueFileName;
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file.");
            }

            // Create folder if it doesn't exist
            Path folderPath = this.rootLocation.resolve(folder);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String uniqueFilename = folder + "_" + UUID.randomUUID().toString() + extension;

            // Store file in the specified folder
            Path destinationFile = folderPath.resolve(uniqueFilename);

            // Security check - ensure file is within the target directory
            if (!destinationFile.getParent().equals(folderPath.toAbsolutePath())) {
                throw new StorageException("Cannot store file outside target directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return the relative path from root (folder/filename)
            return folder + "/" + uniqueFilename;

        } catch (IOException e) {
            throw new StorageException("Failed to store file in folder: " + folder, e);
        }
    }

    @Override
    public Resource load(String fileName) {
        try {
            Resource resource = getResource(fileName);

            if (resource.exists() && resource.isReadable()) {
                return resource;
            }

            throw new StorageFileNotFoundException("Could not read file: " + fileName);
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + fileName, e);
        }
    }

    /**
     * Determines the MIME content type for a given file.
     * Uses multiple detection methods for comprehensive file type support.
     *
     * @param fileName The name of the file
     * @return The MIME content type string
     */
    @Override
    public String getContentType(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "application/octet-stream";
        }

        String contentType = "application/octet-stream";

        try {
            // Handle files in subfolders (e.g., "course_thumbnails/file.jpg")
            Path filePath = rootLocation.resolve(fileName).normalize().toAbsolutePath();

            if (Files.exists(filePath)) {
                String detectedType = Files.probeContentType(filePath);
                if (detectedType != null && !detectedType.isEmpty()) {
                    contentType = detectedType;
                } else {
                    contentType = getContentTypeByExtension(fileName);
                }
            } else {
                contentType = getContentTypeByExtension(fileName);
            }
        } catch (Exception e) {
            contentType = getContentTypeByExtension(fileName);
        }

        return contentType;
    }

    /**
     * Determines content type based on file extension.
     * Supports a wide range of file types including images, documents, audio, video, etc.
     */
    private String getContentTypeByExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            // Images
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "bmp" -> "image/bmp";
            case "tiff", "tif" -> "image/tiff";
            case "ico" -> "image/x-icon";
            case "heic" -> "image/heic";
            case "heif" -> "image/heif";

            // Documents
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";
            case "rtf" -> "application/rtf";
            case "odt" -> "application/vnd.oasis.opendocument.text";
            case "ods" -> "application/vnd.oasis.opendocument.spreadsheet";
            case "odp" -> "application/vnd.oasis.opendocument.presentation";

            // Web files
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "xml" -> "application/xml";

            // Audio
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "flac" -> "audio/flac";
            case "aac" -> "audio/aac";
            case "m4a" -> "audio/mp4";

            // Video
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "wmv" -> "video/x-ms-wmv";
            case "flv" -> "video/x-flv";
            case "webm" -> "video/webm";
            case "mkv" -> "video/x-matroska";

            // Archives
            case "zip" -> "application/zip";
            case "rar" -> "application/vnd.rar";
            case "tar" -> "application/x-tar";
            case "gz" -> "application/gzip";
            case "7z" -> "application/x-7z-compressed";

            // Other common types
            case "csv" -> "text/csv";
            case "md" -> "text/markdown";
            case "log" -> "text/plain";

            default -> "application/octet-stream";
        };
    }

    /**
     * Gets the file extension from a filename.
     *
     * @param fileName The filename
     * @return The file extension (without the dot) or empty string if no extension
     */
    @Override
    public String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Checks if a file is an image based on its extension.
     *
     * @param fileName The filename to check
     * @return true if the file is an image, false otherwise
     */
    @Override
    public boolean isImage(String fileName) {
        String extension = getFileExtension(fileName);
        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "tiff", "tif", "ico", "heic", "heif" -> true;
            default -> false;
        };
    }

    /**
     * Checks if a file is a document based on its extension.
     *
     * @param fileName The filename to check
     * @return true if the file is a document, false otherwise
     */
    @Override
    public boolean isDocument(String fileName) {
        String extension = getFileExtension(fileName);
        return switch (extension) {
            case "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp" -> true;
            default -> false;
        };
    }

    /**
     * Checks if a file is a video based on its extension.
     *
     * @param fileName The filename to check
     * @return true if the file is a video, false otherwise
     */
    @Override
    public boolean isVideo(String fileName) {
        String extension = getFileExtension(fileName);
        return switch (extension) {
            case "mp4", "avi", "mov", "wmv", "flv", "webm", "mkv", "m4v", "3gp", "ogv" -> true;
            default -> false;
        };
    }

    /**
     * Checks if a file is an audio file based on its extension.
     *
     * @param fileName The filename to check
     * @return true if the file is an audio file, false otherwise
     */
    @Override
    public boolean isAudio(String fileName) {
        String extension = getFileExtension(fileName);
        return switch (extension) {
            case "mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "opus" -> true;
            default -> false;
        };
    }

    private Resource getResource(String fileName) throws MalformedURLException {
        // Allow files in subfolders but still prevent directory traversal
        if (fileName.contains("..")) {
            throw new StorageFileNotFoundException("Invalid file name: " + fileName);
        }

        Path targetLocation = rootLocation.resolve(fileName).normalize().toAbsolutePath();

        if (!targetLocation.startsWith(rootLocation.toAbsolutePath())) {
            throw new StorageFileNotFoundException("File access denied: " + fileName);
        }

        return new UrlResource(targetLocation.toUri());
    }
}