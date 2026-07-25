package apps.sarafrika.elimika.instructor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Aggregate directory row for an instructor within an organisation.
 * <p>
 * Combines the instructor's identity (from the tenancy org-domain mapping), their
 * highest recorded qualification and top skill (instructor profile), their aggregate
 * review rating, and the number of class definitions they are the default instructor for.
 * Powers the organisation Instructors table without per-row round-trips.
 */
@Schema(
        name = "OrgInstructorSummary",
        description = "Aggregated directory row for one instructor scoped to an organisation.",
        example = """
        {
          "user_uuid": "550e8400-e29b-41d4-a716-446655440000",
          "instructor_uuid": "inst-1234-5678-90ab-cdef12345678",
          "full_name": "Jane Smith",
          "email": "jane.smith@example.com",
          "highest_qualification": "Master's Degree",
          "field_of_study": "Computer Science",
          "top_skill": "Cloud Computing",
          "average_rating": 4.7,
          "review_count": 12,
          "class_count": 5
        }
        """
)
public record OrgInstructorSummaryDTO(

        @Schema(description = "UUID of the underlying user account.")
        @JsonProperty("user_uuid")
        UUID userUuid,

        @Schema(description = "UUID of the instructor profile.")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(description = "Instructor's full name.")
        @JsonProperty("full_name")
        String fullName,

        @Schema(description = "Instructor's email address.", nullable = true)
        @JsonProperty("email")
        String email,

        @Schema(description = "Most recent recorded qualification (level of study). Null when none recorded.", nullable = true)
        @JsonProperty("highest_qualification")
        String highestQualification,

        @Schema(description = "Field of study of the most recent qualification. Null when none recorded.", nullable = true)
        @JsonProperty("field_of_study")
        String fieldOfStudy,

        @Schema(description = "A representative skill for the instructor. Null when none recorded.", nullable = true)
        @JsonProperty("top_skill")
        String topSkill,

        @Schema(description = "Average review rating (1-5). Null when there are no reviews.", nullable = true)
        @JsonProperty("average_rating")
        Double averageRating,

        @Schema(description = "Total number of reviews.")
        @JsonProperty("review_count")
        long reviewCount,

        @Schema(description = "Number of class definitions this instructor is the default instructor for.")
        @JsonProperty("class_count")
        long classCount
) {
}
