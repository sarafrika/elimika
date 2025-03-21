package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.ContentTypeDTO;
import apps.sarafrika.elimika.course.model.ContentType;

/**
 * Utility class for mapping between ContentType entities and DTOs
 */
public class ContentTypeMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private ContentTypeMapper() {
    }

    /**
     * Converts a ContentType entity to a ContentTypeDTO
     *
     * @param entity the ContentType entity to convert
     * @return the corresponding ContentTypeDTO
     */
    public static ContentTypeDTO toDto(ContentType entity) {
        if (entity == null) {
            return null;
        }

        return new ContentTypeDTO(
                entity.getUuid(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate()
        );
    }

    /**
     * Converts a ContentTypeDTO to a ContentType entity
     *
     * @param dto the ContentTypeDTO to convert
     * @return the corresponding ContentType entity
     */
    public static ContentType toEntity(ContentTypeDTO dto) {
        if (dto == null) {
            return null;
        }

        ContentType contentType = new ContentType();
        contentType.setUuid(dto.uuid());
        contentType.setName(dto.name());
        contentType.setDescription(dto.description());

        return contentType;
    }
}