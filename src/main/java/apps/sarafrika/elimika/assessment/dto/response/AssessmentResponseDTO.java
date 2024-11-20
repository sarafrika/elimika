package apps.sarafrika.elimika.assessment.dto.response;

import apps.sarafrika.elimika.assessment.persistence.Assessment;

import java.util.Date;

public record AssessmentResponseDTO(
        Long id,
        String title,
        String type,
        String description,
        int maximumScore,
        int passingScore,
        Date dueDate,
        int timeLimit
) {

    public static AssessmentResponseDTO from(Assessment assessment) {

        return new AssessmentResponseDTO(
                assessment.getId(),
                assessment.getTitle(),
                assessment.getType(),
                assessment.getDescription(),
                assessment.getMaximumScore(),
                assessment.getPassingScore(),
                assessment.getDueDate(),
                assessment.getTimeLimit()
        );
    }
}
