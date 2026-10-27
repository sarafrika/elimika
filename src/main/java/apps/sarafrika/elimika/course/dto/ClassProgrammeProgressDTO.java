package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(name = "ClassProgrammeProgress", description = "Programme marks and skill levels for the learners of one programme class")
public record ClassProgrammeProgressDTO(
        @JsonProperty("class_definition_uuid") UUID classDefinitionUuid,
        @JsonProperty("program_uuid") UUID programUuid,
        @JsonProperty("program_title") String programTitle,
        @JsonProperty("skills") List<SkillSummary> skills,
        @JsonProperty("learners") List<Learner> learners
) {

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
