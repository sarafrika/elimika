package apps.sarafrika.elimika.shared.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * The location for storing files.
     */
    private String location;

    /**
     * Base URL for accessing stored files
     * Used for generating file URLs
     */
    private String baseUrl;

    /**
     * Folder structure configuration
     */
    private Folders folders = new Folders();

    @Setter
    @Getter
    public static class Folders {
        /**
         * Folder for user profile images
         */
        private String profileImages = "profile_images";

        /**
         * Folder for course thumbnails
         */
        private String courseThumbnails = "course_thumbnails";

        /**
         * Folder for course materials (documents, videos, etc.)
         */
        private String courseMaterials = "course_materials";

        /**
         * Folder for organization logos
         */
        private String organizationLogos = "organization_logos";

        /**
         * Folder for certificates
         */
        private String certificates = "certificates";

        /**
         * Folder for profile and verification documents (e.g., instructor credentials)
         */
        private String profileDocuments = "profile_documents";

        /**
         * Folder for assignments
         */
        private String assignments = "assignments";

        /**
         * Folder for temporary uploads
         */
        private String temp = "temp";
    }
}
