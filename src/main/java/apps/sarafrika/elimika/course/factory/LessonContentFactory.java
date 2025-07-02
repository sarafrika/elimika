package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.LessonContentDTO;
import apps.sarafrika.elimika.course.model.LessonContent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LessonContentFactory {

    // Convert LessonContent entity to LessonContentDTO
    public static LessonContentDTO toDTO(LessonContent lessonContent) {
        if (lessonContent == null) {
            return null;
        }
        return new LessonContentDTO(
                lessonContent.getUuid(),
                lessonContent.getLessonUuid(),
                lessonContent.getContentTypeUuid(),
                lessonContent.getTitle(),
                lessonContent.getDescription(),
                lessonContent.getContentText(),
                lessonContent.getFileUrl(),
                lessonContent.getFileSizeBytes(),
                lessonContent.getMimeType(),
                lessonContent.getDisplayOrder(),
                lessonContent.getIsRequired(),
                lessonContent.getCreatedDate(),
                lessonContent.getCreatedBy(),
                lessonContent.getLastModifiedDate(),
                lessonContent.getLastModifiedBy()
        );
    }

    // Convert LessonContentDTO to LessonContent entity
    public static LessonContent toEntity(LessonContentDTO dto) {
        if (dto == null) {
            return null;
        }
        LessonContent lessonContent = new LessonContent();
        lessonContent.setUuid(dto.uuid());
        lessonContent.setLessonUuid(dto.lessonUuid());
        lessonContent.setContentTypeUuid(dto.contentTypeUuid());
        lessonContent.setTitle(dto.title());
        lessonContent.setDescription(dto.description());
        lessonContent.setContentText(dto.contentText());
        lessonContent.setFileUrl(dto.fileUrl());
        lessonContent.setFileSizeBytes(dto.fileSizeBytes());
        lessonContent.setMimeType(dto.mimeType());
        lessonContent.setDisplayOrder(dto.displayOrder());
        lessonContent.setIsRequired(dto.isRequired());
        lessonContent.setCreatedDate(dto.createdDate());
        lessonContent.setCreatedBy(dto.createdBy());
        lessonContent.setLastModifiedDate(dto.updatedDate());
        lessonContent.setLastModifiedBy(dto.updatedBy());
        return lessonContent;
    }
}