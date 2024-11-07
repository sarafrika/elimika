package apps.sarafrika.elimika.course.config;

import apps.sarafrika.elimika.course.config.exception.*;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
class CourseExceptionHandler {

    @ExceptionHandler(CourseNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ResponseDTO<Void>> handleCourseNotFoundException(CourseNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now()));
    }

    @ExceptionHandler(LessonNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ResponseDTO<Void>> handleLessonNotFoundException(LessonNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now()));
    }

    @ExceptionHandler(PrerequisiteTypeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handlePrerequisiteTypeNotFoundException(PrerequisiteTypeNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(PrerequisiteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handlePrerequisiteNotFoundException(PrerequisiteNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(PrerequisiteValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ResponseDTO<Void> handlePrerequisiteValidationException(PrerequisiteValidationException e) {

        return new ResponseDTO<>(null, HttpStatus.UNPROCESSABLE_ENTITY.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(PrerequisiteGroupNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handlePrerequisiteGroupNotFoundException(PrerequisiteGroupNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(LearningMaterialNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handleLearningMaterialNotFoundException(LearningMaterialNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(CoursePricingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handleCoursePricingNotFoundException(CoursePricingNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }
}

