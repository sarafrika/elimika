package apps.sarafrika.elimika.shared.dto;

import java.util.List;

public record ResponsePageableDTO<T>(
        List<T> content,
        int page,
        int size,
        int totalPages,
        long totalElements
) {
}
