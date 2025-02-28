package apps.sarafrika.elimika.common.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter @Setter
public class PageMetadata {
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;
    private boolean hasNext;
    private boolean hasPrevious;

    public static PageMetadata from(Page<?> page) {
        PageMetadata metadata = new PageMetadata();
        metadata.setPageNumber(page.getNumber());
        metadata.setPageSize(page.getSize());
        metadata.setTotalElements(page.getTotalElements());
        metadata.setTotalPages(page.getTotalPages());
        metadata.setFirst(page.isFirst());
        metadata.setLast(page.isLast());
        metadata.setHasNext(page.hasNext());
        metadata.setHasPrevious(page.hasPrevious());
        return metadata;
    }
}
