package apps.sarafrika.elimika.assessment.service;

import apps.sarafrika.elimika.assessment.dto.request.CreateAssessmentRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateAssessmentRequestDTO;
import apps.sarafrika.elimika.assessment.dto.response.AssessmentResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface AssessmentService {

    ResponsePageableDTO<AssessmentResponseDTO> findAssessmentsByCourse(Long courseId, Pageable pageable);

    ResponsePageableDTO<AssessmentResponseDTO> findAssessmentsByLesson(Long lessonId, Pageable pageable);

    ResponseDTO<AssessmentResponseDTO> findAssessment(Long id);

    ResponseDTO<Void> createAssessment(CreateAssessmentRequestDTO createAssessmentRequestDTO);

    ResponseDTO<Void> updateAssessment(Long id, UpdateAssessmentRequestDTO updateAssessmentRequestDTO);

    void deleteAssessment(Long id);
}
