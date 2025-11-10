package apps.sarafrika.elimika.systemconfig.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgeGateConfig(
        Integer minAge,
        Integer maxAge,
        Set<String> allowedRegions,
        Set<String> blockedRegions,
        Set<String> allowedDemographics,
        Set<String> blockedDemographics
) {
    public Set<String> allowedRegions() {
        return allowedRegions == null ? Collections.emptySet() : allowedRegions;
    }

    public Set<String> blockedRegions() {
        return blockedRegions == null ? Collections.emptySet() : blockedRegions;
    }

    public Set<String> allowedDemographics() {
        return allowedDemographics == null ? Collections.emptySet() : allowedDemographics;
    }

    public Set<String> blockedDemographics() {
        return blockedDemographics == null ? Collections.emptySet() : blockedDemographics;
    }
}
