package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.shared.enums.Gender;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * The directory projection of a user: what it takes to draw somebody on screen, and nothing else.
 * <p>
 * {@link UserDTO} carries email, phone number, date of birth, Keycloak id, audit columns and the
 * caller's organisation affiliations. A roster row, an avatar, a calendar chip or an instructor card
 * needs none of that — it needs a name, a picture and an identifier. Splitting the two is what lets
 * the batch lookup route sit at the authenticated baseline without handing every signed-in caller
 * the platform's contact list.
 * <p>
 * {@code gender} is the one field here that is not a pure display attribute. It is carried because
 * the student-facing instructor search filters on it (Male / Female / Other) and it lives on the
 * user row, so including it costs no extra query; it is demographic rather than contact information,
 * so it does not reopen what this DTO exists to close. Nothing else should be added without the same
 * kind of argument.
 * <p>
 * Deliberately absent: {@code user_domain}. Resolving it costs two queries plus one lookup per
 * domain mapping <em>per user</em> — precisely the per-row fan-out a batch endpoint exists to avoid.
 *
 * @param uuid            the user's identifier, and the key callers index the batch response by
 * @param userNo          the human-facing account number shown on admissions and payment records
 * @param firstName       given name
 * @param middleName      middle name, folded into {@code full_name}; null or blank when unset
 * @param lastName        family name
 * @param profileImageUrl resolved public avatar URL, or {@code null} when none is set
 * @param gender          demographic marker used by the instructor search filters
 */
@Schema(
        name = "UserSummary",
        description = "Reduced user projection for directory lookups: display identity only, no contact details",
        example = """
                {
                    "uuid": "d2e6f6c4-3d44-11ee-be56-0242ac120002",
                    "user_no": "123456789",
                    "first_name": "Jane",
                    "middle_name": "A.",
                    "last_name": "Doe",
                    "full_name": "Jane A. Doe",
                    "display_name": "Jane Doe",
                    "profile_image_url": "https://example.com/images/jane.jpg",
                    "gender": "FEMALE"
                }
                """
)
public record UserSummaryDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique system identifier for the user.",
                example = "d2e6f6c4-3d44-11ee-be56-0242ac120002",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(
                description = "**[READ-ONLY]** Unique numeric identifier used for payments and admissions.",
                example = "123456789",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("user_no")
        String userNo,

        @Schema(
                description = "**[READ-ONLY]** User's given/first name.",
                example = "Jane",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("first_name")
        String firstName,

        @Schema(
                description = "**[READ-ONLY]** User's middle name or initial. Null when not recorded.",
                example = "A.",
                nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("middle_name")
        String middleName,

        @Schema(
                description = "**[READ-ONLY]** User's family/last name.",
                example = "Doe",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("last_name")
        String lastName,

        @Schema(
                description = "**[READ-ONLY]** Public URL of the user's avatar. Null when no image has been uploaded.",
                example = "https://example.com/images/jane.jpg",
                format = "uri",
                nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("profile_image_url")
        String profileImageUrl,

        @Schema(
                description = "**[READ-ONLY]** User's gender. Consumed by the instructor search filters. "
                        + "Null when not disclosed.",
                example = "FEMALE",
                allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"},
                nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("gender")
        Gender gender

) {

    /**
     * Mirrors {@link UserDTO#getFullName()} so a consumer swapping a search response for a directory
     * response reads the same field under the same name.
     */
    @Schema(
            description = "**[READ-ONLY]** Full name including the middle name when one is recorded.",
            example = "Jane A. Doe",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @JsonProperty(value = "full_name", access = JsonProperty.Access.READ_ONLY)
    public String getFullName() {
        return join(firstName, middleName, lastName);
    }

    /**
     * Mirrors {@link UserDTO#getDisplayName()}: first and last name only.
     */
    @Schema(
            description = "**[READ-ONLY]** Display name for UI purposes: first and last name only.",
            example = "Jane Doe",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @JsonProperty(value = "display_name", access = JsonProperty.Access.READ_ONLY)
    public String getDisplayName() {
        return join(firstName, lastName);
    }

    /**
     * Skips absent parts instead of leaving the double spaces {@link UserDTO} produces when a name
     * component is null. A directory row is rendered verbatim, so the spacing matters here.
     */
    private static String join(String... parts) {
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!joined.isEmpty()) {
                joined.append(' ');
            }
            joined.append(part.trim());
        }
        return joined.toString();
    }
}
