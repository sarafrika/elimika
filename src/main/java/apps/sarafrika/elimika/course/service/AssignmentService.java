package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.AssignmentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface AssignmentService {
    AssignmentDTO createAssignment(AssignmentDTO assignmentDTO);

    AssignmentDTO getAssignmentByUuid(UUID uuid);

    Page<AssignmentDTO> getAllAssignments(Pageable pageable);

    AssignmentDTO updateAssignment(UUID uuid, AssignmentDTO assignmentDTO);

    void deleteAssignment(UUID uuid);

    Page<AssignmentDTO> search(Map<String, String> searchParams, Pageable pageable);
}