package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * The {@code course_content} index document: one lesson, lesson content item, quiz or assignment.
 * <p>
 * Deliberately narrow. It never carries quiz questions or options (the answers), rubric data,
 * submissions or file URLs: global search builds its results from stored fields alone, so anything
 * stored here is one scope mistake away from a learner.
 *
 * @param type      {@code lesson}, {@code content}, {@code quiz} or {@code assignment}
 * @param body      description plus learning objectives, content text or instructions, tags stripped
 *                  and truncated
 * @param scope     {@code COURSE} for course material, {@code CLASS} for a class-specific clone
 * @param published whether an enrolled learner may see the item: its lesson is published and active
 *                  and, for a quiz or assignment, the item itself is published too (as in
 *                  {@code LearnerMaterialScope})
 * @param updatedAt last change, UTC epoch seconds
 */
public record CourseContentSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("type") String type,
        @JsonProperty("course_uuid") UUID courseUuid,
        @JsonProperty("lesson_uuid") UUID lessonUuid,
        @JsonProperty("lesson_number") Integer lessonNumber,
        @JsonProperty("lesson_title") String lessonTitle,
        @JsonProperty("course_name") String courseName,
        @JsonProperty("title") String title,
        @JsonProperty("body") String body,
        @JsonProperty("content_type") String contentType,
        @JsonProperty("scope") String scope,
        @JsonProperty("class_definition_uuid") UUID classDefinitionUuid,
        @JsonProperty("published") boolean published,
        @JsonProperty("display_order") Integer displayOrder,
        @JsonProperty("updated_at") Long updatedAt
) implements SearchDocument {
}
