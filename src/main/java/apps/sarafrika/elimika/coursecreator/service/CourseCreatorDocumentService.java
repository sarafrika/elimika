package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDocumentDTO;

import java.util.List;
import java.util.UUID;

public interface CourseCreatorDocumentService {

    CourseCreatorDocumentDTO createCourseCreatorDocument(CourseCreatorDocumentDTO documentDTO);

    CourseCreatorDocumentDTO getCourseCreatorDocumentByUuid(UUID uuid);

    List<CourseCreatorDocumentDTO> getDocumentsByCourseCreatorUuid(UUID courseCreatorUuid);

    /**
     * Returns the documents of a course creator that the current caller is entitled to see.
     * The owning course creator and platform admins see every document; any other authenticated
     * caller sees only admin-verified documents, which is what the public profile renders.
     */
    List<CourseCreatorDocumentDTO> getVisibleDocumentsByCourseCreatorUuid(UUID courseCreatorUuid);

    /**
     * Updates a document that belongs to the given course creator. The document is looked up within
     * that course creator so a caller authorised for their own profile cannot reach another creator's
     * document by its UUID.
     */
    CourseCreatorDocumentDTO updateCourseCreatorDocument(UUID courseCreatorUuid, UUID uuid, CourseCreatorDocumentDTO documentDTO);

    CourseCreatorDocumentDTO verifyCourseCreatorDocument(UUID uuid, String verifiedBy, String verificationNotes);

    /**
     * Deletes a document that belongs to the given course creator, scoped the same way as the update.
     */
    void deleteCourseCreatorDocument(UUID courseCreatorUuid, UUID uuid);
}
