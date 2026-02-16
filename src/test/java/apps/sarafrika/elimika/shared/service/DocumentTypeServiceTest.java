package apps.sarafrika.elimika.shared.service;

import apps.sarafrika.elimika.shared.dto.DocumentTypeOptionDTO;
import apps.sarafrika.elimika.shared.model.DocumentType;
import apps.sarafrika.elimika.shared.repository.DocumentTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTypeServiceTest {

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    private DocumentTypeService documentTypeService;

    @BeforeEach
    void setUp() {
        documentTypeService = new DocumentTypeService(documentTypeRepository, new ObjectMapper());
    }

    @Test
    void listDocumentTypesMapsDocumentTypeOptions() {
        DocumentType certificate = new DocumentType();
        certificate.setUuid(UUID.fromString("f4ee4d9c-cf22-4b34-8a43-1905f37eb9b9"));
        certificate.setName("CERTIFICATE");
        certificate.setDescription("Educational certificates and diplomas");
        certificate.setMaxFileSizeMb(10);
        certificate.setAllowedExtensions("[\"pdf\",\"jpg\",\"png\"]");
        certificate.setIsRequired(false);

        when(documentTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of(certificate));

        List<DocumentTypeOptionDTO> result = documentTypeService.listDocumentTypes();

        assertThat(result).hasSize(1);
        DocumentTypeOptionDTO item = result.getFirst();
        assertThat(item.uuid()).isEqualTo(certificate.getUuid());
        assertThat(item.name()).isEqualTo("CERTIFICATE");
        assertThat(item.description()).isEqualTo("Educational certificates and diplomas");
        assertThat(item.maxFileSizeMb()).isEqualTo(10);
        assertThat(item.allowedExtensions()).containsExactly("pdf", "jpg", "png");
        assertThat(item.isRequired()).isFalse();
    }

    @Test
    void listDocumentTypesReturnsEmptyAllowedExtensionsWhenRawValueIsInvalidJson() {
        DocumentType broken = new DocumentType();
        broken.setUuid(UUID.randomUUID());
        broken.setName("BROKEN");
        broken.setAllowedExtensions("not-json");

        when(documentTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of(broken));

        List<DocumentTypeOptionDTO> result = documentTypeService.listDocumentTypes();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().allowedExtensions()).isEmpty();
    }
}
