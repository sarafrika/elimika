package apps.sarafrika.elimika.assessment.persistence;

import apps.sarafrika.elimika.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerOption extends BaseEntity {

    @Column(name = "option_text")
    private String optionText;

    @Column(name = "correct")
    private boolean correct;

    @Column(name = "order_in_question")
    private int orderInQuestion;

    @Column(name = "question_id")
    private Long questionId;

}


