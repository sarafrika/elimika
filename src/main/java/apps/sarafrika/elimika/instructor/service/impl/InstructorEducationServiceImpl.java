package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.instructor.dto.InstructorEducationDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorEducationFactory;
import apps.sarafrika.elimika.instructor.model.InstructorEducation;
import apps.sarafrika.elimika.instructor.repository.InstructorEducationRepository;
import apps.sarafrika.elimika.instructor.service.InstructorEducationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorEducationServiceImpl implements InstructorEducationService {

    private final InstructorEducationRepository instructorEducationRepository;
    private final GenericSpecificationBuilder<InstructorEducation> specificationBuilder;

    private static final String INSTRUCTOR_EDUCATION_NOT_FOUND_TEMPLATE = "Instructor education with ID %s not found";

    @Override
    public InstructorEducationDTO createInstructorEducation(InstructorEducationDTO instructorEducationDTO) {
        InstructorEducation instructorEducation = InstructorEducationFactory.toEntity(instructorEducationDTO);
        instructorEducation.setCreatedDate(LocalDateTime.now());

        InstructorEducation savedEducation = instructorEducationRepository.save(instructorEducation);
        return InstructorEducationFactory.toDTO(savedEducation);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorEducationDTO getInstructorEducationByUuid(UUID uuid) {
        return instructorEducationRepository.findByUuid(uuid)
                .map(InstructorEducationFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_EDUCATION_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorEducationDTO> getAllInstructorEducation(Pageable pageable) {
        return instructorEducationRepository.findAll(pageable).map(InstructorEducationFactory::toDTO);
    }

    @Override
    public InstructorEducationDTO updateInstructorEducation(UUID uuid, InstructorEducationDTO instructorEducationDTO) {
        InstructorEducation existingEducation = instructorEducationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_EDUCATION_NOT_FOUND_TEMPLATE, uuid)));

        // Update fields from DTO
        updateEducationFields(existingEducation, instructorEducationDTO);

        InstructorEducation updatedEducation = instructorEducationRepository.save(existingEducation);
        return InstructorEducationFactory.toDTO(updatedEducation);
    }

    @Override
    public void deleteInstructorEducation(UUID uuid) {
        if (!instructorEducationRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(INSTRUCTOR_EDUCATION_NOT_FOUND_TEMPLATE, uuid));
        }
        instructorEducationRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorEducationDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<InstructorEducation> spec = specificationBuilder.buildSpecification(InstructorEducation.class, searchParams);
        return instructorEducationRepository.findAll(spec, pageable).map(InstructorEducationFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorEducationDTO> getEducationByInstructorUuid(UUID instructorUuid) {
        return instructorEducationRepository.findByInstructorUuid(instructorUuid)
                .stream()
                .map(InstructorEducationFactory::toDTO)
                .collect(Collectors.toList());
    }

    private void updateEducationFields(InstructorEducation existingEducation, InstructorEducationDTO dto) {
        if (dto.instructorUuid() != null) {
            existingEducation.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.qualification() != null) {
            existingEducation.setQualification(dto.qualification());
        }
        if (dto.schoolName() != null) {
            existingEducation.setSchoolName(dto.schoolName());
        }
        if (dto.yearCompleted() != null) {
            existingEducation.setYearCompleted(dto.yearCompleted());
        }
        if (dto.certificateNumber() != null) {
            existingEducation.setCertificateNumber(dto.certificateNumber());
        }
    }
}