package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.ProgramCourseDTO;
import apps.sarafrika.elimika.course.factory.ProgramCourseFactory;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.service.ProgramCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramCourseServiceImpl implements ProgramCourseService {

    private final ProgramCourseRepository programCourseRepository;
    private final GenericSpecificationBuilder<ProgramCourse> specificationBuilder;

    private static final String PROGRAM_COURSE_NOT_FOUND_TEMPLATE = "Program course with ID %s not found";

    @Override
    public ProgramCourseDTO createProgramCourse(ProgramCourseDTO programCourseDTO) {
        ProgramCourse programCourse = ProgramCourseFactory.toEntity(programCourseDTO);

        // Set defaults
        if (programCourse.getIsRequired() == null) {
            programCourse.setIsRequired(true);
        }

        ProgramCourse savedProgramCourse = programCourseRepository.save(programCourse);
        return ProgramCourseFactory.toDTO(savedProgramCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramCourseDTO getProgramCourseByUuid(UUID uuid) {
        return programCourseRepository.findByUuid(uuid)
                .map(ProgramCourseFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_COURSE_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramCourseDTO> getAllProgramCourses(Pageable pageable) {
        return programCourseRepository.findAll(pageable).map(ProgramCourseFactory::toDTO);
    }

    @Override
    public ProgramCourseDTO updateProgramCourse(UUID uuid, ProgramCourseDTO programCourseDTO) {
        ProgramCourse existingProgramCourse = programCourseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_COURSE_NOT_FOUND_TEMPLATE, uuid)));

        updateProgramCourseFields(existingProgramCourse, programCourseDTO);

        ProgramCourse updatedProgramCourse = programCourseRepository.save(existingProgramCourse);
        return ProgramCourseFactory.toDTO(updatedProgramCourse);
    }

    @Override
    public void deleteProgramCourse(UUID uuid) {
        if (!programCourseRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(PROGRAM_COURSE_NOT_FOUND_TEMPLATE, uuid));
        }
        programCourseRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramCourseDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<ProgramCourse> spec = specificationBuilder.buildSpecification(
                ProgramCourse.class, searchParams);
        return programCourseRepository.findAll(spec, pageable).map(ProgramCourseFactory::toDTO);
    }

    private void updateProgramCourseFields(ProgramCourse existingProgramCourse, ProgramCourseDTO dto) {
        if (dto.programUuid() != null) {
            existingProgramCourse.setProgramUuid(dto.programUuid());
        }
        if (dto.courseUuid() != null) {
            existingProgramCourse.setCourseUuid(dto.courseUuid());
        }
        if (dto.sequenceOrder() != null) {
            existingProgramCourse.setSequenceOrder(dto.sequenceOrder());
        }
        if (dto.isRequired() != null) {
            existingProgramCourse.setIsRequired(dto.isRequired());
        }
        if (dto.prerequisiteCourseUuid() != null) {
            existingProgramCourse.setPrerequisiteCourseUuid(dto.prerequisiteCourseUuid());
        }
    }
}