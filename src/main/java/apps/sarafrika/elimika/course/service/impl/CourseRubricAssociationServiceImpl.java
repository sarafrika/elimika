package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.course.dto.CourseRubricAssociationDTO;
import apps.sarafrika.elimika.course.factory.CourseRubricAssociationFactory;
import apps.sarafrika.elimika.course.model.CourseRubricAssociation;
import apps.sarafrika.elimika.course.repository.AssessmentRubricRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseRubricAssociationRepository;
import apps.sarafrika.elimika.course.service.CourseRubricAssociationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseRubricAssociationServiceImpl implements CourseRubricAssociationService {

    private final CourseRubricAssociationRepository associationRepository;
    private final CourseRepository courseRepository;
    private final AssessmentRubricRepository rubricRepository;

    private static final String COURSE_NOT_FOUND_TEMPLATE = "Course with ID %s not found";
    private static final String RUBRIC_NOT_FOUND_TEMPLATE = "Assessment rubric with ID %s not found";
    private static final String ASSOCIATION_NOT_FOUND_TEMPLATE = "Association with ID %s not found";
    private static final String ASSOCIATION_BY_COURSE_RUBRIC_NOT_FOUND_TEMPLATE = "Association for course %s and rubric %s not found";
    private static final String ASSOCIATION_BY_CONTEXT_NOT_FOUND_TEMPLATE = "Association for course %s, rubric %s, and context '%s' not found";
    private static final String DUPLICATE_ASSOCIATION_TEMPLATE = "Rubric %s is already associated with course %s";

    @Override
    public CourseRubricAssociationDTO associateRubricWithCourse(CourseRubricAssociationDTO dto) {
        validateCourseAndRubricExist(dto.courseUuid(), dto.rubricUuid());

        if (associationRepository.existsByCourseUuidAndRubricUuidAndUsageContext(dto.courseUuid(), dto.rubricUuid(), dto.usageContext())) {
            throw new DuplicateResourceException(
                    String.format("Rubric %s is already associated with course %s for the context '%s'",
                            dto.rubricUuid(), dto.courseUuid(), dto.usageContext()));
        }

        CourseRubricAssociation association = CourseRubricAssociationFactory.toEntity(dto);
        association.setAssociationDate(java.time.LocalDateTime.now());

        if (dto.isPrimaryRubric()) {
            setPrimaryRubric(dto.courseUuid(), dto.rubricUuid(), dto.associatedBy());
        }

        CourseRubricAssociation savedAssociation = associationRepository.save(association);
        return CourseRubricAssociationFactory.toDTO(savedAssociation);
    }

    @Override
    public void dissociateRubricFromCourse(UUID courseUuid, UUID rubricUuid) {
        CourseRubricAssociation association = associationRepository.findByCourseUuidAndRubricUuid(courseUuid, rubricUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSOCIATION_BY_COURSE_RUBRIC_NOT_FOUND_TEMPLATE, courseUuid, rubricUuid)));
        associationRepository.delete(association);
    }

    @Override
    public void dissociateRubricFromCourseByContext(UUID courseUuid, UUID rubricUuid, String usageContext) {
        CourseRubricAssociation association = associationRepository.findByCourseUuidAndRubricUuidAndUsageContext(courseUuid, rubricUuid, usageContext)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSOCIATION_BY_CONTEXT_NOT_FOUND_TEMPLATE, courseUuid, rubricUuid, usageContext)));
        associationRepository.delete(association);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseRubricAssociationDTO> getRubricsByCourse(UUID courseUuid, Pageable pageable) {
        validateCourseExists(courseUuid);
        List<CourseRubricAssociation> associations = associationRepository.findByCourseUuid(courseUuid);
        return new PageImpl<>(
                associations.stream().map(CourseRubricAssociationFactory::toDTO).collect(Collectors.toList()),
                pageable,
                associations.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseRubricAssociationDTO> getCoursesByRubric(UUID rubricUuid, Pageable pageable) {
        validateRubricExists(rubricUuid);
        List<CourseRubricAssociation> associations = associationRepository.findByRubricUuid(rubricUuid);
        return new PageImpl<>(
                associations.stream().map(CourseRubricAssociationFactory::toDTO).collect(Collectors.toList()),
                pageable,
                associations.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CourseRubricAssociationDTO getPrimaryRubricForCourse(UUID courseUuid) {
        validateCourseExists(courseUuid);
        return associationRepository.findByCourseUuidAndIsPrimaryRubricTrue(courseUuid)
                .map(CourseRubricAssociationFactory::toDTO)
                .orElse(null);
    }

    @Override
    public CourseRubricAssociationDTO setPrimaryRubric(UUID courseUuid, UUID rubricUuid, UUID instructorUuid) {
        validateCourseAndRubricExist(courseUuid, rubricUuid);

        // Find and unset the current primary rubric for the course
        associationRepository.findByCourseUuidAndIsPrimaryRubricTrue(courseUuid)
                .ifPresent(primary -> {
                    primary.setIsPrimaryRubric(false);
                    associationRepository.save(primary);
                });

        // Find or create the new primary association
        CourseRubricAssociation association = associationRepository
                .findByCourseUuidAndRubricUuid(courseUuid, rubricUuid)
                .orElseGet(() -> {
                    CourseRubricAssociation newAssociation = new CourseRubricAssociation();
                    newAssociation.setCourseUuid(courseUuid);
                    newAssociation.setRubricUuid(rubricUuid);
                    newAssociation.setAssociatedBy(instructorUuid);
                    newAssociation.setAssociationDate(java.time.LocalDateTime.now());
                    return newAssociation;
                });

        association.setIsPrimaryRubric(true);
        CourseRubricAssociation updatedAssociation = associationRepository.save(association);
        return CourseRubricAssociationFactory.toDTO(updatedAssociation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseRubricAssociationDTO> getAssociationsByContext(UUID courseUuid, String usageContext, Pageable pageable) {
        validateCourseExists(courseUuid);
        List<CourseRubricAssociation> associations = associationRepository.findByCourseUuid(courseUuid)
                .stream()
                .filter(a -> usageContext.equalsIgnoreCase(a.getUsageContext()))
                .collect(Collectors.toList());
        return new PageImpl<>(
                associations.stream().map(CourseRubricAssociationFactory::toDTO).collect(Collectors.toList()),
                pageable,
                associations.size()
        );
    }

    @Override
    public CourseRubricAssociationDTO updateAssociation(UUID associationUuid, CourseRubricAssociationDTO dto) {
        CourseRubricAssociation existingAssociation = associationRepository.findByUuid(associationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSOCIATION_NOT_FOUND_TEMPLATE, associationUuid)));

        CourseRubricAssociationFactory.updateEntityFromDTO(existingAssociation, dto);

        if (dto.isPrimaryRubric()) {
            setPrimaryRubric(dto.courseUuid(), dto.rubricUuid(), dto.associatedBy());
        }

        CourseRubricAssociation updatedAssociation = associationRepository.save(existingAssociation);
        return CourseRubricAssociationFactory.toDTO(updatedAssociation);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseRubricAssociationDTO getAssociationByUuid(UUID associationUuid) {
        return associationRepository.findByUuid(associationUuid)
                .map(CourseRubricAssociationFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSOCIATION_NOT_FOUND_TEMPLATE, associationUuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRubricAssociatedWithCourse(UUID courseUuid, UUID rubricUuid) {
        return associationRepository.existsByCourseUuidAndRubricUuid(courseUuid, rubricUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public long getRubricUsageCount(UUID rubricUuid) {
        return associationRepository.countCoursesByRubricUuid(rubricUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseRubricAssociationDTO> getAssociationsByInstructor(UUID instructorUuid, Pageable pageable) {
        List<CourseRubricAssociation> associations = associationRepository.findByAssociatedBy(instructorUuid);
        return new PageImpl<>(
                associations.stream().map(CourseRubricAssociationFactory::toDTO).collect(Collectors.toList()),
                pageable,
                associations.size()
        );
    }

    private void validateCourseAndRubricExist(UUID courseUuid, UUID rubricUuid) {
        validateCourseExists(courseUuid);
        validateRubricExists(rubricUuid);
    }

    private void validateCourseExists(UUID courseUuid) {
        if (!courseRepository.existsByUuid(courseUuid)) {
            throw new ResourceNotFoundException(String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid));
        }
    }

    private void validateRubricExists(UUID rubricUuid) {
        if (!rubricRepository.existsByUuid(rubricUuid)) {
            throw new ResourceNotFoundException(String.format(RUBRIC_NOT_FOUND_TEMPLATE, rubricUuid));
        }
    }
}
