package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.timetabling.spi.StudentClassEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Decides which classes a caller may see in a class listing.
 * <p>
 * Platform admins see every class. Everyone else sees active PUBLIC classes, plus the classes of
 * organisations they staff (in any visibility or state), the classes they teach, and the classes
 * they are enrolled in — so a learner's dashboard still resolves a private class they study in.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassListingVisibility {

    /** Upper bound on the enrolled classes read for one listing; learners hold far fewer. */
    private static final int MAX_ENROLLED_CLASSES = 1000;

    private final DomainSecurityService domainSecurityService;
    private final UserLookupService userLookupService;
    private final ObjectProvider<TimetableService> timetableServiceProvider;

    /**
     * What the current caller may see, resolved once per listing.
     */
    public Scope forCurrentCaller() {
        if (domainSecurityService.isPlatformAdmin()) {
            return Scope.EVERYTHING;
        }
        return new Scope(false, staffedOrganisations(), domainSecurityService.getCurrentInstructorUuid(),
                enrolledClasses());
    }

    private Set<UUID> staffedOrganisations() {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            return Set.of();
        }
        return userLookupService.getActiveUserOrganizations(userUuid).stream()
                .filter(domainSecurityService::staffsOrganisation)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<UUID> enrolledClasses() {
        UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
        TimetableService timetableService = timetableServiceProvider.getIfAvailable();
        if (studentUuid == null || timetableService == null) {
            return Set.of();
        }
        try {
            return timetableService.getClassEnrollmentsForStudent(studentUuid, PageRequest.of(0, MAX_ENROLLED_CLASSES))
                    .stream()
                    .map(StudentClassEnrollmentSummaryDTO::class_definition_uuid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (RuntimeException e) {
            log.warn("Could not resolve enrolled classes for student {}; listing public classes only", studentUuid, e);
            return Set.of();
        }
    }

    /**
     * @param everything          true for platform admins
     * @param staffedOrganisations organisations whose classes the caller sees in full
     * @param instructorUuid      the caller's instructor profile, if any
     * @param enrolledClasses     classes the caller studies in
     */
    public record Scope(boolean everything,
                        Set<UUID> staffedOrganisations,
                        UUID instructorUuid,
                        Set<UUID> enrolledClasses) {

        static final Scope EVERYTHING = new Scope(true, Set.of(), null, Set.of());

        public boolean staffs(UUID organisationUuid) {
            return everything || (organisationUuid != null && staffedOrganisations.contains(organisationUuid));
        }

        public boolean admits(ClassDefinition definition) {
            if (everything || staffs(definition.getOrganisationUuid())) {
                return true;
            }
            if (instructorUuid != null && instructorUuid.equals(definition.getDefaultInstructorUuid())) {
                return true;
            }
            if (enrolledClasses.contains(definition.getUuid())) {
                return true;
            }
            return Boolean.TRUE.equals(definition.getIsActive())
                    && definition.getClassVisibility() == ClassVisibility.PUBLIC;
        }

        public Specification<ClassDefinition> toSpecification() {
            if (everything) {
                return (root, query, cb) -> cb.conjunction();
            }
            return (root, query, cb) -> {
                List<Predicate> visible = new ArrayList<>();
                visible.add(cb.and(
                        cb.isTrue(root.get("isActive")),
                        cb.equal(root.get("classVisibility"), ClassVisibility.PUBLIC)));
                if (!staffedOrganisations.isEmpty()) {
                    visible.add(root.get("organisationUuid").in(staffedOrganisations));
                }
                if (instructorUuid != null) {
                    visible.add(cb.equal(root.get("defaultInstructorUuid"), instructorUuid));
                }
                if (!enrolledClasses.isEmpty()) {
                    visible.add(root.get("uuid").in(enrolledClasses));
                }
                return cb.or(visible.toArray(Predicate[]::new));
            };
        }
    }
}
