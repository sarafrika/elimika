package apps.sarafrika.elimika.course.config.exception;

public class LessonNotFoundException extends RuntimeException {
    public LessonNotFoundException(String message) {
        super(message);
    }
}
