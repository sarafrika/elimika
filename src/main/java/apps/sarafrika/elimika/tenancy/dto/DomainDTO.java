package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record DomainDTO(

        @JsonProperty("uuid")
        UUID uuid,

        @JsonProperty("name")
        String name
) {
}
