package apps.sarafrika.elimika.shared.service;

import apps.sarafrika.elimika.shared.dto.DocumentTypeOptionDTO;
import apps.sarafrika.elimika.shared.model.DocumentType;
import apps.sarafrika.elimika.shared.repository.DocumentTypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTypeService {

    private static final Sort SORT_BY_NAME_ASC = Sort.by(Sort.Direction.ASC, "name");

    private final DocumentTypeRepository documentTypeRepository;
    private final ObjectMapper objectMapper;

    public List<DocumentTypeOptionDTO> listDocumentTypes() {
        return documentTypeRepository.findAll(SORT_BY_NAME_ASC)
                .stream()
                .map(this::toOption)
                .toList();
    }

    private DocumentTypeOptionDTO toOption(DocumentType documentType) {
        return new DocumentTypeOptionDTO(
                documentType.getUuid(),
                documentType.getName(),
                documentType.getDescription(),
                documentType.getMaxFileSizeMb(),
                parseAllowedExtensions(documentType.getAllowedExtensions()),
                documentType.getIsRequired()
        );
    }

    private List<String> parseAllowedExtensions(String rawExtensions) {
        if (!StringUtils.hasText(rawExtensions)) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(rawExtensions);
            if (!node.isArray()) {
                return List.of();
            }

            List<String> extensions = new ArrayList<>();
            node.forEach(item -> {
                if (item.isTextual()) {
                    extensions.add(item.asText());
                }
            });
            return List.copyOf(extensions);
        } catch (Exception ex) {
            log.warn("Unable to parse allowed_extensions for document type; returning empty list", ex);
            return List.of();
        }
    }
}
