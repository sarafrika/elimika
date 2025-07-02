package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.AssignmentSubmissionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface AssignmentSubmissionService {
    AssignmentSubmissionDTO createAssignmentSubmission(AssignmentSubmissionDTO assignmentSubmissionDTO);

    AssignmentSubmissionDTO getAssignmentSubmissionByUuid(UUID uuid);

    Page<AssignmentSubmissionDTO> getAllAssignmentSubmissions(Pageable pageable);

    AssignmentSubmissionDTO updateAssignmentSubmission(UUID uuid, AssignmentSubmissionDTO assignmentSubmissionDTO);

    void deleteAssignmentSubmission(UUID uuid);

    Page<AssignmentSubmissionDTO> search(Map<String, String> searchParams, Pageable pageable);
}