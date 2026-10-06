package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorDocumentDTO;
import apps.sarafrika.elimika.instructor.internal.InstructorProfileBridge;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSectionService;
import apps.sarafrika.elimika.profile.spi.UserDocumentDTO;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorDocumentServiceImplTest {

    @Mock
    private ProfessionalProfileService profileService;

    @Mock
    private ProfileSectionService<UserDocumentDTO> documents;

    @Mock
    private InstructorProfileBridge bridge;

    private InstructorDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InstructorDocumentServiceImpl(profileService, bridge);
    }

    @Test
    void getDocumentsByInstructorUuidReadsTheOwnersSharedDocuments() {
        UUID instructorUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        when(bridge.findUserUuid(instructorUuid)).thenReturn(Optional.of(userUuid));
        when(profileService.documents()).thenReturn(documents);
        when(documents.list(userUuid)).thenReturn(List.of(document(userUuid, "certificate.pdf"),
                document(userUuid, "identity.pdf")));

        List<InstructorDocumentDTO> result = service.getDocumentsByInstructorUuid(instructorUuid);

        assertThat(result)
                .hasSize(2)
                .allSatisfy(document -> assertThat(document.instructorUuid()).isEqualTo(instructorUuid))
                .extracting(InstructorDocumentDTO::originalFilename)
                .containsExactly("certificate.pdf", "identity.pdf");
    }

    @Test
    void anUnknownInstructorHasNoDocuments() {
        UUID instructorUuid = UUID.randomUUID();
        when(bridge.findUserUuid(instructorUuid)).thenReturn(Optional.empty());

        assertThat(service.getDocumentsByInstructorUuid(instructorUuid)).isEmpty();
    }

    private static UserDocumentDTO document(UUID userUuid, String originalFilename) {
        String path = "profile_documents/users/" + userUuid + "/" + originalFilename;
        return new UserDocumentDTO(UUID.randomUUID(), userUuid, UUID.randomUUID(), null, null, null, originalFilename,
                path, path, 1024L, "application/pdf", null, originalFilename, null, null, false, null, null, null,
                DocumentStatus.PENDING, null, null, null, null, null);
    }
}
