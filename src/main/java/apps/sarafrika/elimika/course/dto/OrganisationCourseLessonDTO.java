package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * A single lesson as seen by whoever asked for the course's content.
 * <p>
 * Without full read access only the outline is returned — {@code uuid} and {@code contents} are
 * both omitted, so neither the lesson bodies nor the identifiers that would fetch them one by one
 * ever reach the caller. With full access the lesson {@code uuid} and its {@code contents} are
 * included.
 */
@Schema(name = "OrganisationCourseLesson", description = "Lesson outline (always) plus content (only when the caller has full read access).")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganisationCourseLessonDTO(

        @Schema(description = "Lesson identifier. Only present when the caller has full read access.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Ordering position of the lesson within the course.", example = "1")
        @JsonProperty("lesson_number")
        Integer lessonNumber,

        @Schema(description = "Lesson title.", example = "Introduction to Object-Oriented Programming")
        @JsonProperty("title")
        String title,

        @Schema(description = "Short lesson description.", example = "Classes, objects, inheritance, and polymorphism.")
        @JsonProperty("description")
        String description,

        @Schema(description = "What the learner will be able to do after the lesson.")
        @JsonProperty("learning_objectives")
        String learningObjectives,

        @Schema(description = "Number of content items in the lesson. Shown even in the preview so schools can gauge depth.", example = "4")
        @JsonProperty("content_count")
        int contentCount,

        @Schema(description = "Full lesson content. Only present when the caller has full read access.")
        @JsonProperty("contents")
        List<LessonContentDTO> contents
) {
}
