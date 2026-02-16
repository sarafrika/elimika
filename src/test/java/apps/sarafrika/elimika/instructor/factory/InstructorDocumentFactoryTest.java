package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorDocumentDTO;
import apps.sarafrika.elimika.instructor.model.InstructorDocument;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InstructorDocumentFactoryTest {

    @Test
    void toEntityMapsUploadMetadataFields() {
        UUID documentUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID documentTypeUuid = UUID.randomUUID();
        UUID educationUuid = UUID.randomUUID();
        UUID experienceUuid = UUID.randomUUID();
        UUID membershipUuid = UUID.randomUUID();
        LocalDateTime uploadDate = LocalDateTime.of(2026, 2, 16, 17, 31, 46);
        LocalDateTime verifiedAt = LocalDateTime.of(2026, 2, 17, 10, 0, 0);
        LocalDate expiryDate = LocalDate.of(2028, 12, 31);

        InstructorDocumentDTO dto = new InstructorDocumentDTO(
                documentUuid,
                instructorUuid,
                documentTypeUuid,
                educationUuid,
                experienceUuid,
                membershipUuid,
                "cashflow-statement.pdf",
                "profile_documents/instructors/abc123.pdf",
                "profile_documents/instructors/abc123.pdf",
                2048L,
                "application/pdf",
                "hash123",
                "Title",
                "Description",
                uploadDate,
                false,
                "admin@example.com",
                verifiedAt,
                "ok",
                DocumentStatus.PENDING,
                expiryDate,
                null,
                null,
                null,
                null
        );

        InstructorDocument entity = InstructorDocumentFactory.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getUuid()).isEqualTo(documentUuid);
        assertThat(entity.getInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(entity.getDocumentTypeUuid()).isEqualTo(documentTypeUuid);
        assertThat(entity.getEducationUuid()).isEqualTo(educationUuid);
        assertThat(entity.getExperienceUuid()).isEqualTo(experienceUuid);
        assertThat(entity.getMembershipUuid()).isEqualTo(membershipUuid);
        assertThat(entity.getOriginalFilename()).isEqualTo("cashflow-statement.pdf");
        assertThat(entity.getStoredFilename()).isEqualTo("profile_documents/instructors/abc123.pdf");
        assertThat(entity.getFilePath()).isEqualTo("profile_documents/instructors/abc123.pdf");
        assertThat(entity.getFileSizeBytes()).isEqualTo(2048L);
        assertThat(entity.getMimeType()).isEqualTo("application/pdf");
        assertThat(entity.getFileHash()).isEqualTo("hash123");
        assertThat(entity.getTitle()).isEqualTo("Title");
        assertThat(entity.getDescription()).isEqualTo("Description");
        assertThat(entity.getUploadDate()).isEqualTo(uploadDate);
        assertThat(entity.getIsVerified()).isFalse();
        assertThat(entity.getVerifiedBy()).isEqualTo("admin@example.com");
        assertThat(entity.getVerifiedAt()).isEqualTo(verifiedAt);
        assertThat(entity.getVerificationNotes()).isEqualTo("ok");
        assertThat(entity.getStatus()).isEqualTo(DocumentStatus.PENDING);
        assertThat(entity.getExpiryDate()).isEqualTo(expiryDate);
    }

    @Test
    void toEntityReturnsNullWhenDtoIsNull() {
        assertThat(InstructorDocumentFactory.toEntity(null)).isNull();
    }
}
