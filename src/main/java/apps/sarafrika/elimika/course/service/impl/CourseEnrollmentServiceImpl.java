package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO;
import apps.sarafrika.elimika.course.factory.CourseEnrollmentFactory;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.service.CourseEnrollmentService;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseEnrollmentServiceImpl implements CourseEnrollmentService {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final GenericSpecificationBuilder<CourseEnrollment> specificationBuilder;

    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Course enrollment with ID %s not found";

    @Override
    public CourseEnrollmentDTO createCourseEnrollment(CourseEnrollmentDTO courseEnrollmentDTO) {
        CourseEnrollment enrollment = CourseEnrollmentFactory.toEntity(courseEnrollmentDTO);

        // Set defaults based on CourseEnrollmentDTO business logic
        if (enrollment.getEnrollmentDate() == null) {
            enrollment.setEnrollmentDate(LocalDateTime.now());
        }
        if (enrollment.getStatus() == null) {
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
        }
        if (enrollment.getProgressPercentage() == null) {
            enrollment.setProgressPercentage(BigDecimal.ZERO);
        }

        CourseEnrollment savedEnrollment = courseEnrollmentRepository.save(enrollment);
        return CourseEnrollmentFactory.toDTO(savedEnrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseEnrollmentDTO getCourseEnrollmentByUuid(UUID uuid) {
        return courseEnrollmentRepository.findByUuid(uuid)
                .map(CourseEnrollmentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseEnrollmentDTO> getAllCourseEnrollments(Pageable pageable) {
        return courseEnrollmentRepository.findAll(pageable).map(CourseEnrollmentFactory::toDTO);
    }

    @Override
    public CourseEnrollmentDTO updateCourseEnrollment(UUID uuid, CourseEnrollmentDTO courseEnrollmentDTO) {
        CourseEnrollment existingEnrollment = courseEnrollmentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, uuid)));

        updateEnrollmentFields(existingEnrollment, courseEnrollmentDTO);

        CourseEnrollment updatedEnrollment = courseEnrollmentRepository.save(existingEnrollment);
        return CourseEnrollmentFactory.toDTO(updatedEnrollment);
    }

    @Override
    public void deleteCourseEnrollment(UUID uuid) {
        if (!courseEnrollmentRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, uuid));
        }
        courseEnrollmentRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseEnrollmentDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseEnrollment> spec = specificationBuilder.buildSpecification(
                CourseEnrollment.class, searchParams);
        return courseEnrollmentRepository.findAll(spec, pageable).map(CourseEnrollmentFactory::toDTO);
    }

    @Override
    public boolean existsByCourseUuidAndStatusIn(UUID uuid, List<EnrollmentStatus> statuses) {
        return courseEnrollmentRepository.existsByCourseUuidAndStatusIn(uuid, statuses);
    }

    private void updateEnrollmentFields(CourseEnrollment existingEnrollment, CourseEnrollmentDTO dto) {
        if (dto.studentUuid() != null) {
            existingEnrollment.setStudentUuid(dto.studentUuid());
        }
        if (dto.courseUuid() != null) {
            existingEnrollment.setCourseUuid(dto.courseUuid());
        }
        if (dto.enrollmentDate() != null) {
            existingEnrollment.setEnrollmentDate(dto.enrollmentDate());
        }
        if (dto.completionDate() != null) {
            existingEnrollment.setCompletionDate(dto.completionDate());
        }
        if (dto.status() != null) {
            existingEnrollment.setStatus(dto.status());
        }
        if (dto.progressPercentage() != null) {
            existingEnrollment.setProgressPercentage(dto.progressPercentage());
        }
        if (dto.finalGrade() != null) {
            existingEnrollment.setFinalGrade(dto.finalGrade());
        }
    }
}