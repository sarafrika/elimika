package apps.sarafrika.elimika.shared.spi;

import java.util.UUID;

/** Whether a learner's lesson can still be changed, given the session reports that cover it. */
public interface SessionReportLockLookup {

    /** True once a session teaching this lesson to the learner has a locked report. */
    boolean isLessonLocked(UUID studentUuid, UUID courseUuid, UUID lessonUuid);
}
