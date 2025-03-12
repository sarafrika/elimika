package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.event.mailgun.WebhooksCreationEvent;
import apps.sarafrika.elimika.common.event.organisation.OrganisationCreationEvent;
import apps.sarafrika.elimika.common.event.organisation.SuccessfulOrganisationCreationEvent;
import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.factory.OrganisationFactory;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.services.OrganisationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationServiceImpl implements OrganisationService {
    private static final long INITIAL_CODE = 1000L;

    @Value("${app.keycloak.realm}")
    private String realm;

    private final ApplicationEventPublisher eventPublisher;
    private final OrganisationRepository organisationRepository;
    private final GenericSpecificationBuilder<Organisation> specificationBuilder;
    private final Lock orgCodeLock = new ReentrantLock();

    @Override
    @Transactional
    public OrganisationDTO createOrganisation(OrganisationDTO organisationDTO) {
        log.debug("Creating new organisation: {}", organisationDTO.name());

        try {
            Organisation organisation = OrganisationFactory.toEntity(organisationDTO);
            organisation.setAuthRealm(realm);
            organisation.setSlug(organisation.getName().replaceAll("\\s+", "-").toLowerCase());
            organisation = organisationRepository.save(organisation);

            publishOrganisationCreationEvent(organisation);

            eventPublisher.publishEvent(new WebhooksCreationEvent(organisation.getDomain(), ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUriString()));

            log.info("Successfully created organisation with UUID: {}", organisation.getUuid());
            return OrganisationFactory.toDTO(organisation);
        } catch (Exception e) {
            log.error("Failed to create organisation: {}", organisationDTO.name(), e);
            throw new RuntimeException("Failed to create organisation: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrganisationDTO getOrganisationByUuid(UUID uuid) {
        log.debug("Fetching organisation by UUID: {}", uuid);
        return organisationRepository.findByUuid(uuid)
                .map(OrganisationFactory::toDTO)
                .orElseThrow(() -> {
                    log.warn("Organisation not found for UUID: {}", uuid);
                    return new RecordNotFoundException("Organisation not found for UUID: " + uuid);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganisationDTO> getAllOrganisations(Pageable pageable) {
        log.debug("Fetching all organisations");
        return organisationRepository.findAll(pageable)
                .map(OrganisationFactory::toDTO);
    }


    @Override
    @Transactional
    public OrganisationDTO updateOrganisation(UUID uuid, OrganisationDTO organisationDTO) {
        log.debug("Updating organisation with UUID: {}", uuid);

        try {
            Organisation organisation = findOrganisationOrThrow(uuid);
            updateOrganisationFields(organisation, organisationDTO);
            organisation = organisationRepository.save(organisation);

            log.info("Successfully updated organisation with UUID: {}", uuid);
            return OrganisationFactory.toDTO(organisation);
        } catch (RecordNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update organisation with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to update organisation: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteOrganisation(UUID uuid) {
        log.debug("Deleting organisation with UUID: {}", uuid);

        try {
            Organisation organisation = findOrganisationOrThrow(uuid);
            organisationRepository.delete(organisation);
            log.info("Successfully deleted organisation with UUID: {}", uuid);
        } catch (RecordNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete organisation with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to delete organisation: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganisationDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Organisation> spec = specificationBuilder.buildSpecification(Organisation.class, searchParams);
        Page<Organisation> organisations = organisationRepository.findAll(spec, pageable);
        return organisations.map(OrganisationFactory::toDTO);
    }

    @ApplicationModuleListener
    void onOrganisationCreation(SuccessfulOrganisationCreationEvent event) {
        log.debug("Processing successful organisation creation event for UUID: {}", event.blastWaveId());

        try {
            Organisation organisation = findOrganisationOrThrow(event.blastWaveId());
            organisation.setKeycloakId(event.keycloakId());
            organisationRepository.save(organisation);
            log.info("Successfully processed organisation creation event for UUID: {}", event.blastWaveId());
        } catch (Exception e) {
            log.error("Failed to process organisation creation event for UUID: {}", event.blastWaveId(), e);
            throw new RuntimeException("Failed to process organisation creation event: " + e.getMessage(), e);
        }
    }

    private Organisation findOrganisationOrThrow(UUID uuid) {
        return organisationRepository.findByUuid(uuid)
                .orElseThrow(() -> new RecordNotFoundException("Organisation not found for UUID: " + uuid));
    }

    private void updateOrganisationFields(Organisation organisation, OrganisationDTO dto) {
        organisation.setName(dto.name());
        organisation.setDescription(dto.description());
        organisation.setActive(dto.active());
    }

    private void publishOrganisationCreationEvent(Organisation organisation) {
        eventPublisher.publishEvent(new OrganisationCreationEvent(
                organisation.getName(),
                organisation.getSlug(),
                organisation.getDescription(),
                realm,
                organisation.getDomain(),
                organisation.getUuid()
        ));
    }
}