package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.*;
import apps.sarafrika.elimika.tenancy.factory.TrainingBranchFactory;
import apps.sarafrika.elimika.tenancy.factory.UserFactory;
import apps.sarafrika.elimika.tenancy.repository.*;
import apps.sarafrika.elimika.tenancy.services.TrainingBranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingBranchServiceImpl implements TrainingBranchService {

    private final TrainingBranchRepository trainingBranchRepository;
    private final UserRepository userRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;
    private final OrganisationRepository organisationRepository;
    private final GenericSpecificationBuilder<TrainingBranch> specificationBuilder;

    @Override
    @Transactional
    public TrainingBranchDTO createTrainingBranch(TrainingBranchDTO trainingBranchDTO) {
        log.debug("Creating new training branch: {}", trainingBranchDTO.branchName());

        try {
            TrainingBranch trainingBranch = TrainingBranchFactory.toEntity(trainingBranchDTO);

            // Check for duplicate branch name within the same organisation
            if (trainingBranchRepository.existsByOrganisationUuidAndBranchNameAndDeletedFalse(
                    trainingBranch.getOrganisationUuid(), trainingBranch.getBranchName())) {
                throw new IllegalArgumentException("Training branch with this name already exists in the organisation");
            }

            // Validate organisation exists
            organisationRepository.findByUuid(trainingBranch.getOrganisationUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));

            trainingBranch = trainingBranchRepository.save(trainingBranch);

            log.info("Successfully created training branch with UUID: {}", trainingBranch.getUuid());
            return TrainingBranchFactory.toDTO(trainingBranch);
        } catch (Exception e) {
            log.error("Failed to create training branch: {}", trainingBranchDTO.branchName(), e);
            throw new RuntimeException("Failed to create training branch.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingBranchDTO getTrainingBranchByUuid(UUID uuid) {
        log.debug("Fetching training branch by UUID: {}", uuid);
        return trainingBranchRepository.findByUuidAndDeletedFalse(uuid)
                .map(TrainingBranchFactory::toDTO)
                .orElseThrow(() -> {
                    log.warn("Training branch not found for UUID: {}", uuid);
                    return new ResourceNotFoundException("Training branch not found for UUID: " + uuid);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingBranchDTO> getAllTrainingBranches(Pageable pageable) {
        log.debug("Fetching all training branches");
        return trainingBranchRepository.findByDeletedFalse(pageable)
                .map(TrainingBranchFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingBranchDTO> getTrainingBranchesByOrganisation(UUID organisationUuid, Pageable pageable) {
        log.debug("Fetching training branches for organisation UUID: {}", organisationUuid);
        return trainingBranchRepository.findByOrganisationUuidAndDeletedFalse(organisationUuid, pageable)
                .map(TrainingBranchFactory::toDTO);
    }

    @Override
    @Transactional
    public TrainingBranchDTO updateTrainingBranch(UUID uuid, TrainingBranchDTO trainingBranchDTO) {
        log.debug("Updating training branch with UUID: {}", uuid);

        try {
            TrainingBranch trainingBranch = findTrainingBranchOrThrow(uuid);
            updateTrainingBranchFields(trainingBranch, trainingBranchDTO);

            trainingBranch = trainingBranchRepository.save(trainingBranch);

            log.info("Successfully updated training branch with UUID: {}", uuid);
            return TrainingBranchFactory.toDTO(trainingBranch);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update training branch with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to update training branch: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteTrainingBranch(UUID uuid) {
        log.debug("Soft deleting training branch with UUID: {}", uuid);

        try {
            TrainingBranch trainingBranch = findTrainingBranchOrThrow(uuid);

            // Remove all user assignments to this branch
            List<UserOrganisationDomainMapping> branchMappings =
                    userOrganisationDomainMappingRepository.findActiveByBranch(uuid);

            branchMappings.forEach(mapping -> {
                mapping.setBranchUuid(null);
            });
            userOrganisationDomainMappingRepository.saveAll(branchMappings);

            // Soft delete the branch
            trainingBranch.setDeleted(true);
            trainingBranch.setActive(false);
            trainingBranchRepository.save(trainingBranch);
            log.info("Successfully soft deleted training branch with UUID: {}", uuid);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete training branch with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to delete training branch: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingBranchDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<TrainingBranch> spec = specificationBuilder.buildSpecification(TrainingBranch.class, searchParams);
        // Add deleted=false filter to search
        Specification<TrainingBranch> notDeletedSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.isFalse(root.get("deleted"));
        Specification<TrainingBranch> combinedSpec = spec.and(notDeletedSpec);

        Page<TrainingBranch> trainingBranches = trainingBranchRepository.findAll(combinedSpec, pageable);
        return trainingBranches.map(TrainingBranchFactory::toDTO);
    }

    @Override
    @Transactional
    public void assignUserToBranch(UUID branchUuid, UUID userUuid, String domainName) {
        log.debug("Assigning user {} to branch {} with domain {}", userUuid, branchUuid, domainName);

        TrainingBranch branch = findTrainingBranchOrThrow(branchUuid);
        User user = findUserOrThrow(userUuid);
        UserDomain domain = findDomainByNameOrThrow(domainName);

        // Check if user is already in the organisation
        UserOrganisationDomainMapping existingMapping = userOrganisationDomainMappingRepository
                .findActiveByUserAndOrganisation(userUuid, branch.getOrganisationUuid())
                .orElse(null);

        if (existingMapping != null) {
            // Update existing mapping to include branch
            existingMapping.setBranchUuid(branchUuid);
            existingMapping.setDomainUuid(domain.getUuid());
            userOrganisationDomainMappingRepository.save(existingMapping);
        } else {
            // Create new mapping with branch assignment
            UserOrganisationDomainMapping mapping = UserOrganisationDomainMapping.builder()
                    .userUuid(userUuid)
                    .organisationUuid(branch.getOrganisationUuid())
                    .domainUuid(domain.getUuid())
                    .branchUuid(branchUuid)
                    .active(true)
                    .startDate(LocalDate.now())
                    .createdBy("system")
                    .build();

            userOrganisationDomainMappingRepository.save(mapping);
        }

        log.info("Successfully assigned user {} to branch {} with domain {}", userUuid, branchUuid, domainName);
    }

    @Override
    @Transactional
    public void removeUserFromBranch(UUID branchUuid, UUID userUuid) {
        log.debug("Removing user {} from branch {}", userUuid, branchUuid);

        TrainingBranch branch = findTrainingBranchOrThrow(branchUuid);

        List<UserOrganisationDomainMapping> mappings = userOrganisationDomainMappingRepository
                .findActiveByUserAndOrganisation(userUuid, branch.getOrganisationUuid())
                .stream()
                .filter(mapping -> branchUuid.equals(mapping.getBranchUuid()))
                .toList();

        if (mappings.isEmpty()) {
            throw new ResourceNotFoundException("User is not assigned to this branch");
        }

        // Remove branch assignment but keep organisation assignment
        mappings.forEach(mapping -> {
            mapping.setBranchUuid(null);
        });

        userOrganisationDomainMappingRepository.saveAll(mappings);
        log.info("Removed user {} from branch {}", userUuid, branchUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getBranchUsers(UUID branchUuid) {
        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByBranch(branchUuid);

        return mappings.stream()
                .map(mapping -> {
                    User user = findUserOrThrow(mapping.getUserUuid());
                    List<String> userDomains = getUserDomainsForUser(user.getUuid());
                    return UserFactory.toDTO(user, userDomains);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getBranchUsersByDomain(UUID branchUuid, String domainName) {
        UserDomain domain = findDomainByNameOrThrow(domainName);

        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByBranch(branchUuid)
                        .stream()
                        .filter(mapping -> mapping.getDomainUuid().equals(domain.getUuid()))
                        .toList();

        return mappings.stream()
                .map(mapping -> {
                    User user = findUserOrThrow(mapping.getUserUuid());
                    List<String> userDomains = getUserDomainsForUser(user.getUuid());
                    return UserFactory.toDTO(user, userDomains);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingBranchDTO> getUserBranches(UUID userUuid) {
        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByUser(userUuid)
                        .stream()
                        .filter(mapping -> mapping.getBranchUuid() != null)
                        .toList();

        return mappings.stream()
                .map(mapping -> {
                    TrainingBranch branch = findTrainingBranchOrThrow(mapping.getBranchUuid());
                    return TrainingBranchFactory.toDTO(branch);
                })
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserInBranch(UUID branchUuid, UUID userUuid) {
        return userOrganisationDomainMappingRepository.findActiveByBranch(branchUuid)
                .stream()
                .anyMatch(mapping -> mapping.getUserUuid().equals(userUuid));
    }

    @Override
    @Transactional
    public void updatePointOfContact(UUID branchUuid, UUID pocUserUuid) {
        log.debug("Updating point of contact for branch {} to user {}", branchUuid, pocUserUuid);

        TrainingBranch branch = findTrainingBranchOrThrow(branchUuid);
        User pocUser = findUserOrThrow(pocUserUuid);

        // Verify the user is assigned to this branch or organisation
        boolean userInBranch = isUserInBranch(branchUuid, pocUserUuid);
        boolean userInOrganisation = userOrganisationDomainMappingRepository
                .findActiveByUserAndOrganisation(pocUserUuid, branch.getOrganisationUuid())
                .isPresent();

        if (!userInBranch && !userInOrganisation) {
            throw new IllegalArgumentException("Point of contact must be assigned to the branch or organisation");
        }

        branch.setPocUserUuid(pocUserUuid);
        trainingBranchRepository.save(branch);

        log.info("Updated point of contact for branch {} to user {}", branchUuid, pocUserUuid);
    }

    // Private helper methods
    private TrainingBranch findTrainingBranchOrThrow(UUID uuid) {
        return trainingBranchRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Training branch not found for UUID: " + uuid));
    }

    private User findUserOrThrow(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for UUID: " + uuid));
    }

    private UserDomain findDomainByNameOrThrow(String domainName) {
        return userDomainRepository.findByDomainName(domainName)
                .orElseThrow(() -> new IllegalArgumentException("No known domain with the provided name: " + domainName));
    }

    private List<String> getUserDomainsForUser(UUID userUuid) {
        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByUser(userUuid);

        return mappings.stream()
                .map(mapping -> {
                    UserDomain domain = userDomainRepository.findByUuid(mapping.getDomainUuid())
                            .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));
                    return domain.getDomainName();
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private void updateTrainingBranchFields(TrainingBranch trainingBranch, TrainingBranchDTO dto) {
        trainingBranch.setOrganisationUuid(dto.organisationUuid());
        trainingBranch.setBranchName(dto.branchName());
        trainingBranch.setAddress(dto.address());
        trainingBranch.setPocUserUuid(dto.pocUserUuid());
        trainingBranch.setActive(dto.active());
    }
}