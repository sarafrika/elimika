package apps.sarafrika.elimika.shared.dto;

import apps.sarafrika.elimika.shared.utils.PageLinks;
import apps.sarafrika.elimika.shared.utils.PageMetadata;
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