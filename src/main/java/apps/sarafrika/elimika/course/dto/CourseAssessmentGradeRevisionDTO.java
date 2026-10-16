package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.model.CourseAssessmentGradeRevision;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "CourseAssessmentGradeRevision", description = "One grade given to a learner on a gradebook line item, oldest first")
public record CourseAssessmentGradeRevisionDTO(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("line_item_uuid") UUID lineItemUuid,
        @JsonProperty("enrollment_uuid") UUID enrollmentUuid,
        @JsonProperty("revision_number") Integer revisionNumber,
        @JsonProperty("item_type") CourseAssessmentLineItemType itemType,
        @JsonProperty("score") BigDecimal score,
        @JsonProperty("max_score") BigDecimal maxScore,
        @JsonProperty("percentage") BigDecimal percentage,
        @JsonProperty("comments") String comments,
        @JsonProperty("criteria") List<CriterionDTO> criteria,
        @JsonProperty("graded_at") LocalDateTime gradedAt,
        @JsonProperty("graded_by_uuid") UUID gradedByUuid
) {

    public record CriterionDTO(
            @JsonProperty("criteria_uuid") UUID criteriaUuid,
            @JsonProperty("criteria_name") String criteriaName,
            @JsonProperty("scoring_level_uuid") UUID scoringLevelUuid,
            @JsonProperty("scoring_level_name") String scoringLevelName,
            @JsonProperty("points") BigDecimal points,
            @JsonProperty("comments") String comments
    ) {
    }

    public static CourseAssessmentGradeRevisionDTO from(CourseAssessmentGradeRevision revision) {
        List<CriterionDTO> criteria = revision.getCriteria() == null ? null : revision.getCriteria().stream()
                .map(c -> new CriterionDTO(c.criteriaUuid(), c.criteriaName(), c.scoringLevelUuid(),
                        c.scoringLevelName(), c.points(), c.comments()))
                .toList();
        return new CourseAssessmentGradeRevisionDTO(revision.getUuid(), revision.getLineItemUuid(),
                revision.getEnrollmentUuid(), revision.getRevisionNumber(), revision.getItemType(),
                revision.getScore(), revision.getMaxScore(), revision.getPercentage(), revision.getComments(),
                criteria, revision.getGradedAt(), revision.getGradedByUuid());
    }
}
