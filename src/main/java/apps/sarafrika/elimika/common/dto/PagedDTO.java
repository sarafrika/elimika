package apps.sarafrika.elimika.common.dto;

import apps.sarafrika.elimika.common.util.PageLinks;
import apps.sarafrika.elimika.common.util.PageMetadata;
import org.springframework.data.domain.Page;

import java.util.List;

public record PagedDTO<T>(
        List<T> content,
        PageMetadata metadata,
        PageLinks links
) {
    public static <T> PagedDTO<T> from(Page<T> page, String baseUrl) {
        return new PagedDTO<>(
                page.getContent(),
                PageMetadata.from(page),
                PageLinks.from(page, baseUrl)
        );
    }
}