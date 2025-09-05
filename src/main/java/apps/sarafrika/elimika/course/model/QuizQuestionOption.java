package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_question_options")
public class QuizQuestionOption extends BaseEntity {

    @Column(name = "question_uuid")
    private UUID questionUuid;

    @Column(name = "option_text")
    private String optionText;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "display_order")
    private Integer displayOrder;
}
