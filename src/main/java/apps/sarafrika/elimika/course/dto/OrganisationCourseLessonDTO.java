package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * A single lesson as seen by an organisation viewing a course.
 * <p>
 * When the organisation is not approved to train the course, only the outline is
 * returned ({@code uuid} and {@code contents} are omitted) so no lesson content leaks.
 * Once approved, the lesson {@code uuid} and full {@code contents} are included.
 */
@Schema(name = "OrganisationCourseLesson", description = "Lesson outline (always) plus content (only when the organisation is approved to train).")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganisationCourseLessonDTO(

        @Schema(description = "Lesson identifier. Only present when the organisation has full read access.", accessMode = Schema.AccessMode.READ_ONLY)
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

        @Schema(description = "Full lesson content. Only present when the organisation has full read access.")
        @JsonProperty("contents")
        List<LessonContentDTO> contents
) {
}
