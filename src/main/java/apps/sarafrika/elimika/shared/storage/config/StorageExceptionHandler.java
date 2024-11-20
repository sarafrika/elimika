package apps.sarafrika.elimika.shared.storage.config;

import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.storage.config.exception.StorageException;
import apps.sarafrika.elimika.shared.storage.config.exception.StorageFileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class StorageExceptionHandler {

    @ExceptionHandler(StorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ResponseDTO<Void> handleStorageException(StorageException e) {

        return new ResponseDTO<>(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), null, LocalDateTime.now());
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseDTO<Void> handleStorageFileNotFoundException(StorageFileNotFoundException e) {

        return new ResponseDTO<>(null, HttpStatus.NOT_FOUND.value(), e.getMessage(), null, LocalDateTime.now());
    }
}
