package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDocumentDTO;

import java.util.List;
import java.util.UUID;

public interface CourseCreatorDocumentService {
    CourseCreatorDocumentDTO createCourseCreatorDocument(CourseCreatorDocumentDTO documentDTO);
    CourseCreatorDocumentDTO getCourseCreatorDocumentByUuid(UUID uuid);
    List<CourseCreatorDocumentDTO> getDocumentsByCourseCreatorUuid(UUID courseCreatorUuid);

    /** The owner and platform admins see every document; anyone else only the admin-verified ones. */
    List<CourseCreatorDocumentDTO> getVisibleDocumentsByCourseCreatorUuid(UUID courseCreatorUuid);

    /** Scoped to the course creator, so a foreign document reads as not found. */
    CourseCreatorDocumentDTO updateCourseCreatorDocument(UUID courseCreatorUuid, UUID uuid, CourseCreatorDocumentDTO documentDTO);
    CourseCreatorDocumentDTO verifyCourseCreatorDocument(UUID uuid, String verifiedBy, String verificationNotes);
    void deleteCourseCreatorDocument(UUID courseCreatorUuid, UUID uuid);
    boolean isDocumentFileOf(UUID courseCreatorUuid, String filePath);
}
