package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * One card on the public catalogue page: a course or a programme, ranked together with the other.
 * Every field is always present; a value a type does not have is {@code null}.
 */
@Schema(name = "CatalogueItem", description = "A course or programme card on the public catalogue page.")
public record CatalogueItem(

        @Schema(description = "Result type.", allowableValues = {"course", "programme"}, example = "course")
        @JsonProperty("type")
        String type,

        @Schema(description = "Course or programme UUID.")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Course name or programme title.", example = "Python for data analysis")
        @JsonProperty("title")
        String title,

        @Schema(description = "Description as authored, truncated to 2000 characters.")
        @JsonProperty("description")
        String description,

        @Schema(description = "Public thumbnail URL (/api/v1/files/... or an external URL). A programme "
                + "shows its first member course's thumbnail. Null when there is none.")
        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @Schema(description = "Category names, alphabetical. A programme has at most one.")
        @JsonProperty("category_names")
        List<String> categoryNames,

        @Schema(description = "Category UUIDs, in the order of category_names.")
        @JsonProperty("category_uuids")
        List<UUID> categoryUuids,

        @Schema(description = "Course creator UUID.")
        @JsonProperty("creator_uuid")
        UUID creatorUuid,

        @Schema(description = "Course creator display name.", example = "Grace Hopper")
        @JsonProperty("creator_name")
        String creatorName,

        @Schema(description = "Course: its difficulty name. Programme: the range over its member courses "
                + "(\"Beginner → Advanced\", or one name when they agree); null when unknown.",
                example = "Beginner")
        @JsonProperty("level")
        String level,

        @Schema(description = "Average review rating 1-5, null when unrated.", example = "4.5")
        @JsonProperty("rating_avg")
        Double ratingAvg,

        @Schema(description = "Number of reviews.", example = "12")
        @JsonProperty("review_count")
        long reviewCount,

        @Schema(description = "Published lessons (programme: across its member courses).", example = "8")
        @JsonProperty("lesson_count")
        long lessonCount,

        @Schema(description = "Programme: member course count. Null for a course.", example = "3")
        @JsonProperty("course_count")
        Long courseCount,

        @Schema(description = "Distinct learners with an active or completed enrolment.", example = "120")
        @JsonProperty("learner_count")
        long learnerCount,

        @Schema(description = "Course: active public classes delivering it. Null for a programme.", example = "2")
        @JsonProperty("class_count")
        Long classCount,

        @Schema(description = "Course: the lowest class fee among its open classes (active, public, not full, "
                + "registration and teaching not ended) - what a learner actually pays. Null when it has no open class with a "
                + "fee, and always null for a programme.", example = "2500.00")
        @JsonProperty("price_from")
        BigDecimal priceFrom,

        @Schema(description = "Course: its open classes (active, public, not full, registration and teaching not ended). "
                + "Always 0 for a programme.", example = "2")
        @JsonProperty("open_class_count")
        long openClassCount,

        @Schema(description = "Age band label such as \"18+\" when the course's lower age limit is 18 or "
                + "more; null otherwise and for programmes.", example = "18+")
        @JsonProperty("age_label")
        String ageLabel,

        @Schema(description = "List price; null when not set.", example = "1500.00")
        @JsonProperty("price")
        BigDecimal price,

        @Schema(description = "True when the price is missing or zero.")
        @JsonProperty("is_free")
        boolean isFree,

        @Schema(description = "When q matched the title: the HTML-escaped title with matches wrapped in "
                + "<em>...</em> (the only markup). Null otherwise.", example = "<em>Python</em> for data analysis")
        @JsonProperty("highlight")
        String highlight
) {
}
