package apps.sarafrika.elimika.timetabling.security;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Decides who may look at a class or session roster, at one enrolment, or at a named learner's
 * enrolment record.
 * <p>
 * A roster names the learners sitting in one organisation's class. Holding the {@code instructor}
 * or {@code admin} domain says only that the caller teaches or administers <em>something</em>,
 * somewhere on the platform — it says nothing about this session or this learner — so it cannot be
 * the rule here. The rule is a relationship to the class: the instructor scheduled on one of its
 * sessions, the class's default instructor, someone who runs the organisation that owns the class,
 * or a platform administrator doing support.
 * <p>
 * Every answer fails closed and is memoised for the life of the request, because a single request
 * commonly asks the same question at the route and again while assembling the response.
 */
@Service("timetableSecurityService")
@RequiredArgsConstructor
@Slf4j
public class TimetableSecurityService {

    private static final String CACHE_INSTANCE_PREFIX = "security.instanceRoster.";
    private static final String CACHE_CLASS_PREFIX = "security.classRoster.";
    private static final String CACHE_ENROLMENT_PREFIX = "security.enrolmentReach.";
    private static final String CACHE_LEARNER_PREFIX = "security.learnerRecord.";

    private final ScheduledInstanceRepository scheduledInstanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final InstructorLookupService instructorLookupService;
    private final DomainSecurityService domainSecurityService;
    private final RequestScopedCache requestScopedCache;

    /**
     * True when the caller may see the enrolments of one scheduled session.
     *
     * @param instanceUuid the scheduled instance named in the request
     */
    public boolean canReadInstanceRoster(UUID instanceUuid) {
        if (instanceUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_INSTANCE_PREFIX + instanceUuid, () -> {
            try {
                if (domainSecurityService.isPlatformAdmin()) {
                    return true;
                }
                ScheduledInstance instance = scheduledInstanceRepository.findByUuid(instanceUuid).orElse(null);
                if (instance == null) {
                    return false;
                }
                return hasReachOverSession(instance);
            } catch (Exception e) {
                log.error("Error checking roster access for scheduled instance {}", instanceUuid, e);
                return false;
            }
        });
    }

