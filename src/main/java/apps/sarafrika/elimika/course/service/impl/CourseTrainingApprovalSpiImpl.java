package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.service.CourseTrainingApplicationService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseTrainingApprovalSpiImpl implements CourseTrainingApprovalSpi {

    private final CourseTrainingApplicationService courseTrainingApplicationService;

    @Override
    public boolean isInstructorApproved(UUID courseUuid, UUID instructorUuid) {
        if (courseUuid == null || instructorUuid == null) {
            return false;
        }
        return courseTrainingApplicationService.hasApprovedApplication(
                courseUuid,
                CourseTrainingApplicantType.INSTRUCTOR,
                instructorUuid
        );
    }

    @Override
    public boolean isOrganisationApproved(UUID courseUuid, UUID organisationUuid) {
        if (courseUuid == null || organisationUuid == null) {
            return false;
        }
        return courseTrainingApplicationService.hasApprovedApplication(
                courseUuid,
                CourseTrainingApplicantType.ORGANISATION,
                organisationUuid
        );
    }
}
