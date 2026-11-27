package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "CategoryInterests", description = "The course categories the caller picked for one domain, in the order saved")
public record CategoryInterestsDTO(
        @JsonProperty("domain") UserDomain domain,
        @JsonProperty("categories") List<Category> categories
) {

    @Schema(name = "CategoryInterest")
    public record Category(
            @JsonProperty("category_uuid") UUID categoryUuid,
            @JsonProperty("name") String name
    ) {
    }
}
