package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.internal.config.InternalCommerceProperties;
import apps.sarafrika.elimika.commerce.internal.service.RegionResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegionResolverImpl implements RegionResolver {

    private final InternalCommerceProperties internalCommerceProperties;

    @Override
    public String resolveRegionCode(String requestedRegion) {
        String configured = normalize(internalCommerceProperties.getDefaultRegion());
        if (StringUtils.hasText(configured)) {
            return configured;
        }
        return normalize(requestedRegion);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
}
