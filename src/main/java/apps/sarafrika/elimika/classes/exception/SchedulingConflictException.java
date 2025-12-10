package apps.sarafrika.elimika.classes.exception;

import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;

import java.util.List;

public class SchedulingConflictException extends RuntimeException {

    private final List<ClassSchedulingConflictDTO> conflicts;

    public SchedulingConflictException(String message, List<ClassSchedulingConflictDTO> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    public List<ClassSchedulingConflictDTO> getConflicts() {
        return conflicts;
    }
}
