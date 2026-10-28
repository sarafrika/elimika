package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "ProgramReport", description = "A learner's live programme report: mark, pass checks, courses, skills and remarks")
public record ProgramReportDTO(
        @JsonProperty("program_uuid") UUID programUuid,
        @JsonProperty("program_title") String programTitle,
        @JsonProperty("program_code") String programCode,
        @JsonProperty("program_enrollment_uuid") UUID programEnrollmentUuid,
        @JsonProperty("student_uuid") UUID studentUuid,
        @Schema(description = "Weighted mark over decided mandatory courses")
        @JsonProperty("final_grade") BigDecimal finalGrade,
        @Schema(description = "True until every mandatory course is decided")
        @JsonProperty("final_grade_provisional") boolean finalGradeProvisional,
        @JsonProperty("pass_mark") BigDecimal passMark,
        @Schema(description = "IN_PROGRESS, PASSED or FAILED")
        @JsonProperty("result_status") String resultStatus,
        @JsonProperty("mandatory_total") int mandatoryTotal,
        @JsonProperty("mandatory_passed") int mandatoryPassed,
        @Schema(description = "Start of the reporting period; null for the whole programme")
        @JsonProperty("from") LocalDate from,
        @JsonProperty("to") LocalDate to,
        @JsonProperty("courses") List<CourseRow> courses,
        @Schema(description = "Attendance over every programme course in the period")
        @JsonProperty("attendance") Attendance attendance,
        @JsonProperty("skills") List<SkillRow> skills,
        @JsonProperty("remarks") List<ProgramRemarkDTO> remarks,
        @Schema(description = "True when the caller may add a progress remark")
        @JsonProperty("can_add_remarks") boolean canAddRemarks
) {

    public ProgramReportDTO withCanAddRemarks(boolean canAdd) {
        return new ProgramReportDTO(programUuid, programTitle, programCode, programEnrollmentUuid, studentUuid, finalGrade,
                finalGradeProvisional, passMark, resultStatus, mandatoryTotal, mandatoryPassed, from, to, courses, attendance, skills,
                remarks, canAdd);
    }

    @Schema(name = "ProgramReportCourse")
    public record CourseRow(
            @JsonProperty("course_uuid") UUID courseUuid,
            @JsonProperty("course_title") String courseTitle,
            @JsonProperty("course_code") String courseCode,
            @JsonProperty("is_required") boolean isRequired,
            @JsonProperty("weight_percentage") BigDecimal weightPercentage,
            @JsonProperty("course_enrollment_uuid") UUID courseEnrollmentUuid,
            @JsonProperty("final_grade") BigDecimal finalGrade,
            @JsonProperty("grade_band") String gradeBand,
            @Schema(description = "IN_PROGRESS, PASSED, FAILED or NOT_STARTED")
            @JsonProperty("result_status") String resultStatus,
            @Schema(description = "Points this course adds: final × weight ÷ 100, once decided")
            @JsonProperty("contribution") BigDecimal contribution,
            @JsonProperty("sessions_attended") int sessionsAttended,
            @Schema(description = "Attended plus absent in the period; excused sessions are left out")
            @JsonProperty("sessions_counted") int sessionsCounted,
            @JsonProperty("sessions_excused") int sessionsExcused,
            @Schema(description = "Attended, absent and excused sessions in the period")
            @JsonProperty("sessions_total") int sessionsTotal,
            @Schema(description = "Attended ÷ counted × 100; excused left out; null when nothing is counted")
            @JsonProperty("attendance_percentage") BigDecimal attendancePercentage
    ) {
    }

    @Schema(name = "ProgramReportAttendance")
    public record Attendance(
            @JsonProperty("sessions_attended") int sessionsAttended,
            @Schema(description = "Attended plus absent; excused sessions are left out")
            @JsonProperty("sessions_counted") int sessionsCounted,
            @JsonProperty("sessions_excused") int sessionsExcused,
            @JsonProperty("sessions_total") int sessionsTotal,
            @Schema(description = "Attended ÷ counted × 100; null when nothing is counted")
            @JsonProperty("attendance_percentage") BigDecimal attendancePercentage
    ) {
    }

    @Schema(name = "ProgramReportSkill")
    public record SkillRow(
            @JsonProperty("skill_uuid") UUID skillUuid,
            @JsonProperty("skill_name") String skillName,
            @Schema(description = "Null until there is evidence")
            @JsonProperty("level_name") String levelName,
            @JsonProperty("counts_as_achieved") boolean countsAsAchieved,
            @JsonProperty("score") BigDecimal score,
            @JsonProperty("graded_at") LocalDateTime gradedAt,
            @JsonProperty("history") List<Evidence> history
    ) {
    }

    @Schema(name = "ProgramReportEvidence")
    public record Evidence(
            @JsonProperty("course_uuid") UUID courseUuid,
            @JsonProperty("course_title") String courseTitle,
            @JsonProperty("course_assessment_uuid") UUID courseAssessmentUuid,
            @JsonProperty("assessment_title") String assessmentTitle,
            @Schema(description = "Assessment and line item, e.g. \"Lesson 3 · Solo performance\"")
            @JsonProperty("source_title") String sourceTitle,
            @JsonProperty("score") BigDecimal score,
            @JsonProperty("level_name") String levelName,
            @JsonProperty("graded_at") LocalDateTime gradedAt,
            @Schema(description = "True when a later regrade of the same work replaced it")
            @JsonProperty("superseded") boolean superseded,
            @Schema(description = "User who graded the work; null for older or system grades")
            @JsonProperty("graded_by_uuid") UUID gradedByUuid,
            @JsonProperty("graded_by_name") String gradedByName
    ) {
    }
}
