package apps.sarafrika.elimika.shared.event.notification;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardLinksTest {

    private final UUID id = UUID.randomUUID();

    @Test
    void classLinksTargetTheRecipientsDashboard() {
        assertThat(DashboardLinks.studentClass(id)).isEqualTo("/dashboard/student/schedule/classes/" + id);
        assertThat(DashboardLinks.instructorClass(id)).isEqualTo("/dashboard/instructor/classes/class-training/" + id);
        assertThat(DashboardLinks.courseCreatorCourse(id)).isEqualTo("/dashboard/course-creator/courses/" + id);
        assertThat(DashboardLinks.studentAssignment(id)).isEqualTo("/dashboard/student/assignment/" + id);
    }

    @Test
    void missingIdsFallBackToTheRoleLandingPage() {
        assertThat(DashboardLinks.studentClass(null)).isEqualTo(DashboardLinks.STUDENT_SCHEDULE);
        assertThat(DashboardLinks.instructorClass(null)).isEqualTo(DashboardLinks.INSTRUCTOR_TRAINING_HUB);
        assertThat(DashboardLinks.courseCreatorCourse(null)).isEqualTo(DashboardLinks.COURSE_CREATOR_COURSES);
        assertThat(DashboardLinks.studentAssignment(null)).isEqualTo("/dashboard/student/assessment/assignments");
    }
}
