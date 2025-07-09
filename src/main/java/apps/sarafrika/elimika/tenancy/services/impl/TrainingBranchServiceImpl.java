package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import apps.sarafrika.elimika.tenancy.factory.TrainingBranchFactory;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.services.TrainingBranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingBranchServiceImpl implements TrainingBranchService {

    private final TrainingBranchRepository trainingBranchRepository;
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
            // Soft delete instead of hard delete
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

    private TrainingBranch findTrainingBranchOrThrow(UUID uuid) {
        return trainingBranchRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Training branch not found for UUID: " + uuid));
    }

    private void updateTrainingBranchFields(TrainingBranch trainingBranch, TrainingBranchDTO dto) {
        trainingBranch.setOrganisationUuid(dto.organisationUuid());
        trainingBranch.setBranchName(dto.branchName());
        trainingBranch.setAddress(dto.address());
        trainingBranch.setPocUserUuid(dto.pocUserUuid());
        trainingBranch.setActive(dto.active());
    }
}