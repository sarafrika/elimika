package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.dto.AgeGroupDTO;
import apps.sarafrika.elimika.course.dto.AgeGroupRequest;
import apps.sarafrika.elimika.course.internal.agegroup.AgeGroups;
import apps.sarafrika.elimika.course.internal.agegroup.AgeGroups.AgeGroupScope;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.util.enums.AgeGroupOwnerType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Training-application policy over {@link AgeGroups}: instructors only, and required on a new submission. */
@Component
@RequiredArgsConstructor
public class TrainingApplicationAgeGroups {

    private final AgeGroups ageGroups;

    public AgeGroupScope forCourse(Course course) {
        return ageGroups.forCourse(course);
    }

    public AgeGroupScope forProgram(UUID programUuid) {
        return ageGroups.forProgram(programUuid);
    }

    /** A new instructor submission must say whom it would teach; updates may still omit the list. */
    public void requireForSubmission(CourseTrainingApplicantType applicantType, List<AgeGroupRequest> groups) {
        if (CourseTrainingApplicantType.INSTRUCTOR.equals(applicantType) && groups == null) {
            throw new IllegalArgumentException("At least one age group is required");
        }
    }

    /** Null leaves stored groups alone; organisations may not send any, instructors may not send none. */
    public void validate(CourseTrainingApplicantType applicantType, AgeGroupScope scope, List<AgeGroupRequest> groups) {
        if (groups == null) {
            return;
        }
        if (!CourseTrainingApplicantType.INSTRUCTOR.equals(applicantType)) {
            if (!groups.isEmpty()) {
                throw new IllegalArgumentException("Only instructor applicants can define age groups");
            }
            return;
        }
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("At least one age group is required");
        }
        ageGroups.validate(scope, groups);
    }

    public void replace(TrainingApplicationType type, UUID applicationUuid, List<AgeGroupRequest> groups) {
        ageGroups.replace(AgeGroupOwnerType.forApplication(type), applicationUuid, groups);
    }

    public void deleteFor(TrainingApplicationType type, UUID applicationUuid) {
        ageGroups.deleteFor(AgeGroupOwnerType.forApplication(type), applicationUuid);
    }

    public Map<UUID, List<AgeGroupDTO>> groups(TrainingApplicationType type, Collection<UUID> applicationUuids) {
        return ageGroups.groupsFor(AgeGroupOwnerType.forApplication(type), applicationUuids);
    }
}
