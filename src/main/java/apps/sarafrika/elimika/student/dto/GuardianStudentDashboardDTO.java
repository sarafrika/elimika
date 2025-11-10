package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgramProgressView;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;

import java.util.List;
import java.util.UUID;

public record GuardianStudentDashboardDTO(
        UUID studentUuid,
        String studentName,
        GuardianShareScope shareScope,
        GuardianLinkStatus status,
        List<LearnerCourseProgressView> courseProgress,
        List<LearnerProgramProgressView> programProgress
) {
}
