package apps.sarafrika.elimika.assessment.dto.request;

public record CreateAnswerOptionRequestDTO(
        String optionText,
        boolean correct,
        int orderInQuestion
) {
}
