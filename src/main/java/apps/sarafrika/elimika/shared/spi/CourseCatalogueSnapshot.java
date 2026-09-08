package apps.sarafrika.elimika.shared.spi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The public face of a course, as a catalogue listing needs it.
 * <p>
 * Deliberately a narrow projection rather than the course record. A catalogue entry is served to
 * anonymous browsers, so this carries only what a course card shows — what it is called, what it
 * covers, how long it runs, who wrote it, what it costs — and none of the commercial terms behind
 * it. {@code minimum_training_fee}, the creator and instructor share percentages and the
 * revenue-share notes are the course owner's business and are absent by construction: there is no
 * field here to leak them through.
 *
 * @param uuid                  the course
 * @param name                  display title
 * @param description           rich-text blurb, as authored
 * @param thumbnailUrl          storage key, not a resolved URL
 * @param durationHours         hours component of the advertised duration
 * @param durationMinutes       minutes component of the advertised duration
 * @param categoryNames         the disciplines it is filed under
 * @param price                 list price, the same figure the catalogue entry sells at
 * @param ageLowerLimit         who the course is for, lower bound, null when unrestricted
 * @param ageUpperLimit         who the course is for, upper bound, null when unrestricted
 * @param published             whether the course itself is published
 * @param acceptsNewEnrollments whether it can currently be enrolled on
 * @param creatorUuid           the course creator
 * @param creatorName           the creator's display name, resolved so the caller needs no second
 *                              lookup — the whole point of this projection
 */
public record CourseCatalogueSnapshot(
        @JsonProperty("uuid")
        UUID uuid,
        @JsonProperty("name")
        String name,
        @JsonProperty("description")
        String description,
        @JsonProperty("thumbnail_url")
        String thumbnailUrl,
        @JsonProperty("duration_hours")
        Integer durationHours,
        @JsonProperty("duration_minutes")
        Integer durationMinutes,
        @JsonProperty("category_names")
        List<String> categoryNames,
        @JsonProperty("price")
        BigDecimal price,
        @JsonProperty("age_lower_limit")
        Integer ageLowerLimit,
        @JsonProperty("age_upper_limit")
        Integer ageUpperLimit,
        @JsonProperty("published")
        boolean published,
        @JsonProperty("accepts_new_enrollments")
        boolean acceptsNewEnrollments,
        @JsonProperty("creator_uuid")
        UUID creatorUuid,
        @JsonProperty("creator_name")
        String creatorName
) {
}
