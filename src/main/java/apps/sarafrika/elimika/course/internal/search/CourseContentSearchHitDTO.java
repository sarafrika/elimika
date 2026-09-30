package apps.sarafrika.elimika.course.internal.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * One in-course search result: enough to label it and open the item inside its lesson.
 *
 * @param type      {@code lesson}, {@code content}, {@code quiz} or {@code assignment}
 * @param uuid      the item's UUID (for a lesson, the lesson's)
 * @param highlight a short excerpt of the matched text with {@code <em>} markers, or {@code null}
 */
@Schema(name = "CourseContentSearchHit", description = "A lesson, content item, quiz or assignment matching an in-course search")
public record CourseContentSearchHitDTO(
        @JsonProperty("type") String type,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("lesson_uuid") UUID lessonUuid,
        @JsonProperty("lesson_number") Integer lessonNumber,
        @JsonProperty("lesson_title") String lessonTitle,
        @JsonProperty("title") String title,
        @JsonProperty("highlight") String highlight
) {
}
