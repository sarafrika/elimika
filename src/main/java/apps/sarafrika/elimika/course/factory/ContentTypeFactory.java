package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.ContentTypeDTO;
import apps.sarafrika.elimika.course.model.ContentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentTypeFactory {

    // Convert ContentType entity to ContentTypeDTO
    public static ContentTypeDTO toDTO(ContentType contentType) {
        if (contentType == null) {
            return null;
        }
        return new ContentTypeDTO(
                contentType.getUuid(),
                contentType.getName(),
                contentType.getMimeTypes(),
                contentType.getMaxFileSizeMb(),
                contentType.getCreatedDate(),
                contentType.getCreatedBy(),
                contentType.getLastModifiedDate(),
                contentType.getLastModifiedBy()
        );
    }

    // Convert ContentTypeDTO to ContentType entity
    public static ContentType toEntity(ContentTypeDTO dto) {
        if (dto == null) {
            return null;
        }
        ContentType contentType = new ContentType();
        contentType.setUuid(dto.uuid());
        contentType.setName(dto.name());
        contentType.setMimeTypes(dto.mimeTypes());
        contentType.setMaxFileSizeMb(dto.maxFileSizeMb());
        contentType.setCreatedDate(dto.createdDate());
        contentType.setCreatedBy(dto.createdBy());
        contentType.setLastModifiedDate(dto.updatedDate());
        contentType.setLastModifiedBy(dto.updatedBy());
        return contentType;
    }
}