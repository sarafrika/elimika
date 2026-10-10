package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassBatchSummaryDTO;
import apps.sarafrika.elimika.classes.internal.ClassListingVisibility;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.security.ActingDomainCap;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Resolves many classes by id in a fixed number of queries whatever the batch size: classes, titles,
// instructors and enrolment counts are one read each. Visibility is the class listing's; enrolment
// figures are shown only to parties to the class.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassBatchLookupService {

    /** Upper bound on ids per request, matching the search page cap. */
    public static final int MAX_BATCH_SIZE = 100;

    private final ClassDefinitionRepository classDefinitionRepository;
    private final ClassListingVisibility classListingVisibility;
    private final CourseInfoService courseInfoService;
    private final InstructorLookupService instructorLookupService;
    private final ObjectProvider<TimetableService> timetableServiceProvider;
    private final DomainSecurityService domainSecurityService;
    private final ActingDomainCap actingDomainCap;

    /** Visible classes among {@code uuids} in request order; unknown or hidden ids are omitted. */
    public List<ClassBatchSummaryDTO> findByUuids(Collection<UUID> uuids) {
        Set<UUID> ids = uuids == null ? Set.of() : uuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("At most " + MAX_BATCH_SIZE + " class uuids may be requested at once");
        }
        log.debug("Batch lookup of {} classes", ids.size());

        Map<UUID, ClassDefinition> byUuid = classDefinitionRepository.findByUuidIn(ids).stream()
                .collect(Collectors.toMap(ClassDefinition::getUuid, Function.identity(), (first, second) -> first));
        ClassListingVisibility.Scope scope = classListingVisibility.forCurrentCaller();
        List<ClassDefinition> visible = ids.stream()
                .map(byUuid::get)
                .filter(Objects::nonNull)
                .filter(scope::admits)
                .toList();
        if (visible.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> courseTitles = lookup(distinct(visible, ClassDefinition::getCourseUuid),
                courseInfoService::getCourseNames);
        Map<UUID, String> programTitles = lookup(distinct(visible, ClassDefinition::getProgramUuid),
                courseInfoService::getTrainingProgramTitles);
        Map<UUID, InstructorDirectoryEntry> instructors = lookup(
                distinct(visible, ClassDefinition::getDefaultInstructorUuid),
                instructorLookupService::findInstructorDirectoryEntries);

        PartyCheck parties = new PartyCheck();
        List<UUID> countable = visible.stream().filter(parties::isParty).map(ClassDefinition::getUuid).toList();
        Map<UUID, Long> enrolled = enrolmentCounts(countable);

        return visible.stream()
                .map(definition -> toSummary(definition, courseTitles, programTitles, instructors, enrolled))
                .toList();
    }

    private Map<UUID, Long> enrolmentCounts(List<UUID> classUuids) {
        TimetableService timetableService = timetableServiceProvider.getIfAvailable();
        if (classUuids.isEmpty() || timetableService == null) {
            return Map.of();
        }
        return timetableService.getActiveEnrolmentCounts(classUuids);
    }

    private static ClassBatchSummaryDTO toSummary(ClassDefinition definition,
                                                  Map<UUID, String> courseTitles,
                                                  Map<UUID, String> programTitles,
                                                  Map<UUID, InstructorDirectoryEntry> instructors,
                                                  Map<UUID, Long> enrolled) {
        InstructorDirectoryEntry entry = definition.getDefaultInstructorUuid() == null
                ? null : instructors.get(definition.getDefaultInstructorUuid());
        ClassBatchSummaryDTO.InstructorSummary instructor = entry == null ? null
                : new ClassBatchSummaryDTO.InstructorSummary(entry.instructorUuid(), entry.displayName(),
                entry.adminVerified());
        Long enrolledCount = enrolled.get(definition.getUuid());
        Long seatsRemaining = enrolledCount == null || definition.getMaxParticipants() == null ? null
                : Math.max(0L, definition.getMaxParticipants() - enrolledCount);
        return new ClassBatchSummaryDTO(
                definition.getUuid(),
                definition.getTitle(),
                FileUrlResolver.publicUrl(definition.getThumbnailUrl()),
                definition.getCourseUuid(),
                definition.getCourseUuid() == null ? null : courseTitles.get(definition.getCourseUuid()),
                definition.getProgramUuid(),
                definition.getProgramUuid() == null ? null : programTitles.get(definition.getProgramUuid()),
                definition.getOrganisationUuid(),
                definition.getDefaultInstructorUuid(),
                instructor,
                definition.getIsActive(),
                definition.getClassVisibility(),
                definition.getLocationType(),
                definition.getSessionFormat(),
                definition.getDefaultStartTime(),
                definition.getDefaultEndTime(),
                definition.getSalePrice(),
                definition.getMaxParticipants(),
                definition.getAllowWaitlist(),
                enrolledCount,
                seatsRemaining);
    }

    private static <V> Map<UUID, V> lookup(Set<UUID> ids, Function<Collection<UUID>, Map<UUID, V>> query) {
        return ids.isEmpty() ? Map.of() : query.apply(ids);
    }

    private static Set<UUID> distinct(List<ClassDefinition> definitions, Function<ClassDefinition, UUID> field) {
        return definitions.stream().map(field).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    // The instructor-pay party rule (own instructor, organisation manager, platform admin), each
    // clause capped by the acting dashboard, resolved once per caller and once per organisation.
    private final class PartyCheck {
        private Boolean admin;
        private UUID currentInstructor;
        private boolean instructorResolved;
        private final Map<UUID, Boolean> managedOrganisations = new HashMap<>();

        boolean isParty(ClassDefinition definition) {
            try {
                if (isAdmin()) {
                    return true;
                }
                UUID instructorUuid = definition.getDefaultInstructorUuid();
                if (instructorUuid != null && actingDomainCap.permits(UserDomain.instructor)
                        && instructorUuid.equals(currentInstructor())) {
                    return true;
                }
                UUID organisationUuid = definition.getOrganisationUuid();
                return organisationUuid != null && actingDomainCap.permits(UserDomain.organisation_user)
                        && managedOrganisations.computeIfAbsent(organisationUuid, this::manages);
            } catch (Exception e) {
                // Withholding is the safe failure: a reader who cannot be identified is not entitled.
                return false;
            }
        }

        private boolean isAdmin() {
            if (admin == null) {
                admin = actingDomainCap.permits(UserDomain.admin) && domainSecurityService.isPlatformAdmin();
            }
            return admin;
        }

        private UUID currentInstructor() {
            if (!instructorResolved) {
                currentInstructor = domainSecurityService.getCurrentInstructorUuid();
                instructorResolved = true;
            }
            return currentInstructor;
        }

        private boolean manages(UUID organisationUuid) {
            return domainSecurityService.belongsToOrganisationWithDomain(organisationUuid, UserDomain.organisation_user)
                    || domainSecurityService.belongsToOrganisationWithDomain(organisationUuid, UserDomain.admin);
        }
    }
}
