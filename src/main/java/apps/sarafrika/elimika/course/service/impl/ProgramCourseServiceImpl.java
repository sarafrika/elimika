package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.ProgramCourseDTO;
import apps.sarafrika.elimika.course.dto.ProgramCourseWeightDTO;
import apps.sarafrika.elimika.course.internal.ProgramWeightsChangedEvent;
import apps.sarafrika.elimika.course.factory.ProgramCourseFactory;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.service.ProgramCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramCourseServiceImpl implements ProgramCourseService {

    private final ProgramCourseRepository programCourseRepository;
    private final GenericSpecificationBuilder<ProgramCourse> specificationBuilder;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal FULL_WEIGHT = new BigDecimal("100");

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
        specificationBuilder.validateSortProperties(ProgramCourse.class, pageable);
        return programCourseRepository.findAll(pageable).map(ProgramCourseFactory::toDTO);
    }

    @Override
    public ProgramCourseDTO updateProgramCourse(UUID uuid, ProgramCourseDTO programCourseDTO) {
        ProgramCourse existingProgramCourse = programCourseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_COURSE_NOT_FOUND_TEMPLATE, uuid)));

        updateProgramCourseFields(existingProgramCourse, programCourseDTO);

        ProgramCourse updatedProgramCourse = programCourseRepository.save(existingProgramCourse);
        eventPublisher.publishEvent(new ProgramWeightsChangedEvent(updatedProgramCourse.getProgramUuid()));
        return ProgramCourseFactory.toDTO(updatedProgramCourse);
    }

    @Override
    public void deleteProgramCourse(UUID uuid) {
        if (!programCourseRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(PROGRAM_COURSE_NOT_FOUND_TEMPLATE, uuid));
        }
        UUID programUuid = programCourseRepository.findByUuid(uuid).map(ProgramCourse::getProgramUuid).orElse(null);
        programCourseRepository.deleteByUuid(uuid);
        if (programUuid != null) {
            eventPublisher.publishEvent(new ProgramWeightsChangedEvent(programUuid));
        }
    }

    @Override
    public List<ProgramCourseDTO> updateCourseWeights(UUID programUuid, List<ProgramCourseWeightDTO> weights) {
        List<ProgramCourse> courses = programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid);
        Map<UUID, ProgramCourseWeightDTO> byUuid = new HashMap<>();
        for (ProgramCourseWeightDTO weight : weights) {
            if (byUuid.put(weight.programCourseUuid(), weight) != null) {
                throw new IllegalArgumentException("Each programme course may appear only once");
            }
        }
        Set<UUID> expected = new HashSet<>(courses.stream().map(ProgramCourse::getUuid).toList());
        if (!expected.equals(byUuid.keySet())) {
            throw new IllegalArgumentException("Send a weight row for every course in the programme, and only those courses");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ProgramCourse course : courses) {
            ProgramCourseWeightDTO weight = byUuid.get(course.getUuid());
            boolean required = Boolean.TRUE.equals(weight.isRequired());
            BigDecimal value = weight.weightPercentage();
            if (required && (value == null || value.signum() <= 0)) {
                throw new IllegalArgumentException("Every mandatory course needs a weight above 0%");
            }
            if (!required && value != null && value.signum() != 0) {
                throw new IllegalArgumentException("Optional courses don't count toward the programme mark, so they can't carry a weight");
            }
            course.setIsRequired(required);
            course.setWeightPercentage(required ? value : null);
            total = required ? total.add(value) : total;
        }
        if (total.compareTo(FULL_WEIGHT) != 0) {
            throw new IllegalArgumentException("Mandatory course weights must total exactly 100% (currently " + total.stripTrailingZeros().toPlainString() + "%)");
        }
        List<ProgramCourseDTO> saved = programCourseRepository.saveAll(courses).stream().map(ProgramCourseFactory::toDTO).toList();
        eventPublisher.publishEvent(new ProgramWeightsChangedEvent(programUuid));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceCompleteWeights(UUID programUuid) {
        List<ProgramCourse> required = programCourseRepository.findByProgramUuidAndIsRequiredTrue(programUuid);
        if (required.isEmpty()) {
            return;
        }
        boolean missing = required.stream().anyMatch(course -> course.getWeightPercentage() == null);
        BigDecimal total = required.stream().map(ProgramCourse::getWeightPercentage)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (missing || total.compareTo(FULL_WEIGHT) != 0) {
            throw new IllegalStateException("Set the programme's course weights so mandatory courses total exactly 100% before publishing");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramCourseDTO> search(Map<String, String> searchParams, Pageable pageable) {
        specificationBuilder.validateSortProperties(ProgramCourse.class, pageable);
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
            if (!dto.isRequired()) {
                existingProgramCourse.setWeightPercentage(null);
            }
        }
        if (dto.prerequisiteCourseUuid() != null) {
            existingProgramCourse.setPrerequisiteCourseUuid(dto.prerequisiteCourseUuid());
        }
    }
}