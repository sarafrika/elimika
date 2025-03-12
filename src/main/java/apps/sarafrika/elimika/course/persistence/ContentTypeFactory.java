package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CreateContentTypeDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateContentTypeDTO;

public class ContentTypeFactory {

    public static ContentType create(CreateContentTypeDTO createContentTypeDTO) {

        return ContentType.builder()
                .name(createContentTypeDTO.name())
                .description(createContentTypeDTO.description())
                .build();
    }

    public static void update(ContentType contentType, UpdateContentTypeDTO updateContentTypeDTO) {

        contentType.setName(updateContentTypeDTO.name());
        contentType.setDescription(updateContentTypeDTO.description());
    }
}
