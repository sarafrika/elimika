package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.ContentType;

public record ContentTypeResponseDTO(Long id, String name, String description) {

    public static ContentTypeResponseDTO from(ContentType contentType) {

        return new ContentTypeResponseDTO(contentType.getId(), contentType.getName(), contentType.getDescription());
    }
}
