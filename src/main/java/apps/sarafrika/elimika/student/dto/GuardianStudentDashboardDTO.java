package apps.sarafrika.elimika.student.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgramProgressView;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;

import java.util.List;
import java.util.UUID;

public record GuardianStudentDashboardDTO(
        @JsonProperty("student_uuid") UUID studentUuid,
        @JsonProperty("student_name") String studentName,
        @JsonProperty("share_scope") GuardianShareScope shareScope,
        @JsonProperty("status") GuardianLinkStatus status,
        @JsonProperty("course_progress") List<LearnerCourseProgressView> courseProgress,
        @JsonProperty("program_progress") List<LearnerProgramProgressView> programProgress
) {
}
