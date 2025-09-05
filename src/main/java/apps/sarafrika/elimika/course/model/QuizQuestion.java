package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.course.util.enums.QuestionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_questions")
public class QuizQuestion extends BaseEntity {

    @Column(name = "quiz_uuid")
    private UUID quizUuid;

    @Column(name = "question_text")
    private String questionText;

    @Column(name = "question_type")
    private QuestionType questionType;

    @Column(name = "points")
    private BigDecimal points;

    @Column(name = "display_order")
    private Integer displayOrder;
}