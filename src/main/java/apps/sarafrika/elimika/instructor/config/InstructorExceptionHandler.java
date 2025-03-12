package apps.sarafrika.elimika.instructor.config;

import apps.sarafrika.elimika.instructor.config.exception.InstructorAvailabilityNotFoundException;
import apps.sarafrika.elimika.instructor.config.exception.InstructorNotFoundException;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class InstructorExceptionHandler {

    @ExceptionHandler(InstructorNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ResponseDTO<Void>> handleInstructorNotFoundException(InstructorNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now()));
    }

    @ExceptionHandler(InstructorAvailabilityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ResponseDTO<Void>> handleInstructorAvailabilityNotFoundException(InstructorAvailabilityNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now()));
    }

}

