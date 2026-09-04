package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorDocumentDTO;
import apps.sarafrika.elimika.instructor.model.InstructorDocument;
import apps.sarafrika.elimika.instructor.repository.InstructorDocumentRepository;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorDocumentServiceImplTest {

    @Mock
    private InstructorDocumentRepository instructorDocumentRepository;

    @Mock
    private GenericSpecificationBuilder<InstructorDocument> specificationBuilder;

    @Mock
    private InstructorLookupService instructorLookupService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MediaStorageService mediaStorageService;

    @Mock
    private DomainSecurityService domainSecurityService;

    private InstructorDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InstructorDocumentServiceImpl(
                instructorDocumentRepository,
                specificationBuilder,
                instructorLookupService,
                eventPublisher,
                mediaStorageService,
                domainSecurityService
        );
    }

    @Test
    void getDocumentsByInstructorUuidReturnsEveryMatchingDocument() {
        UUID instructorUuid = UUID.randomUUID();
        InstructorDocument certificate = document(instructorUuid, "certificate.pdf");
        InstructorDocument identity = document(instructorUuid, "identity.pdf");

        when(instructorDocumentRepository.findByInstructorUuid(instructorUuid))
                .thenReturn(List.of(certificate, identity));

        List<InstructorDocumentDTO> result = service.getDocumentsByInstructorUuid(instructorUuid);

        assertThat(result)
                .hasSize(2)
                .extracting(InstructorDocumentDTO::originalFilename)
                .containsExactly("certificate.pdf", "identity.pdf");
    }

    private InstructorDocument document(UUID instructorUuid, String originalFilename) {
        InstructorDocument document = new InstructorDocument();
        document.setUuid(UUID.randomUUID());
        document.setInstructorUuid(instructorUuid);
        document.setDocumentTypeUuid(UUID.randomUUID());
        document.setOriginalFilename(originalFilename);
        document.setStoredFilename("profile_documents/instructors/" + instructorUuid + "/" + originalFilename);
        document.setFilePath(document.getStoredFilename());
        document.setFileSizeBytes(1024L);
        document.setMimeType("application/pdf");
        document.setTitle(originalFilename);
        document.setIsVerified(false);
        document.setStatus(DocumentStatus.PENDING);
        return document;
    }
}
