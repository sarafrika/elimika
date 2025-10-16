package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.service.UserContextService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.*;
import apps.sarafrika.elimika.tenancy.factory.OrganisationFactory;
import apps.sarafrika.elimika.tenancy.factory.UserFactory;
import apps.sarafrika.elimika.tenancy.repository.*;
import apps.sarafrika.elimika.tenancy.services.OrganisationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationServiceImpl implements OrganisationService {


    private final ApplicationEventPublisher eventPublisher;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;
    private final TrainingBranchRepository trainingBranchRepository;
    private final GenericSpecificationBuilder<Organisation> specificationBuilder;
    private final UserContextService userContextService;

    @Override
    @Transactional
    public OrganisationDTO createOrganisation(OrganisationDTO organisationDTO) {
        log.debug("Creating new organisation with automatic creator assignment: {}", organisationDTO.name());

        try {
            Organisation organisation = OrganisationFactory.toEntity(organisationDTO);

            // Auto-generate slug from name
            organisation.setSlug(organisation.getName().replaceAll("\\s+", "-").toLowerCase());

            final Organisation savedOrganisation = organisationRepository.save(organisation);

            // Automatically assign the current authenticated user as organisation_user
            userContextService.getCurrentUserUuidOptional().ifPresent(creatorUuid -> {
                log.debug("Assigning current user {} as admin for organisation {}", creatorUuid, savedOrganisation.getUuid());
                assignCreatorAsOrganisationUser(creatorUuid, savedOrganisation.getUuid());
            });

            log.info("Successfully created organisation with UUID: {}", savedOrganisation.getUuid());
            return OrganisationFactory.toDTO(savedOrganisation);
        } catch (Exception e) {
            log.error("Failed to create organisation: {}", organisationDTO.name(), e);
            throw new RuntimeException("Failed to create organisation.", e);
        }
    }

    @Override
    @Transactional
    public OrganisationDTO createOrganisation(OrganisationDTO organisationDTO, UUID creatorUuid) {
        log.debug("Creating new organisation with creator {}: {}", creatorUuid, organisationDTO.name());

        try {
            Organisation organisation = OrganisationFactory.toEntity(organisationDTO);

            // Auto-generate slug from name
            organisation.setSlug(organisation.getName().replaceAll("\\s+", "-").toLowerCase());

            organisation = organisationRepository.save(organisation);

            // Automatically assign the creating user as organisation_user
            if (creatorUuid != null) {
                assignCreatorAsOrganisationUser(creatorUuid, organisation.getUuid());
            }

            log.info("Successfully created organisation with UUID: {}, creator {} assigned as admin",
                    organisation.getUuid(), creatorUuid);
            return OrganisationFactory.toDTO(organisation);
        } catch (Exception e) {
            log.error("Failed to create organisation: {}", organisationDTO.name(), e);
            throw new RuntimeException("Failed to create organisation.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrganisationDTO getOrganisationByUuid(UUID uuid) {
        log.debug("Fetching organisation by UUID: {}", uuid);
        return organisationRepository.findByUuidAndDeletedFalse(uuid)
                .map(OrganisationFactory::toDTO)
                .orElseThrow(() -> {
                    log.warn("Organisation not found for UUID: {}", uuid);
                    return new ResourceNotFoundException("Organisation not found for UUID: " + uuid);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganisationDTO> getAllOrganisations(Pageable pageable) {
        log.debug("Fetching all organisations");
        return organisationRepository.findByDeletedFalse(pageable)
                .map(OrganisationFactory::toDTO);
    }

    @Override
    @Transactional
    public OrganisationDTO updateOrganisation(UUID uuid, OrganisationDTO organisationDTO) {
        log.debug("Updating organisation with UUID: {}", uuid);

        try {
            Organisation organisation = findOrganisationOrThrow(uuid);
            updateOrganisationFields(organisation, organisationDTO);

            // Update slug if name changed
            organisation.setSlug(organisation.getName().replaceAll("\\s+", "-").toLowerCase());

            organisation = organisationRepository.save(organisation);

            log.info("Successfully updated organisation with UUID: {}", uuid);
            return OrganisationFactory.toDTO(organisation);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update organisation with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to update organisation: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteOrganisation(UUID uuid) {
        log.debug("Soft deleting organisation with UUID: {}", uuid);

        try {
            Organisation organisation = findOrganisationOrThrow(uuid);

            // Soft delete all user mappings for this organisation
            List<UserOrganisationDomainMapping> mappings =
                    userOrganisationDomainMappingRepository.findActiveByOrganisation(uuid);
            mappings.forEach(mapping -> {
                mapping.setActive(false);
                mapping.setEndDate(LocalDate.now());
                mapping.setDeleted(true);
            });
            userOrganisationDomainMappingRepository.saveAll(mappings);

            // Soft delete the organisation
            organisation.setDeleted(true);
            organisation.setActive(false);
            organisationRepository.save(organisation);
            log.info("Successfully soft deleted organisation with UUID: {}", uuid);
        } catch (ResourceNotFoundException e) {
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
        // Add deleted=false filter to search
        Specification<Organisation> notDeletedSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.isFalse(root.get("deleted"));
        Specification<Organisation> combinedSpec = spec.and(notDeletedSpec);

        Page<Organisation> organisations = organisationRepository.findAll(combinedSpec, pageable);
        return organisations.map(OrganisationFactory::toDTO);
    }

    @Override
    @Transactional
    public void inviteUserToOrganisation(UUID organisationUuid, String email, String domainName, UUID branchUuid) {
        log.debug("Inviting user with email {} to organisation {} with domain {}", email, organisationUuid, domainName);

        Organisation organisation = findOrganisationOrThrow(organisationUuid);
        UserDomain domain = findDomainByNameOrThrow(domainName);

        // Validate branch belongs to organisation if provided
        if (branchUuid != null) {
            TrainingBranch branch = trainingBranchRepository.findByUuid(branchUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Training branch not found"));

            if (!branch.getOrganisationUuid().equals(organisationUuid)) {
                throw new IllegalArgumentException("Training branch does not belong to the specified organisation");
            }
        }

        // Check if user exists
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // For admin and organisation_user roles, user must exist (they should register first)
            if ("admin".equals(domainName) || "organisation_user".equals(domainName)) {
                throw new IllegalStateException("User must register on the platform before being invited as " + domainName +
                        ". Please ask them to create an account first.");
            }

            // For student/instructor invitations, we'll create invitation and let them register later
            log.info("Creating invitation for non-existent user {} with domain {}", email, domainName);
            // This will be handled by the InvitationService
            return;
        }

        User user = userOptional.get();

        // Check if mapping already exists
        Optional<UserOrganisationDomainMapping> existingMapping =
                userOrganisationDomainMappingRepository.findActiveByUserAndOrganisation(user.getUuid(), organisationUuid);

        if (existingMapping.isPresent()) {
            log.warn("User {} is already associated with organisation {}", email, organisationUuid);
            throw new IllegalStateException("User is already associated with this organisation");
        }

        // Create new mapping
        UserOrganisationDomainMapping mapping = UserOrganisationDomainMapping.builder()
                .userUuid(user.getUuid())
                .organisationUuid(organisationUuid)
                .domainUuid(domain.getUuid())
                .branchUuid(branchUuid)
                .active(true)
                .startDate(LocalDate.now())
                .createdBy("system") // TODO: Replace with actual user context
                .build();

        userOrganisationDomainMappingRepository.save(mapping);

        // For invitation-only roles, also add to standalone domains
        if ("admin".equals(domainName) || "organisation_user".equals(domainName)) {
            addStandaloneDomainToUser(user, domain);
        }

        log.info("Successfully invited user {} to organisation {} with domain {}", email, organisationUuid, domainName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getOrganisationUsers(UUID organisationUuid) {
        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByOrganisation(organisationUuid);

        return mappings.stream()
                .map(mapping -> {
                    User user = userRepository.findByUuid(mapping.getUserUuid())
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                    // Get all user domains for this user
                    List<String> userDomains = getUserDomainsForUser(user.getUuid());
                    return UserFactory.toDTO(user, userDomains);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getOrganisationUsersByDomain(UUID organisationUuid, String domainName) {
        UserDomain domain = findDomainByNameOrThrow(domainName);

        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByOrganisationAndDomain(organisationUuid, domain.getUuid());

        return mappings.stream()
                .map(mapping -> {
                    User user = userRepository.findByUuid(mapping.getUserUuid())
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    List<String> userDomains = getUserDomainsForUser(user.getUuid());
                    return UserFactory.toDTO(user, userDomains);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeUserFromOrganisation(UUID organisationUuid, UUID userUuid) {
        log.debug("Removing user {} from organisation {}", userUuid, organisationUuid);

        UserOrganisationDomainMapping mapping = userOrganisationDomainMappingRepository
                .findActiveByUserAndOrganisation(userUuid, organisationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active mapping found for user and organisation"));

        // Soft delete - set end date and deactivate
        mapping.setActive(false);
        mapping.setEndDate(LocalDate.now());
        mapping.setDeleted(true);

        userOrganisationDomainMappingRepository.save(mapping);
        log.info("Removed user {} from organisation {}", userUuid, organisationUuid);
    }

    @Override
    @Transactional
    public void updateUserRoleInOrganisation(UUID organisationUuid, UUID userUuid, String newDomainName) {
        log.debug("Updating user {} role in organisation {} to {}", userUuid, organisationUuid, newDomainName);

        UserDomain newDomain = findDomainByNameOrThrow(newDomainName);

        UserOrganisationDomainMapping mapping = userOrganisationDomainMappingRepository
                .findActiveByUserAndOrganisation(userUuid, organisationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active mapping found for user and organisation"));

        mapping.setDomainUuid(newDomain.getUuid());

        userOrganisationDomainMappingRepository.save(mapping);

        // If updating to invitation-only role, add to standalone domains
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("admin".equals(newDomainName) || "organisation_user".equals(newDomainName)) {
            addStandaloneDomainToUser(user, newDomain);
        }

        log.info("Updated user {} role in organisation {} to {}", userUuid, organisationUuid, newDomainName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisationDTO> getUserOrganisations(UUID userUuid) {
        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByUser(userUuid);

        return mappings.stream()
                .map(mapping -> {
                    Organisation organisation = organisationRepository.findByUuid(mapping.getOrganisationUuid())
                            .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
                    return OrganisationFactory.toDTO(organisation);
                })
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserInOrganisation(UUID organisationUuid, UUID userUuid) {
        return userOrganisationDomainMappingRepository
                .findActiveByUserAndOrganisation(userUuid, organisationUuid)
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserRoleInOrganisation(UUID organisationUuid, UUID userUuid) {
        UserOrganisationDomainMapping mapping = userOrganisationDomainMappingRepository
                .findActiveByUserAndOrganisation(userUuid, organisationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User is not associated with this organisation"));

        UserDomain domain = userDomainRepository.findByUuid(mapping.getDomainUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));

        return domain.getDomainName();
    }


    // ================================
    // PRIVATE HELPER METHODS
    // ================================

    /**
     * Automatically assigns the creating user as organisation_user
     */
    private void assignCreatorAsOrganisationUser(UUID creatorUuid, UUID organisationUuid) {
        log.debug("Assigning creator {} as admin for organisation {}", creatorUuid, organisationUuid);

        try {
            User creator = userRepository.findByUuid(creatorUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Creator user not found"));

            UserDomain adminDomain = findDomainByNameOrThrow("admin");
            UserDomain organisationUserDomain = findDomainByNameOrThrow("organisation_user");

            // Create organisation mapping
            UserOrganisationDomainMapping mapping = UserOrganisationDomainMapping.builder()
                    .userUuid(creatorUuid)
                    .organisationUuid(organisationUuid)
                    .domainUuid(adminDomain.getUuid())
                    .branchUuid(null) // Primary organisation user, not branch-specific
                    .active(true)
                    .startDate(LocalDate.now())
                    .createdBy("system")
                    .build();

            userOrganisationDomainMappingRepository.save(mapping);

            // Add organisation_user domain to standalone domains
            addStandaloneDomainToUser(creator, organisationUserDomain);

            log.info("Successfully assigned creator {} as admin for organisation {}", creatorUuid, organisationUuid);
        } catch (Exception e) {
            log.error("Failed to assign creator as admin", e);
            throw new RuntimeException("Failed to assign creator as admin: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a standalone domain to a user (not organisation-specific)
     */
    private void addStandaloneDomainToUser(User user, UserDomain domain) {
        // Check if mapping already exists
        if (!userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(user.getUuid(), domain.getUuid())) {
            UserDomainMapping mapping = new UserDomainMapping();
            mapping.setUserUuid(user.getUuid());
            mapping.setUserDomainUuid(domain.getUuid());
            userDomainMappingRepository.save(mapping);
            log.info("Added standalone domain {} to user {}", domain.getDomainName(), user.getUuid());
        }
    }

    private Organisation findOrganisationOrThrow(UUID uuid) {
        return organisationRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found for UUID: " + uuid));
    }

    private UserDomain findDomainByNameOrThrow(String domainName) {
        return userDomainRepository.findByDomainName(domainName)
                .orElseThrow(() -> new IllegalArgumentException("No known domain with the provided name: " + domainName));
    }

    private List<String> getUserDomainsForUser(UUID userUuid) {
        Set<String> allDomains = new HashSet<>();

        // Get standalone domains
        List<UserDomainMapping> standaloneMappings = userDomainMappingRepository.findByUserUuid(userUuid);
        for (UserDomainMapping mapping : standaloneMappings) {
            UserDomain domain = userDomainRepository.findByUuid(mapping.getUserDomainUuid()).orElse(null);
            if (domain != null) {
                allDomains.add(domain.getDomainName());
            }
        }

        // Get domains from organisation mappings
        List<UserOrganisationDomainMapping> orgMappings =
                userOrganisationDomainMappingRepository.findActiveByUser(userUuid);
        for (UserOrganisationDomainMapping mapping : orgMappings) {
            UserDomain domain = userDomainRepository.findByUuid(mapping.getDomainUuid()).orElse(null);
            if (domain != null) {
                allDomains.add(domain.getDomainName());
            }
        }

        return new ArrayList<>(allDomains);
    }

    private void updateOrganisationFields(Organisation organisation, OrganisationDTO dto) {
        if (dto.name() != null) {
            organisation.setName(dto.name());
        }
        if (dto.description() != null) {
            organisation.setDescription(dto.description());
        }
        organisation.setActive(dto.active());
        if (dto.licenceNo() != null) {
            organisation.setLicenceNo(dto.licenceNo());
        }
        if (dto.location() != null) {
            organisation.setLocation(dto.location());
        }
        if (dto.country() != null) {
            organisation.setCountry(dto.country());
        }
        if (dto.latitude() != null) {
            organisation.setLatitude(dto.latitude());
        }
        if (dto.longitude() != null) {
            organisation.setLongitude(dto.longitude());
        }
    }

    // ================================
    // ORGANIZATION VERIFICATION METHODS
    // ================================

    @Override
    @Transactional
    public OrganisationDTO verifyOrganisation(UUID organisationUuid, String reason) {
        log.debug("Verifying organisation: {} with reason: {}", organisationUuid, reason);

        Organisation organisation = findOrganisationOrThrow(organisationUuid);

        if (Boolean.TRUE.equals(organisation.getAdminVerified())) {
            log.warn("Organisation {} is already verified", organisationUuid);
        } else {
            organisation.setAdminVerified(true);
            organisation = organisationRepository.save(organisation);
            log.info("Successfully verified organisation: {} for reason: {}", organisationUuid, reason);
        }

        return OrganisationFactory.toDTO(organisation);
    }

    @Override
    @Transactional
    public OrganisationDTO unverifyOrganisation(UUID organisationUuid, String reason) {
        log.debug("Removing verification from organisation: {} with reason: {}", organisationUuid, reason);

        Organisation organisation = findOrganisationOrThrow(organisationUuid);

        if (Boolean.FALSE.equals(organisation.getAdminVerified()) || organisation.getAdminVerified() == null) {
            log.warn("Organisation {} is already unverified", organisationUuid);
        } else {
            organisation.setAdminVerified(false);
            organisation = organisationRepository.save(organisation);
            log.info("Successfully removed verification from organisation: {} for reason: {}", organisationUuid, reason);
        }

        return OrganisationFactory.toDTO(organisation);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOrganisationVerified(UUID organisationUuid) {
        log.debug("Checking verification status for organisation: {}", organisationUuid);

        Organisation organisation = findOrganisationOrThrow(organisationUuid);
        boolean isVerified = Boolean.TRUE.equals(organisation.getAdminVerified());

        log.debug("Organisation {} verification status: {}", organisationUuid, isVerified);
        return isVerified;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganisationDTO> getVerifiedOrganisations(Pageable pageable) {
        log.debug("Fetching verified organisations with pagination: {}", pageable);

        Page<Organisation> organisations = organisationRepository.findByAdminVerifiedTrueAndDeletedFalse(pageable);

        log.debug("Found {} verified organisations", organisations.getTotalElements());
        return organisations.map(OrganisationFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganisationDTO> getUnverifiedOrganisations(Pageable pageable) {
        log.debug("Fetching unverified organisations with pagination: {}", pageable);

        Page<Organisation> organisations = organisationRepository.findByAdminVerifiedFalseOrNullAndDeletedFalse(pageable);

        log.debug("Found {} unverified organisations", organisations.getTotalElements());
        return organisations.map(OrganisationFactory::toDTO);
    }

}