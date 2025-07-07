package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.dto.TrainingProgramDTO;
import apps.sarafrika.elimika.course.factory.CourseFactory;
import apps.sarafrika.elimika.course.factory.TrainingProgramFactory;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.TrainingProgramService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainingProgramServiceImpl implements TrainingProgramService {

    private final TrainingProgramRepository trainingProgramRepository;
    private final GenericSpecificationBuilder<TrainingProgram> specificationBuilder;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final CourseRepository courseRepository;

    private static final String PROGRAM_NOT_FOUND_TEMPLATE = "Training program with ID %s not found";

    @Override
    public TrainingProgramDTO createTrainingProgram(TrainingProgramDTO trainingProgramDTO) {
        TrainingProgram program = TrainingProgramFactory.toEntity(trainingProgramDTO);

        // Set defaults based on TrainingProgramDTO business logic
        if (program.getActive() == null) {
            program.setActive(false);
        }

        TrainingProgram savedProgram = trainingProgramRepository.save(program);
        return TrainingProgramFactory.toDTO(savedProgram);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingProgramDTO getTrainingProgramByUuid(UUID uuid) {
        return trainingProgramRepository.findByUuid(uuid)
                .map(TrainingProgramFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingProgramDTO> getAllTrainingPrograms(Pageable pageable) {
        return trainingProgramRepository.findAll(pageable).map(TrainingProgramFactory::toDTO);
    }

    @Override
    public TrainingProgramDTO updateTrainingProgram(UUID uuid, TrainingProgramDTO trainingProgramDTO) {
        TrainingProgram existingProgram = trainingProgramRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, uuid)));

        updateProgramFields(existingProgram, trainingProgramDTO);

        TrainingProgram updatedProgram = trainingProgramRepository.save(existingProgram);
        return TrainingProgramFactory.toDTO(updatedProgram);
    }

    @Override
    public void deleteTrainingProgram(UUID uuid) {
        if (!trainingProgramRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(PROGRAM_NOT_FOUND_TEMPLATE, uuid));
        }
        trainingProgramRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingProgramDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<TrainingProgram> spec = specificationBuilder.buildSpecification(
                TrainingProgram.class, searchParams);
        return trainingProgramRepository.findAll(spec, pageable).map(TrainingProgramFactory::toDTO);
    }

    // Domain-specific methods leveraging TrainingProgramDTO computed properties
    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getActivePrograms() {
        return trainingProgramRepository.findByActiveTrue()
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getProgramsByCategory(UUID categoryUuid) {
        return trainingProgramRepository.findByCategoryUuid(categoryUuid)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getProgramsByInstructor(UUID instructorUuid) {
        return trainingProgramRepository.findByInstructorUuid(instructorUuid)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getPublishedPrograms() {
        return trainingProgramRepository.findByStatus(ContentStatus.PUBLISHED)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public double getProgramCompletionRate(UUID programUuid) {
        long totalEnrollments = programEnrollmentRepository.countByProgramUuid(programUuid);
        long completedEnrollments = programEnrollmentRepository
                .countByProgramUuidAndStatus(programUuid, EnrollmentStatus.COMPLETED);

        return totalEnrollments > 0 ? (double) completedEnrollments / totalEnrollments * 100 : 0.0;
    }

    @Transactional(readOnly = true)
    public boolean isProgramComplete(UUID studentUuid, UUID programUuid) {
        return programEnrollmentRepository.existsByStudentUuidAndProgramUuidAndStatus(
                studentUuid, programUuid, EnrollmentStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getRequiredCourses(UUID programUuid) {
        return programCourseRepository.findByProgramUuidAndIsRequiredTrue(programUuid)
                .stream()
                .map(pc -> courseRepository.findByUuid(pc.getCourseUuid()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getOptionalCourses(UUID programUuid) {
        return programCourseRepository.findByProgramUuidAndIsRequiredFalse(programUuid)
                .stream()
                .map(pc -> courseRepository.findByUuid(pc.getCourseUuid()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getAllProgramCourses(UUID programUuid) {
        return programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid)
                .stream()
                .map(pc -> courseRepository.findByUuid(pc.getCourseUuid()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    // Leveraging TrainingProgramDTO computed properties for analytics
    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getProgramsByType(String programType) {
        return trainingProgramRepository.findAll()
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .filter(program -> programType.equals(program.getProgramType()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getFreePrograms() {
        return trainingProgramRepository.findByPriceIsNullOrPrice(BigDecimal.ZERO)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .filter(TrainingProgramDTO::isFree) // Using computed property
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getExtendedPrograms() {
        // Programs with 100+ hours (using computed property logic)
        return trainingProgramRepository.findByTotalDurationHoursGreaterThanEqual(100)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .filter(program -> "Extended Program".equals(program.getProgramType()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getIntensivePrograms() {
        // Programs with 50-99 hours
        return trainingProgramRepository.findByTotalDurationHoursBetween(50, 99)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .filter(program -> "Intensive Program".equals(program.getProgramType()))
                .collect(Collectors.toList());
    }

    public TrainingProgramDTO publishProgram(UUID programUuid) {
        TrainingProgram program = trainingProgramRepository.findByUuid(programUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, programUuid)));

        program.setActive(true);

        TrainingProgram updatedProgram = trainingProgramRepository.save(program);
        return TrainingProgramFactory.toDTO(updatedProgram);
    }

    @Transactional(readOnly = true)
    public boolean isProgramReadyForPublishing(UUID programUuid) {
        TrainingProgramDTO program = getTrainingProgramByUuid(programUuid);

        // Business logic: Program needs courses and basic info
        return program.title() != null &&
                program.description() != null &&
                programCourseRepository.countByProgramUuid(programUuid) > 0;
    }

    @Transactional(readOnly = true)
    public int getTotalProgramCourses(UUID programUuid) {
        return (int) programCourseRepository.countByProgramUuid(programUuid);
    }

    @Transactional(readOnly = true)
    public int getTotalRequiredCourses(UUID programUuid) {
        return (int) programCourseRepository.countByProgramUuidAndIsRequiredTrue(programUuid);
    }

    private void updateProgramFields(TrainingProgram existingProgram, TrainingProgramDTO dto) {
        if (dto.title() != null) {
            existingProgram.setTitle(dto.title());
        }
        if (dto.instructorUuid() != null) {
            existingProgram.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.categoryUuid() != null) {
            existingProgram.setCategoryUuid(dto.categoryUuid());
        }
        if (dto.description() != null) {
            existingProgram.setDescription(dto.description());
        }
        if (dto.objectives() != null) {
            existingProgram.setObjectives(dto.objectives());
        }
        if (dto.prerequisites() != null) {
            existingProgram.setPrerequisites(dto.prerequisites());
        }
        if (dto.totalDurationHours() != null) {
            existingProgram.setTotalDurationHours(dto.totalDurationHours());
        }
        if (dto.totalDurationMinutes() != null) {
            existingProgram.setTotalDurationMinutes(dto.totalDurationMinutes());
        }
        if (dto.classLimit() != null) {
            existingProgram.setClassLimit(dto.classLimit());
        }
        if (dto.price() != null) {
            existingProgram.setPrice(dto.price());
        }
        if (dto.active() != null) {
            existingProgram.setActive(dto.active());
        }
    }
}