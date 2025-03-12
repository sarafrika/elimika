package apps.sarafrika.elimika.course.config;

import apps.sarafrika.elimika.course.config.exception.*;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
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

    @ExceptionHandler(LessonResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handleLessonResourceNotFoundException(LessonResourceNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(CourseLearningObjectiveNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handleCourseLearningObjectiveNotFoundException(CourseLearningObjectiveNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handleCategoryNotFoundException(CategoryNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(CourseCategoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handleCourseCategoryNotFoundException(CourseCategoryNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(ContentTypeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handleContentTypeNotFoundException(ContentTypeNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(LessonContentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handleLessonContentNotFoundException(LessonContentNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ResponseDTO<Void> handleValidationException(ValidationException e) {

        return new ResponseDTO<>(null, HttpStatus.UNPROCESSABLE_ENTITY.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ResponseDTO<Void> handleException(Exception e) {

        String MESSAGE = "Unexpected error occurred";

        log.error(MESSAGE, e);

        return new ResponseDTO<>(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), MESSAGE, null, LocalDateTime.now());
    }
}

