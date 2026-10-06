package apps.sarafrika.elimika.coursecreator.dto;

import apps.sarafrika.elimika.coursecreator.util.enums.PortfolioItemType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "CourseCreatorPortfolioItem", description = "Work a course creator shows in the Portfolio tab of the skills wallet")
public record CourseCreatorPortfolioItemDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty(value = "course_creator_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID courseCreatorUuid,

        @NotBlank
        @Size(max = 255)
        @JsonProperty("title")
        String title,

        @NotNull
        @JsonProperty("item_type")
        PortfolioItemType itemType,

        @Size(max = 2048)
        @JsonProperty("link_url")
        String linkUrl,

        @JsonProperty("completed_on")
        LocalDate completedOn,

        @JsonProperty("description")
        String description,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
