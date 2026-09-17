package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.model.TrainingApplicationRecord;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Resolves applicant display names in two batch lookups, one per applicant kind. */
@Component
@RequiredArgsConstructor
public class TrainingApplicantNames {

    private final InstructorLookupService instructorLookupService;
    private final OrganisationLookupService organisationLookupService;

    public Map<UUID, String> resolve(Collection<? extends TrainingApplicationRecord> applications) {
        Set<UUID> instructors = applicantsOfType(applications, CourseTrainingApplicantType.INSTRUCTOR);
        Set<UUID> organisations = applicantsOfType(applications, CourseTrainingApplicantType.ORGANISATION);

        Map<UUID, String> names = new HashMap<>();
        if (!instructors.isEmpty()) {
            instructorLookupService.findInstructorDirectoryEntries(instructors)
                    .forEach((uuid, entry) -> names.put(uuid, entry.displayName()));
        }
        if (!organisations.isEmpty()) {
            names.putAll(organisationLookupService.findOrganisationNames(organisations));
        }
        return names;
    }

    public String resolve(TrainingApplicationRecord application) {
        return resolve(List.of(application)).get(application.getApplicantUuid());
    }

    private static Set<UUID> applicantsOfType(Collection<? extends TrainingApplicationRecord> applications,
                                              CourseTrainingApplicantType type) {
        return applications.stream()
                .filter(application -> type.equals(application.getApplicantType()))
                .map(TrainingApplicationRecord::getApplicantUuid)
                .filter(uuid -> uuid != null)
                .collect(Collectors.toSet());
    }
}
