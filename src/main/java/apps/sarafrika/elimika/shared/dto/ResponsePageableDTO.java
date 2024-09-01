package apps.sarafrika.elimika.shared.dto;

import java.util.List;

public record ResponsePageableDTO<T>(
        List<T> data,
        int page,
        int size,
        int totalPages,
        long totalElements,
        int status,
        String message
) {
}
