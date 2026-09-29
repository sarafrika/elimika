package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.instructor.dto.InstructorExperienceDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorExperienceFactory;
import apps.sarafrika.elimika.instructor.model.InstructorExperience;
import apps.sarafrika.elimika.instructor.repository.InstructorExperienceRepository;
import apps.sarafrika.elimika.instructor.service.InstructorExperienceService;
import apps.sarafrika.elimika.instructor.search.InstructorSearchSource;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorExperienceServiceImpl implements InstructorExperienceService {

    private final InstructorExperienceRepository instructorExperienceRepository;
    private final GenericSpecificationBuilder<InstructorExperience> specificationBuilder;
    private final SearchIndexRequests searchIndexRequests;

    private static final String INSTRUCTOR_EXPERIENCE_NOT_FOUND_TEMPLATE = "Instructor experience with ID %s not found";

    @Override
    public InstructorExperienceDTO createInstructorExperience(InstructorExperienceDTO instructorExperienceDTO) {
        InstructorExperience instructorExperience = InstructorExperienceFactory.toEntity(instructorExperienceDTO);
        instructorExperience.setCreatedDate(LocalDateTime.now());

        // Set default values
        if (instructorExperience.getIsCurrentPosition() == null) {
            instructorExperience.setIsCurrentPosition(false);
        }

        // If it's a current position, ensure end date is null
        if (Boolean.TRUE.equals(instructorExperience.getIsCurrentPosition())) {
            instructorExperience.setEndDate(null);
        }

        InstructorExperience savedExperience = instructorExperienceRepository.save(instructorExperience);
        return InstructorExperienceFactory.toDTO(savedExperience);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorExperienceDTO getInstructorExperienceByUuid(UUID uuid) {
        return instructorExperienceRepository.findByUuid(uuid)
                .map(InstructorExperienceFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_EXPERIENCE_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorExperienceDTO> getAllInstructorExperience(Pageable pageable) {
        specificationBuilder.validateSortProperties(InstructorExperience.class, pageable);
        return instructorExperienceRepository.findAll(pageable).map(InstructorExperienceFactory::toDTO);
    }

    @Override
    public InstructorExperienceDTO updateInstructorExperience(UUID uuid, InstructorExperienceDTO instructorExperienceDTO) {
        InstructorExperience existingExperience = instructorExperienceRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_EXPERIENCE_NOT_FOUND_TEMPLATE, uuid)));

        // Update fields from DTO
        updateExperienceFields(existingExperience, instructorExperienceDTO);

        // Business logic: if it's a current position, ensure end date is null
        if (Boolean.TRUE.equals(existingExperience.getIsCurrentPosition())) {
            existingExperience.setEndDate(null);
        }

        InstructorExperience updatedExperience = instructorExperienceRepository.save(existingExperience);
        return InstructorExperienceFactory.toDTO(updatedExperience);
    }

    @Override
    public void deleteInstructorExperience(UUID uuid) {
        if (!instructorExperienceRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(INSTRUCTOR_EXPERIENCE_NOT_FOUND_TEMPLATE, uuid));
        }
        instructorExperienceRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorExperienceDTO> search(Map<String, String> searchParams, Pageable pageable) {
        specificationBuilder.validateSortProperties(InstructorExperience.class, pageable);
        Specification<InstructorExperience> spec = specificationBuilder.buildSpecification(InstructorExperience.class, searchParams);
        return instructorExperienceRepository.findAll(spec, pageable).map(InstructorExperienceFactory::toDTO);
    }

    private void updateExperienceFields(InstructorExperience existingExperience, InstructorExperienceDTO dto) {
        if (dto.instructorUuid() != null) {
            UUID previousInstructorUuid = existingExperience.getInstructorUuid();
            existingExperience.setInstructorUuid(dto.instructorUuid());
            if (previousInstructorUuid != null && !previousInstructorUuid.equals(dto.instructorUuid())) {
                // The entity trigger only sees the new owner; the old owner's search document still lists this row.
                searchIndexRequests.enqueue(InstructorSearchSource.INDEX, previousInstructorUuid);
            }
        }
        if (dto.position() != null) {
            existingExperience.setPosition(dto.position());
        }
        if (dto.organizationName() != null) {
            existingExperience.setOrganizationName(dto.organizationName());
        }
        if (dto.responsibilities() != null) {
            existingExperience.setResponsibilities(dto.responsibilities());
        }
        if (dto.yearsOfExperience() != null) {
            existingExperience.setYearsOfExperience(dto.yearsOfExperience());
        }
        if (dto.startDate() != null) {
            existingExperience.setStartDate(dto.startDate());
        }
        if (dto.endDate() != null) {
            existingExperience.setEndDate(dto.endDate());
        }
        if (dto.isCurrentPosition() != null) {
            existingExperience.setIsCurrentPosition(dto.isCurrentPosition());
        }
    }
}