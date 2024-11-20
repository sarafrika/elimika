package apps.sarafrika.elimika.assessment.dto.request;

import java.util.Date;

public record CreateAssessmentRequestDTO(
        String title,

        String type,

        String description,

        int maximumScore,

        int passingScore,

        Date dueDate,

        int timeLimit,

        Long courseId,

        Long lessonId
) {
}
