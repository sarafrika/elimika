package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.request.UpdateContentTypeDTO;
import apps.sarafrika.elimika.course.model.ContentType;

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
