package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(name = "ClassProgrammeProgress", description = "Programme marks and skill levels for the learners of one programme class")
public record ClassProgrammeProgressDTO(
        @Schema(description = "READY with data; NO_PROGRAMME or PROGRAMME_CHOICE_REQUIRED with empty skills and learners")
        @JsonProperty("status") Status status,
        @JsonProperty("class_definition_uuid") UUID classDefinitionUuid,
        @Schema(description = "Null unless status is READY")
        @JsonProperty("program_uuid") UUID programUuid,
        @JsonProperty("program_title") String programTitle,
        @Schema(description = "Every programme the class's course belongs to; pick one and resend as program_uuid")
        @JsonProperty("programmes") List<ProgrammeOption> programmes,
        @JsonProperty("skills") List<SkillSummary> skills,
        @JsonProperty("learners") List<Learner> learners
) {

    @Schema(name = "ClassProgrammeProgressStatus")
    public enum Status { READY, NO_PROGRAMME, PROGRAMME_CHOICE_REQUIRED }

    public static ClassProgrammeProgressDTO unresolved(UUID classDefinitionUuid, List<ProgrammeOption> programmes) {
        return new ClassProgrammeProgressDTO(programmes.isEmpty() ? Status.NO_PROGRAMME : Status.PROGRAMME_CHOICE_REQUIRED,
                classDefinitionUuid, null, null, programmes, List.of(), List.of());
    }

    @Schema(name = "ClassProgrammeOption")
    public record ProgrammeOption(
            @JsonProperty("program_uuid") UUID programUuid,
            @JsonProperty("title") String title
    ) {
    }

    @Schema(name = "ClassProgrammeSkill")
    public record SkillSummary(
            @JsonProperty("skill_uuid") UUID skillUuid,
            @JsonProperty("skill_name") String skillName,
            @Schema(description = "Learners on a level that counts as achieved")
            @JsonProperty("achieved") int achieved,
            @Schema(description = "Learners above the lowest level but not yet achieved")
            @JsonProperty("developing") int developing,
            @Schema(description = "Learners on the lowest level")
            @JsonProperty("not_achieved") int notAchieved,
            @JsonProperty("no_evidence") int noEvidence
    ) {
    }

    @Schema(name = "ClassProgrammeLearner")
    public record Learner(
            @JsonProperty("student_uuid") UUID studentUuid,
            @Schema(description = "The learner's account; the instructor student page is /students/{user_uuid}?sId={student_uuid}")
            @JsonProperty("user_uuid") UUID userUuid,
            @JsonProperty("program_enrollment_uuid") UUID programEnrollmentUuid,
            @JsonProperty("final_grade") BigDecimal finalGrade,
            @JsonProperty("final_grade_provisional") boolean finalGradeProvisional,
            @JsonProperty("result_status") String resultStatus,
            @JsonProperty("mandatory_total") int mandatoryTotal,
            @JsonProperty("mandatory_passed") int mandatoryPassed,
            @JsonProperty("skill_levels") List<SkillLevel> skillLevels
    ) {
    }

    @Schema(name = "ClassProgrammeLearnerSkill")
    public record SkillLevel(
            @JsonProperty("skill_uuid") UUID skillUuid,
            @JsonProperty("level_name") String levelName,
            @JsonProperty("counts_as_achieved") boolean countsAsAchieved
    ) {
    }
}
