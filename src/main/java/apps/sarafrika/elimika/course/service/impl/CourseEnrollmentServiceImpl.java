package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO;
import apps.sarafrika.elimika.course.factory.CourseEnrollmentFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseEnrollmentService;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.service.AgeVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final CourseRepository courseRepository;
    private final GenericSpecificationBuilder<CourseEnrollment> specificationBuilder;
    private final AgeVerificationService ageVerificationService;

    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Course enrollment with ID %s not found";

    @Override
    public CourseEnrollmentDTO createCourseEnrollment(CourseEnrollmentDTO courseEnrollmentDTO) {
        Course course = getCourseOrThrow(courseEnrollmentDTO.courseUuid());
        enforceCourseApproval(course);
        enforceAgeLimits(courseEnrollmentDTO.studentUuid(), course);

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
        Course course = getCourseOrThrow(existingEnrollment.getCourseUuid());
        enforceCourseApproval(course);
        enforceAgeLimits(existingEnrollment.getStudentUuid(), course);

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

    @Override
    public boolean existsByStudentUuidAndCourseUuid(UUID studentUuid, UUID courseUuid) {
        return courseEnrollmentRepository.existsByStudentUuidAndCourseUuid(studentUuid, courseUuid);
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

    private Course getCourseOrThrow(UUID courseUuid) {
        if (courseUuid == null) {
            throw new IllegalArgumentException("Course UUID is required for enrollment");
        }
        return courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Course with UUID %s not found", courseUuid)));
    }

    private void enforceAgeLimits(UUID studentUuid, Course course) {
        Integer minAge = course.getAgeLowerLimit();
        Integer maxAge = course.getAgeUpperLimit();
        if (minAge == null && maxAge == null) {
            return;
        }
        ageVerificationService.verifyStudentAge(
                studentUuid,
                minAge,
                maxAge,
                describeCourse(course)
        );
    }

    private void enforceCourseApproval(Course course) {
        if (course == null) {
            return;
        }
        if (!Boolean.TRUE.equals(course.getAdminApproved())) {
            throw new IllegalStateException(describeCourse(course) + " is pending admin approval and cannot accept enrollments.");
        }
    }

    private String describeCourse(Course course) {
        if (course == null) {
            return "the selected course";
        }
        if (StringUtils.hasText(course.getName())) {
            return "course \"" + course.getName().trim() + "\"";
        }
        return "course " + course.getUuid();
    }
}
