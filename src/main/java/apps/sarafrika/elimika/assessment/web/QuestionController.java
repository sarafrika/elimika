package apps.sarafrika.elimika.assessment.web;

import apps.sarafrika.elimika.assessment.dto.request.CreateQuestionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateQuestionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.response.QuestionResponseDTO;
import apps.sarafrika.elimika.assessment.service.QuestionService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = QuestionController.ROOT_PATH)
public class QuestionController {

    protected static final String ROOT_PATH = "api/v1/assessments/{assessmentId}/questions";

    private final QuestionService questionService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<QuestionResponseDTO> getQuestions(final Pageable pageable, @PathVariable Long assessmentId) {

        return questionService.findQuestionsByAssessment(assessmentId, pageable);
    }

    @GetMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<QuestionResponseDTO> getQuestion(@PathVariable Long assessmentId, @PathVariable Long id) {

        return questionService.findQuestion(assessmentId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createQuestion(@RequestBody final CreateQuestionRequestDTO question, @PathVariable Long assessmentId) {

        return questionService.createQuestion(assessmentId, question);
    }

    @PutMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateQuestion(@RequestBody final UpdateQuestionRequestDTO question, @PathVariable Long assessmentId, @PathVariable Long id) {

        return questionService.updateQuestion(assessmentId, question, id);
    }

    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteQuestion(@PathVariable Long assessmentId, @PathVariable Long id) {

        questionService.deleteQuestion(assessmentId, id);
    }

}
