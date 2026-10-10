package apps.sarafrika.elimika.shared.event.notification;

import java.util.UUID;

/**
 * Notification action URLs for the current role-scoped dashboard routes.
 * Every link names the recipient's dashboard so a click never lands on a stale or foreign page.
 */
public final class DashboardLinks {

    public static final String DASHBOARD = "/dashboard";
    public static final String STUDENT_SCHEDULE = "/dashboard/student/schedule";
    public static final String STUDENT_WALLET = "/dashboard/student/wallet";
    public static final String PARENT_BILLING = "/dashboard/parent/billing";
    public static final String INSTRUCTOR_TRAINING_HUB = "/dashboard/instructor/training-hub";
    public static final String COURSE_CREATOR_COURSES = "/dashboard/course-creator/courses";

    private DashboardLinks() {
    }

    public static String studentClass(UUID classDefinitionUuid) {
        return classDefinitionUuid == null
                ? STUDENT_SCHEDULE
                : STUDENT_SCHEDULE + "/classes/" + classDefinitionUuid;
    }

    public static String instructorClass(UUID classDefinitionUuid) {
        return classDefinitionUuid == null
                ? INSTRUCTOR_TRAINING_HUB
                : "/dashboard/instructor/classes/class-training/" + classDefinitionUuid;
    }

    public static String courseCreatorCourse(UUID courseUuid) {
        return courseUuid == null ? COURSE_CREATOR_COURSES : COURSE_CREATOR_COURSES + "/" + courseUuid;
    }

    public static String studentAssignment(UUID assignmentUuid) {
        return assignmentUuid == null
                ? "/dashboard/student/assessment/assignments"
                : "/dashboard/student/assignment/" + assignmentUuid;
    }
}