    /**
     * True when the caller may see the whole enrolment list of a class definition — every learner
     * across every session the class has ever run — and, before any enrolment exists, whether a
     * named learner may join it.
     * <p>
     * The reach is over the class rather than a single session, because both questions are asked of
     * the class: a class's roster page covers all of its sessions, and eligibility is asked while
     * the learner still sits on none of them. A learner is deliberately not included: seeing who
     * else is in a class is not part of joining or attending it, and the service that serves the
     * roster narrows the list to a learner's own rows instead of refusing them outright.
     *
     * @param classDefinitionUuid the class named in the request
     */
    public boolean canReadClassRoster(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_CLASS_PREFIX + classDefinitionUuid, () -> {
            try {
                if (domainSecurityService.isPlatformAdmin()) {
                    return true;
                }
                return hasReachOverClass(classDefinitionUuid);
            } catch (Exception e) {
                log.error("Error checking roster access for class definition {}", classDefinitionUuid, e);
                return false;
            }
        });
    }

    /**
     * True when the caller may see or act on one enrolment because they hold the session it sits on.
     * Ownership by the learner is deliberately not folded in: the routes that a learner may reach
     * say so themselves, so the two rules stay readable side by side in the guard.
     *
     * @param enrollmentUuid the enrolment named in the request
     */
    public boolean canAccessEnrolment(UUID enrollmentUuid) {
        if (enrollmentUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_ENROLMENT_PREFIX + enrollmentUuid, () -> {
            try {
                if (domainSecurityService.isPlatformAdmin()) {
                    return true;
                }
                Enrollment enrollment = enrollmentRepository.findByUuid(enrollmentUuid).orElse(null);
                if (enrollment == null) {
                    return false;
                }
                return canReadInstanceRoster(enrollment.getScheduledInstanceUuid());
            } catch (Exception e) {
                log.error("Error checking access to enrolment {}", enrollmentUuid, e);
                return false;
            }
        });
    }

    /**
     * True when the caller may read the enrolment record of a named learner, because that learner
     * sits on at least one session the caller holds.
     * <p>
     * The alternative rule — "any instructor may look up any learner" — turns a learner's identifier
     * into a key that opens their record to every instructor on the platform, including institutions
     * they have never studied with. Requiring a shared session keeps the reach to the institutions
     * the learner actually joined, and still lets an instructor's own roster page ask about the
     * students on it, because those students came from that instructor's own classes.
     *
     * @param studentUuid the learner named in the request
     */
    public boolean canReadLearnerRecord(UUID studentUuid) {
        if (studentUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_LEARNER_PREFIX + studentUuid, () -> {
            try {
                if (domainSecurityService.isPlatformAdmin()) {
                    return true;
                }
                Set<UUID> instanceUuids = enrollmentRepository.findByStudentUuid(studentUuid).stream()
                        .map(Enrollment::getScheduledInstanceUuid)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (instanceUuids.isEmpty()) {
                    return false;
                }
                return sharesASessionWith(instanceUuids);
            } catch (Exception e) {
                log.error("Error checking access to the enrolment record of student {}", studentUuid, e);
                return false;
            }
        });
    }

    /**
     * Walks the learner's sessions once, cheapest test first: the caller teaching a session is
     * decided in memory, the classes behind those sessions are then deduplicated so that ownership
     * and the owning organisation are each asked about a handful of classes rather than every
     * session the learner has ever attended.
     */
    private boolean sharesASessionWith(Set<UUID> instanceUuids) {
        UUID callerInstructorUuid = currentInstructorUuid();
        Set<UUID> classDefinitionUuids = new LinkedHashSet<>();
        for (ScheduledInstance instance : scheduledInstanceRepository.findByUuidIn(instanceUuids)) {
            if (callerInstructorUuid != null && callerInstructorUuid.equals(instance.getInstructorUuid())) {
                return true;
            }
            if (instance.getClassDefinitionUuid() != null) {
                classDefinitionUuids.add(instance.getClassDefinitionUuid());
            }
        }
        if (classDefinitionUuids.isEmpty()) {
            return false;
        }
        if (callerInstructorUuid != null) {
            for (UUID classDefinitionUuid : classDefinitionUuids) {
                if (classDefinitionLookupService.findDefaultInstructorUuid(classDefinitionUuid)
                        .filter(callerInstructorUuid::equals)
                        .isPresent()) {
                    return true;
                }
            }
        }
        return classDefinitionLookupService.findOrganisationUuids(classDefinitionUuids).values().stream()
                .distinct()
                .anyMatch(domainSecurityService::managesOrganisation);
    }

    private UUID currentInstructorUuid() {
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null) {
            return null;
        }
        return instructorLookupService.findInstructorUuidByUserUuid(callerUuid).orElse(null);
    }

    /**
     * The two ways a non-administrator holds a session: they are scheduled to teach that particular
     * sitting, or they hold the class it instantiates.
     */
    private boolean hasReachOverSession(ScheduledInstance instance) {
        if (domainSecurityService.isInstructorWithUuid(instance.getInstructorUuid())) {
            return true;
        }
        return hasReachOverClass(instance.getClassDefinitionUuid());
    }

    /**
     * The three ways a non-administrator holds a class: they are its default instructor, they are
     * scheduled to teach at least one of its sessions, or they run the organisation it belongs to.
     * The instructor legs are tried first because both are answered from the caller's own identity
     * and a single indexed lookup, before the organisation membership question is asked.
     */
    private boolean hasReachOverClass(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return false;
        }
        UUID callerInstructorUuid = currentInstructorUuid();
        if (callerInstructorUuid != null) {
            boolean ownsClass = classDefinitionLookupService.findDefaultInstructorUuid(classDefinitionUuid)
                    .filter(callerInstructorUuid::equals)
                    .isPresent();
            if (ownsClass) {
                return true;
            }
            if (scheduledInstanceRepository.existsByClassDefinitionUuidAndInstructorUuid(
                    classDefinitionUuid, callerInstructorUuid)) {
                return true;
            }
        }
        return classDefinitionLookupService.findOrganisationUuid(classDefinitionUuid)
                .filter(domainSecurityService::managesOrganisation)
                .isPresent();
    }
}
