package apps.sarafrika.elimika.student.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public record StudentDTO(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("user_uuid") UUID userUuid,
        @JsonProperty("first_guardian_name") String firstGuardianName,
        @JsonProperty("first_guardian_mobile") String firstGuardianMobile,
        @JsonProperty("second_guardian_name") String secondGuardianName,
        @JsonProperty("second_guardian_mobile") String secondGuardianMobile,
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime createdDate,
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY) String createdBy,
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate,
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY) String updatedBy
) {}
