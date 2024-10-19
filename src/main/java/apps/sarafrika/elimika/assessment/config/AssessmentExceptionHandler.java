package apps.sarafrika.elimika.assessment.config;

import apps.sarafrika.elimika.assessment.config.exceptions.AnswerOptionNotFoundException;
import apps.sarafrika.elimika.assessment.config.exceptions.AssessmentNotFoundException;
import apps.sarafrika.elimika.assessment.config.exceptions.QuestionNotFoundException;
import apps.sarafrika.elimika.assessment.config.exceptions.QuestionValidationException;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@ControllerAdvice
public class AssessmentExceptionHandler {

    @ExceptionHandler(AssessmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ResponseDTO<Void>> handleAssessmentNotFoundException(AssessmentNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now()));
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ResponseDTO<Void>> handleQuestionNotFoundException(QuestionNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now()));
    }

    @ExceptionHandler(QuestionValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ResponseEntity<ResponseDTO<Void>> handleQuestionValidationException(QuestionValidationException e) {

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ResponseDTO<>(null, HttpStatus.UNPROCESSABLE_ENTITY.value(), e.getMessage(), null, LocalDateTime.now()));
    }

    @ExceptionHandler(AnswerOptionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ResponseDTO<Void>> handleAnswerOptionNotFoundException(AnswerOptionNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now()));
    }
}
