package apps.sarafrika.elimika.instructor.dto;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InstructorDocumentDTOTest {

    @Test
    void pendingStatusReportsPendingVerification() {
        InstructorDocumentDTO document = document(DocumentStatus.PENDING, false);

        assertThat(document.getVerificationStatus()).isEqualTo("PENDING");
        assertThat(document.isPendingVerification()).isTrue();
    }

    @Test
    void approvedStatusReportsVerified() {
        InstructorDocumentDTO document = document(DocumentStatus.APPROVED, true);

        assertThat(document.getVerificationStatus()).isEqualTo("VERIFIED");
        assertThat(document.isPendingVerification()).isFalse();
    }

    @Test
    void rejectedStatusReportsRejected() {
        InstructorDocumentDTO document = document(DocumentStatus.REJECTED, false);

        assertThat(document.getVerificationStatus()).isEqualTo("REJECTED");
        assertThat(document.isPendingVerification()).isFalse();
    }

    @Test
    void expiredStatusReportsExpired() {
        InstructorDocumentDTO document = document(DocumentStatus.EXPIRED, false);

        assertThat(document.getVerificationStatus()).isEqualTo("EXPIRED");
        assertThat(document.isPendingVerification()).isFalse();
    }

    @Test
    void nullStatusDoesNotTreatUnverifiedAsRejected() {
        InstructorDocumentDTO document = document(null, false);

        assertThat(document.getVerificationStatus()).isEqualTo("PENDING");
        assertThat(document.isPendingVerification()).isTrue();
    }

    private InstructorDocumentDTO document(DocumentStatus status, Boolean isVerified) {
        return new InstructorDocumentDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                "certificate.pdf",
                "profile_documents/instructors/instructor/certificate.pdf",
                "profile_documents/instructors/instructor/certificate.pdf",
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
