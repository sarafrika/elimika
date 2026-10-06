package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.ProgramAssessmentDTO;
import apps.sarafrika.elimika.course.model.ProgramAssessment;
import apps.sarafrika.elimika.course.repository.AssessmentRubricRepository;
import apps.sarafrika.elimika.course.repository.ProgramAssessmentRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.ProgramAssessmentService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramAssessmentServiceImpl implements ProgramAssessmentService {

    private static final BigDecimal FULL_WEIGHT = new BigDecimal("100");

    private final ProgramAssessmentRepository assessmentRepository;
    private final TrainingProgramRepository programRepository;
    private final AssessmentRubricRepository rubricRepository;

    @Override
    public ProgramAssessmentDTO create(UUID programUuid, ProgramAssessmentDTO dto) {
        if (!programRepository.existsByUuid(programUuid)) {
            throw new ResourceNotFoundException("Training program not found for UUID: " + programUuid);
        }
        ProgramAssessment assessment = new ProgramAssessment();
        assessment.setProgramUuid(programUuid);
        apply(assessment, dto);
        validate(programUuid, null, assessment);
        return toDTO(assessmentRepository.save(assessment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramAssessmentDTO> list(UUID programUuid) {
        return assessmentRepository.findByProgramUuidOrderByCreatedDateAsc(programUuid).stream()
                .map(ProgramAssessmentServiceImpl::toDTO).toList();
    }

    @Override
    public ProgramAssessmentDTO update(UUID programUuid, UUID assessmentUuid, ProgramAssessmentDTO dto) {
        ProgramAssessment assessment = find(programUuid, assessmentUuid);
        apply(assessment, dto);
        validate(programUuid, assessmentUuid, assessment);
        return toDTO(assessmentRepository.save(assessment));
    }

    @Override
    public void delete(UUID programUuid, UUID assessmentUuid) {
        // Course components keep their scores; their link to this component is cleared by the foreign key.
        assessmentRepository.delete(find(programUuid, assessmentUuid));
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceFullWeight(UUID programUuid) {
        List<ProgramAssessment> active = activeComponents(programUuid);
        if (active.isEmpty()) {
            return;
        }
        BigDecimal total = active.stream().map(ProgramAssessment::getWeightPercentage).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(FULL_WEIGHT) != 0) {
            throw new IllegalStateException("Program assessment weights must add up to 100% before publishing; they add up to "
                    + total.stripTrailingZeros().toPlainString() + "%.");
        }
    }

    private void validate(UUID programUuid, UUID currentUuid, ProgramAssessment assessment) {
        if (assessment.getRubricUuid() != null && !rubricRepository.existsByUuid(assessment.getRubricUuid())) {
            throw new ResourceNotFoundException("Assessment rubric not found for UUID: " + assessment.getRubricUuid());
        }
        if (Boolean.FALSE.equals(assessment.getActive())) {
            return;
        }
        BigDecimal others = activeComponents(programUuid).stream()
                .filter(other -> !other.getUuid().equals(currentUuid))
                .map(ProgramAssessment::getWeightPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (others.add(assessment.getWeightPercentage()).compareTo(FULL_WEIGHT) > 0) {
            throw new IllegalArgumentException("Program assessment weights cannot exceed 100%");
        }
    }

    private List<ProgramAssessment> activeComponents(UUID programUuid) {
        return assessmentRepository.findByProgramUuidOrderByCreatedDateAsc(programUuid).stream()
                .filter(assessment -> !Boolean.FALSE.equals(assessment.getActive()))
                .toList();
    }

    private ProgramAssessment find(UUID programUuid, UUID assessmentUuid) {
        return assessmentRepository.findByUuidAndProgramUuid(assessmentUuid, programUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Program assessment not found for UUID: " + assessmentUuid));
    }

    private static void apply(ProgramAssessment assessment, ProgramAssessmentDTO dto) {
        assessment.setTitle(dto.title());
        assessment.setAssessmentType(dto.assessmentType());
        assessment.setDescription(dto.description());
        assessment.setWeightPercentage(dto.weightPercentage());
        assessment.setRubricUuid(dto.rubricUuid());
        assessment.setIsRequired(dto.isRequired() == null ? Boolean.TRUE : dto.isRequired());
        assessment.setActive(dto.active() == null ? Boolean.TRUE : dto.active());
    }

    static ProgramAssessmentDTO toDTO(ProgramAssessment assessment) {
        return new ProgramAssessmentDTO(assessment.getUuid(), assessment.getProgramUuid(), assessment.getTitle(),
                assessment.getAssessmentType(), assessment.getDescription(), assessment.getWeightPercentage(),
                assessment.getRubricUuid(), assessment.getIsRequired(), assessment.getActive(), assessment.getCreatedDate());
    }
}
