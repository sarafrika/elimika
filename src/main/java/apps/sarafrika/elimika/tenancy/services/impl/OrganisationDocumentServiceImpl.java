package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import apps.sarafrika.elimika.tenancy.dto.OrganisationDocumentDTO;
import apps.sarafrika.elimika.tenancy.entity.OrganisationDocument;
import apps.sarafrika.elimika.tenancy.factory.OrganisationDocumentFactory;
import apps.sarafrika.elimika.tenancy.repository.OrganisationDocumentRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.services.OrganisationDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationDocumentServiceImpl implements OrganisationDocumentService {

    private final OrganisationDocumentRepository organisationDocumentRepository;
    private final OrganisationRepository organisationRepository;

    @Override
    @Transactional
    public OrganisationDocumentDTO createOrganisationDocument(OrganisationDocumentDTO organisationDocumentDTO) {
        requireOrganisation(organisationDocumentDTO.organisationUuid());

        OrganisationDocument document = OrganisationDocumentFactory.toEntity(organisationDocumentDTO);
        document.setUploadDate(LocalDateTime.now());
        document.setIsVerified(false);
        document.setStatus(DocumentStatus.PENDING);

        OrganisationDocument saved = organisationDocumentRepository.save(document);
        log.debug("Stored validation document {} for organisation {}", saved.getUuid(), saved.getOrganisationUuid());
        return OrganisationDocumentFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisationDocumentDTO> getOrganisationDocuments(UUID organisationUuid) {
        requireOrganisation(organisationUuid);

        return organisationDocumentRepository.findByOrganisationUuid(organisationUuid)
                .stream()
                .map(OrganisationDocumentFactory::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteOrganisationDocument(UUID organisationUuid, UUID documentUuid) {
        OrganisationDocument document = organisationDocumentRepository.findByUuid(documentUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organisation document with ID " + documentUuid + " not found"));

        if (!document.getOrganisationUuid().equals(organisationUuid)) {
            throw new ResourceNotFoundException(
                    "Organisation document with ID " + documentUuid + " not found for organisation " + organisationUuid);
        }

        organisationDocumentRepository.delete(document);
    }

    private void requireOrganisation(UUID organisationUuid) {
        if (organisationUuid == null
                || organisationRepository.findByUuidAndDeletedFalse(organisationUuid).isEmpty()) {
            throw new ResourceNotFoundException("Organisation with ID " + organisationUuid + " not found");
        }
    }
}
