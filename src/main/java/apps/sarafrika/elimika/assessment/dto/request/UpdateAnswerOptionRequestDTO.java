package apps.sarafrika.elimika.assessment.dto.request;

public record UpdateAnswerOptionRequestDTO(
        String optionText,
        boolean correct,
        int orderInQuestion
) {
}
