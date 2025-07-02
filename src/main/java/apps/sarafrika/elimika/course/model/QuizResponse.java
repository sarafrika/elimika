package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_responses")
public class QuizResponse extends BaseEntity {

    @Column(name = "attempt_uuid")
    private UUID attemptUuid;

    @Column(name = "question_uuid")
    private UUID questionUuid;

    @Column(name = "selected_option_uuid")
    private UUID selectedOptionUuid;

    @Column(name = "text_response")
    private String textResponse;

    @Column(name = "points_earned")
    private BigDecimal pointsEarned;

    @Column(name = "is_correct")
    private Boolean isCorrect;
}