package apps.sarafrika.elimika.shared.storage.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers every persisted-reference format observed in production data.
 */
class FileUrlResolverTest {

    // ===== publicUrl =====

    @Test
    void publicUrlResolvesBareKey() {
        assertThat(FileUrlResolver.publicUrl("course_thumbnails/abc.jpg"))
                .isEqualTo("/api/v1/files/course_thumbnails/abc.jpg");
    }

    @Test
    void publicUrlEncodesPathSegmentsButKeepsSlashes() {
        assertThat(FileUrlResolver.publicUrl("assignments/a b/file.pdf"))
                .isEqualTo("/api/v1/files/assignments/a%20b/file.pdf");
    }

    @Test
    void publicUrlPassesThroughExternalUrls() {
        assertThat(FileUrlResolver.publicUrl("https://foodal.com/knowledge/how-to/store-fresh-tomatoes/"))
                .isEqualTo("https://foodal.com/knowledge/how-to/store-fresh-tomatoes/");
    }

    @Test
    void publicUrlPassesThroughLegacyApiUrls() {
        assertThat(FileUrlResolver.publicUrl("/api/v1/courses/media/x.jpg"))
                .isEqualTo("/api/v1/courses/media/x.jpg");
    }

    @Test
    void publicUrlReturnsNullForNullAndBlank() {
        assertThat(FileUrlResolver.publicUrl(null)).isNull();
        assertThat(FileUrlResolver.publicUrl("  ")).isNull();
    }

    // ===== toKey =====

    @Test
    void toKeyKeepsBareKey() {
        assertThat(FileUrlResolver.toKey("certificates/abc.pdf")).isEqualTo("certificates/abc.pdf");
    }

    @Test
    void toKeyStripsUnifiedPrefix() {
        assertThat(FileUrlResolver.toKey("/api/v1/files/course_thumbnails/abc.jpg"))
                .isEqualTo("course_thumbnails/abc.jpg");
    }

    @Test
    void toKeyReprefixesProfileImagesFolder() {
        assertThat(FileUrlResolver.toKey("/api/v1/users/profile-image/22ee.jpg"))
                .isEqualTo("profile_images/22ee.jpg");
    }

    @Test
    void toKeyHandlesAbsoluteSelfHostUrls() {
        assertThat(FileUrlResolver.toKey(
                "https://api.elimika.staging.sarafrika.com/api/v1/users/profile-image/22ee.jpg"))
                .isEqualTo("profile_images/22ee.jpg");
        assertThat(FileUrlResolver.toKey(
                "https://api.elimika.staging.sarafrika.com/api/v1/courses/media/91d0.jpeg"))
                .isEqualTo("91d0.jpeg");
    }

    @Test
    void toKeyHandlesSelfHostUrlsWithoutApiPrefix() {
        assertThat(FileUrlResolver.toKey(
                "https://api.elimika.sarafrika.com/assignments/ba/attachments/4ca9.pdf"))
                .isEqualTo("assignments/ba/attachments/4ca9.pdf");
        assertThat(FileUrlResolver.toKey(
                "https://api.elimika.staging.sarafrika.com/course_materials/57b8/lessons/dc7e/file.mp4"))
                .isEqualTo("course_materials/57b8/lessons/dc7e/file.mp4");
    }

    @Test
    void toKeyDeduplicatesDoubleNestedCertificates() {
        assertThat(FileUrlResolver.toKey("/api/v1/certificates/files/certificates/uuid.pdf"))
                .isEqualTo("certificates/uuid.pdf");
        assertThat(FileUrlResolver.toKey("/api/v1/certificates/files/uuid.pdf"))
                .isEqualTo("certificates/uuid.pdf");
    }

    @Test
    void toKeyHandlesDocumentUrls() {
        assertThat(FileUrlResolver.toKey(
                "/api/v1/instructors/9f1b/documents/files/profile_documents/instructors/9f1b/f349.pdf"))
                .isEqualTo("profile_documents/instructors/9f1b/f349.pdf");
        assertThat(FileUrlResolver.toKey(
                "/api/v1/course-creators/221c/documents/files/profile_documents/course-creators/221c/62f8.pdf"))
                .isEqualTo("profile_documents/course-creators/221c/62f8.pdf");
    }

    @Test
    void toKeyDecodesPercentEncoding() {
        assertThat(FileUrlResolver.toKey("/api/v1/classes/media/class_thumbnails%2Fabc.jpeg"))
                .isEqualTo("class_thumbnails/abc.jpeg");
    }

    @Test
    void toKeyReturnsNullForExternalAndJunk() {
        assertThat(FileUrlResolver.toKey("https://foodal.com/some/page")).isNull();
        assertThat(FileUrlResolver.toKey("/assignment.pdf")).isNull();
        assertThat(FileUrlResolver.toKey(null)).isNull();
        assertThat(FileUrlResolver.toKey("")).isNull();
    }

    // ===== toStorableValue =====

    @Test
    void toStorableValueKeepsExternalUrls() {
        assertThat(FileUrlResolver.toStorableValue("https://cdn.example.com/video.mp4"))
                .isEqualTo("https://cdn.example.com/video.mp4");
    }

    @Test
    void toStorableValueReducesResolvedUrlsToKeys() {
        assertThat(FileUrlResolver.toStorableValue("/api/v1/files/course_thumbnails/abc.jpg"))
                .isEqualTo("course_thumbnails/abc.jpg");
        assertThat(FileUrlResolver.toStorableValue("course_thumbnails/abc.jpg"))
                .isEqualTo("course_thumbnails/abc.jpg");
    }

    @Test
    void isExternalDistinguishesSelfHostFromExternal() {
        assertThat(FileUrlResolver.isExternal("https://foodal.com/page")).isTrue();
        assertThat(FileUrlResolver.isExternal("https://api.elimika.sarafrika.com/x")).isFalse();
        assertThat(FileUrlResolver.isExternal("course_thumbnails/x.jpg")).isFalse();
    }

    @Test
    void fileNameReturnsLastSegment() {
        assertThat(FileUrlResolver.fileName("assignments/a/b/work.pdf")).isEqualTo("work.pdf");
        assertThat(FileUrlResolver.fileName("https://foodal.com/page")).isNull();
    }
}
