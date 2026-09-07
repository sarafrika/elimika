package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentLookupService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of Enrollment Lookup Service
 * <p>
 * Provides read-only access to enrollment information.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentLookupServiceImpl implements EnrollmentLookupService {

    private final EnrollmentRepository enrollmentRepository;

    @Override
    public boolean enrollmentExists(UUID enrollmentUuid) {
        return enrollmentRepository.existsByUuid(enrollmentUuid);
    }

    @Override
    public Optional<UUID> getEnrollmentStudentUuid(UUID enrollmentUuid) {
        return enrollmentRepository.findByUuid(enrollmentUuid)
                .map(Enrollment::getStudentUuid);
    }

    @Override
    public Optional<UUID> getEnrollmentScheduledInstanceUuid(UUID enrollmentUuid) {
        return enrollmentRepository.findByUuid(enrollmentUuid)
                .map(Enrollment::getScheduledInstanceUuid);
    }

    @Override
    public boolean isStudentEnrolledInInstance(UUID studentUuid, UUID scheduledInstanceUuid) {
        return enrollmentRepository.existsByScheduledInstanceUuidAndStudentUuid(
                scheduledInstanceUuid, studentUuid);
    }

    @Override
    public Optional<ClassEnrollmentStatusSnapshot> findMostRecentEnrollmentForCourse(UUID studentUuid, UUID courseUuid) {
        if (studentUuid == null || courseUuid == null) {
            return Optional.empty();
        }
        return enrollmentRepository.findLatestByStudentAndCourseUuid(studentUuid, courseUuid)
                .map(this::toSnapshot);
    }

    @Override
    public Optional<ClassEnrollmentStatusSnapshot> findMostRecentEnrollmentForClassDefinition(UUID studentUuid,
                                                                                              UUID classDefinitionUuid) {
        if (studentUuid == null || classDefinitionUuid == null) {
            return Optional.empty();
        }
        return enrollmentRepository.findLatestByStudentAndClassDefinitionUuid(studentUuid, classDefinitionUuid)
                .map(this::toSnapshot);
    }

    @Override
    public Optional<ClassEnrollmentStatusSnapshot> findMostRecentActiveEnrollmentForCourse(UUID studentUuid, UUID courseUuid) {
        if (studentUuid == null || courseUuid == null) {
            return Optional.empty();
        }
        return enrollmentRepository.findLatestActiveByStudentAndCourseUuid(studentUuid, courseUuid)
                .map(this::toSnapshot);
    }

    @Override
    public long countDistinctLearnersForClassDefinitions(Collection<UUID> classDefinitionUuids) {
        Collection<UUID> classes = sanitise(classDefinitionUuids);
        return classes.isEmpty() ? 0L
                : enrollmentRepository.countDistinctStudentsByClassDefinitionUuidIn(classes);
    }

    @Override
    public long countFilledSeatsForClassDefinitions(Collection<UUID> classDefinitionUuids) {
        Collection<UUID> classes = sanitise(classDefinitionUuids);
        if (classes.isEmpty()) {
            return 0L;
        }
        long filled = 0L;
        for (Object[] row : enrollmentRepository.countDistinctStudentsGroupedByClassDefinition(classes)) {
            if (row.length > 1 && row[1] instanceof Number learners) {
                filled += learners.longValue();
            }
        }
        return filled;
    }

    private static Collection<UUID> sanitise(Collection<UUID> classDefinitionUuids) {
        if (classDefinitionUuids == null || classDefinitionUuids.isEmpty()) {
            return List.of();
        }
        return classDefinitionUuids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private ClassEnrollmentStatusSnapshot toSnapshot(Enrollment enrollment) {
        LocalDateTime updatedAt = enrollment.getLastModifiedDate() != null
                ? enrollment.getLastModifiedDate()
                : enrollment.getCreatedDate();
        String status = enrollment.getStatus() != null ? enrollment.getStatus().getValue() : null;
        return new ClassEnrollmentStatusSnapshot(enrollment.getUuid(), status, updatedAt);
    }
}
