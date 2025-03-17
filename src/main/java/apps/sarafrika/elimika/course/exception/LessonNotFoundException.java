package apps.sarafrika.elimika.course.exception;

public class LessonNotFoundException extends RuntimeException {
    public LessonNotFoundException(String message) {
        super(message);
    }
}
