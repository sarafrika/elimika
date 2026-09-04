package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.shared.security.ClassAccessSecurityService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Decides which enrolment rows each caller is entitled to, and answers the class-scoped questions
 * the enrolment routes are guarded by.
 * <p>
 * One rule covers every shape these rows arrive in: <em>you see your own enrolments, plus every
 * enrolment in a class you run</em>. Whoever runs the class gets the register whole — that is what
 * they teach and mark from, and {@link ClassAccessSecurityService#canManageClass(UUID)} says who
 * those people are. Everyone else is a learner, and a learner is party to their own seat and to
 * nobody else's.
 * <p>
 * Withholding a row rather than blanking its name matters, because a filter is a question. An
 * anonymous row still answers "is this student in this class?" to anyone who searched by that
 * student, and still answers "which sessions does she attend?" through the scheduled instance it
 * keeps. Removing the row answers nothing at all.
 * <p>
 * Filtering rather than refusing the request is what makes the rule uniform. A roster of one class,
 * a search spanning many, and a page of one learner's enrolments are all the same rows; a caller
 * who runs one class and merely attends another gets exactly what they hold in each, instead of a
 * request that succeeds or fails wholesale depending on which endpoint they reached for.
 * <p>
 * It sits in the timetabling SPI because the rows and their scheduled instances are timetabling's,
 * while the ownership question they hang on is the class's — the classes module reaches this through
 * the named interface it is already permitted to depend on.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
@Service("enrollmentVisibilityService")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentVisibilityService {

    private static final String CACHE_MANAGES_INSTANCE_PREFIX = "security.managesInstanceClass.";

    private final ScheduledInstanceRepository scheduledInstanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassAccessSecurityService classAccessSecurityService;
    private final DomainSecurityService domainSecurityService;
    private final RequestScopedCache requestScopedCache;

    /**
     * True when the caller runs the class this scheduled instance belongs to. Instructor block
     * entries carry no class definition and so belong to nobody but the platform admin, which costs
     * nothing to refuse: they hold no enrolments.
     *
     * @param instanceUuid the scheduled instance being read
     */
    @Transactional(readOnly = true)
    public boolean canManageClassOfInstance(UUID instanceUuid) {
        if (instanceUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_MANAGES_INSTANCE_PREFIX + instanceUuid, () -> {
            try {
                UUID classDefinitionUuid = scheduledInstanceRepository.findByUuid(instanceUuid)
                        .map(ScheduledInstance::getClassDefinitionUuid)
                        .orElse(null);
                return classAccessSecurityService.canManageClass(classDefinitionUuid);
            } catch (Exception e) {
                log.error("Error checking management rights over scheduled instance {}", instanceUuid, e);
                return false;
            }
        });
    }

    /**
     * True when the caller runs the class this enrolment sits in — the question to ask before
     * handing over, cancelling or marking an enrolment that is not the caller's own.
     *
     * @param enrollmentUuid the enrolment being acted on
     */
    @Transactional(readOnly = true)
    public boolean canManageClassOfEnrollment(UUID enrollmentUuid) {
        if (enrollmentUuid == null) {
            return false;
        }
        try {
            UUID instanceUuid = enrollmentRepository.findByUuid(enrollmentUuid)
                    .map(Enrollment::getScheduledInstanceUuid)
                    .orElse(null);
            return canManageClassOfInstance(instanceUuid);
        } catch (Exception e) {
            log.error("Error checking management rights over enrolment {}", enrollmentUuid, e);
            return false;
        }
    }

    /**
     * The roster of one named class as this caller is entitled to see it. Saves resolving each row's
     * class when the request already names it.
     *
     * @param classDefinitionUuid the class whose roster was requested
     * @param roster              the full roster as loaded from timetabling
     */
    public List<EnrollmentDTO> visibleToCaller(UUID classDefinitionUuid, List<EnrollmentDTO> roster) {
        if (roster == null || roster.isEmpty()) {
            return List.of();
        }
        if (classAccessSecurityService.canManageClass(classDefinitionUuid)) {
            return roster;
        }
        UUID callerStudentUuid = domainSecurityService.getCurrentStudentUuid();
        return roster.stream()
                .filter(enrollment -> isOwnEnrollment(enrollment, callerStudentUuid))
                .toList();
    }

    /**
     * A page of enrolments drawn from anywhere, filtered row by row — for the enrolment search,
     * where the caller chooses the filter and the rows may span classes they run, classes they only
     * attend, and classes they have nothing to do with.
     * <p>
     * The total is restated whenever rows were withheld, since a total counted over rows the caller
     * cannot see would report their existence just as plainly as returning them.
     */
    @Transactional(readOnly = true)
    public Page<EnrollmentDTO> visibleToCaller(Page<EnrollmentDTO> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) {
            return enrollments;
        }
        List<EnrollmentDTO> visible = visibleToCaller(enrollments.getContent());
        long total = visible.size() == enrollments.getContent().size()
                ? enrollments.getTotalElements()
                : visible.size();
        return new PageImpl<>(visible, enrollments.getPageable(), total);
    }

    /**
     * Enrolment rows drawn from anywhere, filtered row by row.
     * <p>
     * The classes behind the rows are resolved in one query rather than one per row, and each
     * class's answer is settled once, so a five-hundred-row page costs a handful of lookups.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentDTO> visibleToCaller(List<EnrollmentDTO> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) {
            return List.of();
        }
        if (domainSecurityService.isPlatformAdmin()) {
            return enrollments;
        }
        UUID callerStudentUuid = domainSecurityService.getCurrentStudentUuid();
        Map<UUID, UUID> classesByInstance = classesByInstance(enrollments);
        Map<UUID, Boolean> manageableClasses = new HashMap<>();
        return enrollments.stream()
                .filter(Objects::nonNull)
                .filter(enrollment -> isOwnEnrollment(enrollment, callerStudentUuid)
                        || runsClassOf(enrollment, classesByInstance, manageableClasses))
                .toList();
    }

    private boolean runsClassOf(EnrollmentDTO enrollment,
                                Map<UUID, UUID> classesByInstance,
                                Map<UUID, Boolean> manageableClasses) {
        UUID classDefinitionUuid = classesByInstance.get(enrollment.scheduledInstanceUuid());
        return classDefinitionUuid != null
                && manageableClasses.computeIfAbsent(
                        classDefinitionUuid, classAccessSecurityService::canManageClass);
    }

    private Map<UUID, UUID> classesByInstance(List<EnrollmentDTO> enrollments) {
        Set<UUID> instanceUuids = enrollments.stream()
                .filter(Objects::nonNull)
                .map(EnrollmentDTO::scheduledInstanceUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (instanceUuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> classesByInstance = new HashMap<>();
        try {
            for (ScheduledInstance instance : scheduledInstanceRepository.findByUuidIn(instanceUuids)) {
                if (instance.getUuid() != null && instance.getClassDefinitionUuid() != null) {
                    classesByInstance.put(instance.getUuid(), instance.getClassDefinitionUuid());
                }
            }
        } catch (Exception e) {
            // An instance that cannot be resolved is treated as one the caller does not run, so a
            // failure here withholds rows rather than releasing them.
            log.error("Error resolving the classes behind {} enrolment rows", enrollments.size(), e);
            return Map.of();
        }
        return classesByInstance;
    }

    private boolean isOwnEnrollment(EnrollmentDTO enrollment, UUID callerStudentUuid) {
        return enrollment != null
                && callerStudentUuid != null
                && callerStudentUuid.equals(enrollment.studentUuid());
    }
}
