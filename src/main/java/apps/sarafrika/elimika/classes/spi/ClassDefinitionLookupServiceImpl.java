package apps.sarafrika.elimika.classes.spi;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.projection.TrainerClassCount;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.classes.repository.projection.OpenClassSeatRow;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.CourseOpenClassSummary;
import apps.sarafrika.elimika.shared.spi.OpenClassListing;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;

import java.time.LocalDate;
import java.time.ZoneOffset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassDefinitionLookupServiceImpl implements ClassDefinitionLookupService {

    private final ClassDefinitionRepository classDefinitionRepository;
    private final EnrollmentLookupService enrollmentLookupService;
    private final TrainingBranchLookupService trainingBranchLookupService;

    @Override
    public Optional<ClassDefinitionSnapshot> findByUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(ClassDefinitionLookupServiceImpl::toSnapshot);
    }

    @Override
    public Map<UUID, ClassDefinitionSnapshot> findByUuids(Collection<UUID> classDefinitionUuids) {
        if (classDefinitionUuids == null || classDefinitionUuids.isEmpty()) {
            return Map.of();
        }
        Collection<UUID> requested = classDefinitionUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ClassDefinitionSnapshot> snapshots = new LinkedHashMap<>();
        for (ClassDefinition classDefinition : classDefinitionRepository.findByUuidIn(requested)) {
            snapshots.put(classDefinition.getUuid(), toSnapshot(classDefinition));
        }
        return snapshots;
    }

    @Override
    public Optional<UUID> findDefaultInstructorUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(ClassDefinition::getDefaultInstructorUuid);
    }

    @Override
    public Optional<UUID> findOrganisationUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(ClassDefinition::getOrganisationUuid);
    }

    @Override
    public Optional<UUID> findBranchUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(ClassDefinition::getBranchUuid);
    }

    @Override
    public Optional<UUID> findMarketplaceJobUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(ClassDefinition::getMarketplaceJobUuid);
    }

    @Override
    public Map<UUID, UUID> findOrganisationUuids(Collection<UUID> classDefinitionUuids) {
        if (classDefinitionUuids == null || classDefinitionUuids.isEmpty()) {
            return Map.of();
        }

        Collection<UUID> requested = classDefinitionUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UUID> organisationsByClass = new LinkedHashMap<>();
        for (ClassDefinition classDefinition : classDefinitionRepository.findByUuidIn(requested)) {
            if (classDefinition.getUuid() != null && classDefinition.getOrganisationUuid() != null) {
                organisationsByClass.put(classDefinition.getUuid(), classDefinition.getOrganisationUuid());
            }
        }
        return organisationsByClass;
    }

    @Override
    public CourseClassScope findClassScopeForCourse(UUID courseUuid) {
        if (courseUuid == null) {
            return CourseClassScope.empty();
        }
        return toScope(classDefinitionRepository.findClassSeatingByCourseUuid(courseUuid));
    }

    @Override
    public CourseClassScope findClassScopeForCourseAndTrainer(UUID courseUuid,
                                                              UUID instructorUuid,
                                                              Collection<UUID> organisationUuids) {
        boolean noOrganisations = organisationUuids == null || organisationUuids.isEmpty();
        if (courseUuid == null || (instructorUuid == null && noOrganisations)) {
            return CourseClassScope.empty();
        }

        List<Object[]> rows = new ArrayList<>();
        if (instructorUuid != null) {
            rows.addAll(classDefinitionRepository
                    .findClassSeatingByCourseUuidAndInstructorUuid(courseUuid, instructorUuid));
        }
        if (!noOrganisations) {
            rows.addAll(classDefinitionRepository
                    .findClassSeatingByCourseUuidAndOrganisationUuidIn(courseUuid, organisationUuids));
        }
        return toScope(rows);
    }

    /**
     * Folds seating rows into a scope, keyed on the class UUID so a class that an instructor leads
     * <em>and</em> their organisation owns is counted once rather than twice.
     */
    private static CourseClassScope toScope(List<Object[]> rows) {
        Map<UUID, Object[]> distinct = new LinkedHashMap<>();
        for (Object[] row : rows) {
            UUID classUuid = (UUID) row[0];
            if (classUuid != null) {
                distinct.putIfAbsent(classUuid, row);
            }
        }
        if (distinct.isEmpty()) {
            return CourseClassScope.empty();
        }

        List<UUID> all = List.copyOf(distinct.keySet());
        List<UUID> active = new ArrayList<>();
        long capacity = 0L;
        for (Object[] row : distinct.values()) {
            if (!Boolean.TRUE.equals(row[1])) {
                continue;
            }
            active.add((UUID) row[0]);
            if (row[2] instanceof Number seats) {
                capacity += seats.longValue();
            }
        }
        return new CourseClassScope(all, List.copyOf(active), capacity);
    }

    @Override
    public List<UUID> findClassDefinitionUuidsByInstructorUuid(UUID instructorUuid) {
        if (instructorUuid == null) {
            return List.of();
        }
        return classDefinitionRepository.findByDefaultInstructorUuid(instructorUuid)
                .stream()
                .map(ClassDefinition::getUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public List<UUID> findClassDefinitionUuidsByOrganisationUuid(UUID organisationUuid) {
        if (organisationUuid == null) {
            return List.of();
        }
        return classDefinitionRepository.findByOrganisationUuid(organisationUuid)
                .stream()
                .map(ClassDefinition::getUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public Map<UUID, Long> countActiveCourseClassesByInstructor(UUID courseUuid, Collection<UUID> instructorUuids) {
        Collection<UUID> requested = distinct(instructorUuids);
        if (courseUuid == null || requested.isEmpty()) {
            return Map.of();
        }
        return toCountMap(classDefinitionRepository.countActiveByCourseAndInstructor(courseUuid, requested));
    }

    @Override
    public Map<UUID, Long> countActiveCourseClassesByOrganisation(UUID courseUuid, Collection<UUID> organisationUuids) {
        Collection<UUID> requested = distinct(organisationUuids);
        if (courseUuid == null || requested.isEmpty()) {
            return Map.of();
        }
        return toCountMap(classDefinitionRepository.countActiveByCourseAndOrganisation(courseUuid, requested));
    }

    @Override
    public Map<UUID, Long> countActivePublicClassesByCourse(Collection<UUID> courseUuids) {
        Collection<UUID> requested = distinct(courseUuids);
        if (requested.isEmpty()) {
            return Map.of();
        }
        return toCountMap(classDefinitionRepository.countActiveByCourseAndVisibility(requested, ClassVisibility.PUBLIC));
    }

    @Override
    public List<OpenClassListing> findOpenClassesForCourse(UUID courseUuid) {
        if (courseUuid == null) {
            return List.of();
        }
        List<ClassDefinition> open = classDefinitionRepository.findOpenByCourse(courseUuid, ClassVisibility.PUBLIC, todayUtc());
        if (open.isEmpty()) {
            return List.of();
        }
        List<UUID> classUuids = open.stream().map(ClassDefinition::getUuid).toList();
        Map<UUID, Long> filledSeats = enrollmentLookupService.countFilledSeatsByClassDefinition(classUuids);
        Map<UUID, String> branchNames = trainingBranchLookupService.findBranchNames(
                open.stream().map(ClassDefinition::getBranchUuid).filter(Objects::nonNull).distinct().toList());

        List<OpenClassListing> listings = new ArrayList<>(open.size());
        for (ClassDefinition definition : open) {
            listings.add(new OpenClassListing(
                    definition.getUuid(),
                    definition.getTitle(),
                    definition.getLocationType(),
                    definition.getSessionFormat(),
                    definition.getLocationName(),
                    definition.getSalePrice(),
                    definition.getMaxParticipants(),
                    seatsLeft(definition.getMaxParticipants(), filledSeats.getOrDefault(definition.getUuid(), 0L)),
                    startsOn(definition),
                    definition.getAcademicPeriodEndDate(),
                    definition.getRegistrationPeriodEndDate(),
                    definition.getBranchUuid() == null ? null : branchNames.get(definition.getBranchUuid())));
        }
        return listings;
    }

    @Override
    public Map<UUID, CourseOpenClassSummary> summariseOpenClassesByCourse(Collection<UUID> courseUuids) {
        Collection<UUID> requested = distinct(courseUuids);
        if (requested.isEmpty()) {
            return Map.of();
        }
        List<OpenClassSeatRow> rows =
                classDefinitionRepository.findOpenSeatRowsByCourses(requested, ClassVisibility.PUBLIC, todayUtc());
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> filledSeats = enrollmentLookupService.countFilledSeatsByClassDefinition(
                rows.stream().map(OpenClassSeatRow::classUuid).toList());

        // A full class stays listed on the course page but is not something a visitor can join, so
        // it neither counts nor sets the "from" price.
        Map<UUID, long[]> counts = new LinkedHashMap<>();
        Map<UUID, java.math.BigDecimal> minFees = new LinkedHashMap<>();
        for (OpenClassSeatRow row : rows) {
            if (row.courseUuid() == null) {
                continue;
            }
            Integer left = seatsLeft(row.maxParticipants(), filledSeats.getOrDefault(row.classUuid(), 0L));
            if (left != null && left == 0) {
                continue;
            }
            counts.computeIfAbsent(row.courseUuid(), key -> new long[1])[0]++;
            if (row.fee() != null) {
                minFees.merge(row.courseUuid(), row.fee(), (a, b) -> a.compareTo(b) <= 0 ? a : b);
            }
        }
        Map<UUID, CourseOpenClassSummary> byCourse = new LinkedHashMap<>();
        counts.forEach((course, count) ->
                byCourse.put(course, new CourseOpenClassSummary(count[0], minFees.get(course))));
        return byCourse;
    }

    private static LocalDate todayUtc() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /** Seats offered minus seats taken, floored at zero; unknown when the class sets no cap. */
    private static Integer seatsLeft(Integer maxParticipants, long filled) {
        if (maxParticipants == null) {
            return null;
        }
        return (int) Math.max(0L, maxParticipants - filled);
    }

    /** The teaching period's first day, else the day of the first session. */
    private static LocalDate startsOn(ClassDefinition definition) {
        if (definition.getAcademicPeriodStartDate() != null) {
            return definition.getAcademicPeriodStartDate();
        }
        return definition.getDefaultStartTime() == null ? null : definition.getDefaultStartTime().toLocalDate();
    }

    private static Collection<UUID> distinct(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        return uuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<UUID, Long> toCountMap(List<TrainerClassCount> counts) {
        Map<UUID, Long> byTrainer = new LinkedHashMap<>();
        for (TrainerClassCount count : counts) {
            if (count.trainerUuid() != null) {
                byTrainer.put(count.trainerUuid(), count.classCount());
            }
        }
        return byTrainer;
    }

    private static ClassDefinitionSnapshot toSnapshot(ClassDefinition entity) {
        return new ClassDefinitionSnapshot(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getProgramUuid(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getSalePrice(),
                entity.getInstructorPay(),
                entity.getRateBasis(),
                entity.getClassVisibility(),
                entity.getLocationType(),
                entity.getMaxParticipants(),
                entity.getAllowWaitlist(),
                entity.getClassReminderMinutes(),
                entity.getRegistrationPeriodStartDate(),
                entity.getRegistrationPeriodEndDate()
        );
    }
}
