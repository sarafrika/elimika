package apps.sarafrika.elimika.assessment.web;

import apps.sarafrika.elimika.assessment.dto.request.CreateAssessmentRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateAssessmentRequestDTO;
import apps.sarafrika.elimika.assessment.dto.response.AssessmentResponseDTO;
import apps.sarafrika.elimika.assessment.service.AssessmentService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = AssessmentController.ROOT_PATH)
class AssessmentController {

    protected static final String ROOT_PATH = "api/v1/assessments";

    private final AssessmentService assessmentService;

    @GetMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<AssessmentResponseDTO> getAssessment(@PathVariable Long id) {

        return assessmentService.findAssessment(id);
    }

    @GetMapping(path = "/course/{courseId}")
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<AssessmentResponseDTO> getAssessmentsByCourse(@PathVariable Long courseId, Pageable pageable) {

        return assessmentService.findAssessmentsByCourse(courseId, pageable);
    }

    @GetMapping(path = "/lesson/{lessonId}")
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<AssessmentResponseDTO> getAssessmentsByLesson(@PathVariable Long lessonId, Pageable pageable) {

        return assessmentService.findAssessmentsByLesson(lessonId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createAssessment(@RequestBody CreateAssessmentRequestDTO createAssessmentRequestDTO) {

        return assessmentService.createAssessment(createAssessmentRequestDTO);
    }

    @PutMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateAssessment(@PathVariable Long id, @RequestBody UpdateAssessmentRequestDTO updateAssessmentRequestDTO) {

        return assessmentService.updateAssessment(id, updateAssessmentRequestDTO);
    }

    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAssessment(@PathVariable Long id) {

        assessmentService.deleteAssessment(id);
    }

}
