package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDocumentDTO;

import java.util.List;
import java.util.UUID;

public interface CourseCreatorDocumentService {

    CourseCreatorDocumentDTO createCourseCreatorDocument(CourseCreatorDocumentDTO documentDTO);

    CourseCreatorDocumentDTO getCourseCreatorDocumentByUuid(UUID uuid);

    List<CourseCreatorDocumentDTO> getDocumentsByCourseCreatorUuid(UUID courseCreatorUuid);

    CourseCreatorDocumentDTO updateCourseCreatorDocument(UUID uuid, CourseCreatorDocumentDTO documentDTO);

    void deleteCourseCreatorDocument(UUID uuid);
}
