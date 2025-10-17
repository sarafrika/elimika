package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseTrainingRequirementDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CourseTrainingRequirementService {

    CourseTrainingRequirementDTO create(UUID courseUuid, CourseTrainingRequirementDTO dto);

    CourseTrainingRequirementDTO update(UUID courseUuid, UUID requirementUuid, CourseTrainingRequirementDTO dto);

    void delete(UUID courseUuid, UUID requirementUuid);

    CourseTrainingRequirementDTO getByUuid(UUID courseUuid, UUID requirementUuid);

    List<CourseTrainingRequirementDTO> findByCourseUuid(UUID courseUuid);

    Page<CourseTrainingRequirementDTO> search(Map<String, String> searchParams, Pageable pageable);
}
