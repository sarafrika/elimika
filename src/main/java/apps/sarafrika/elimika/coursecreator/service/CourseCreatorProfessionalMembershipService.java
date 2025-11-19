package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorProfessionalMembershipDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseCreatorProfessionalMembershipService {
    CourseCreatorProfessionalMembershipDTO createCourseCreatorProfessionalMembership(CourseCreatorProfessionalMembershipDTO dto);
    CourseCreatorProfessionalMembershipDTO getCourseCreatorProfessionalMembershipByUuid(UUID uuid);
    Page<CourseCreatorProfessionalMembershipDTO> getAllCourseCreatorProfessionalMemberships(Pageable pageable);
    CourseCreatorProfessionalMembershipDTO updateCourseCreatorProfessionalMembership(UUID uuid, CourseCreatorProfessionalMembershipDTO dto);
    void deleteCourseCreatorProfessionalMembership(UUID uuid);
    Page<CourseCreatorProfessionalMembershipDTO> search(Map<String, String> searchParams, Pageable pageable);
}
