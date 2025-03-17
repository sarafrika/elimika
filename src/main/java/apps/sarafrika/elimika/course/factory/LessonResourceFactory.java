package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.request.UpdateLessonResourceRequestDTO;
import apps.sarafrika.elimika.course.model.LessonResource;

public class LessonResourceFactory {

    public static LessonResource create(CreateLessonResourceRequestDTO createLessonResourceRequestDTO) {

        return LessonResource.builder()
                .title(createLessonResourceRequestDTO.title())
                .resourceUrl(createLessonResourceRequestDTO.resourceUrl())
                .displayOrder(createLessonResourceRequestDTO.displayOrder())
                .build();
    }

    public static void update(LessonResource lessonResource, UpdateLessonResourceRequestDTO updateLessonResourceRequestDTO) {

        lessonResource.setTitle(updateLessonResourceRequestDTO.title());
        lessonResource.setResourceUrl(updateLessonResourceRequestDTO.resourceUrl());
        lessonResource.setDisplayOrder(updateLessonResourceRequestDTO.displayOrder());
    }
}
