package apps.sarafrika.elimika.coursecreator.dto;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseCreatorDocumentDTOTest {

    @Test
    void pendingStatusReportsPendingVerification() {
        CourseCreatorDocumentDTO document = document(DocumentStatus.PENDING, false);

        assertThat(document.getVerificationStatus()).isEqualTo("PENDING");
        assertThat(document.isPendingVerification()).isTrue();
    }

    @Test
    void approvedStatusReportsVerified() {
        CourseCreatorDocumentDTO document = document(DocumentStatus.APPROVED, true);

        assertThat(document.getVerificationStatus()).isEqualTo("VERIFIED");
        assertThat(document.isPendingVerification()).isFalse();
    }

    @Test
    void rejectedStatusReportsRejected() {
        CourseCreatorDocumentDTO document = document(DocumentStatus.REJECTED, false);

        assertThat(document.getVerificationStatus()).isEqualTo("REJECTED");
        assertThat(document.isPendingVerification()).isFalse();
    }

    @Test
    void expiredStatusReportsExpired() {
        CourseCreatorDocumentDTO document = document(DocumentStatus.EXPIRED, false);

        assertThat(document.getVerificationStatus()).isEqualTo("EXPIRED");
        assertThat(document.isPendingVerification()).isFalse();
    }

    @Test
    void nullStatusDoesNotTreatUnverifiedAsRejected() {
        CourseCreatorDocumentDTO document = document(null, false);

        assertThat(document.getVerificationStatus()).isEqualTo("PENDING");
        assertThat(document.isPendingVerification()).isTrue();
    }

    private CourseCreatorDocumentDTO document(DocumentStatus status, Boolean isVerified) {
        return new CourseCreatorDocumentDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                "certificate.pdf",
                "profile_documents/course-creators/creator/certificate.pdf",
                "profile_documents/course-creators/creator/certificate.pdf",
                1024L,
                "application/pdf",
                null,
                "Certificate",
                null,
                null,
                isVerified,
                null,
                null,
                null,
                status,
                null,
                null,
                null,
                null,
                null
        );
    }
}
