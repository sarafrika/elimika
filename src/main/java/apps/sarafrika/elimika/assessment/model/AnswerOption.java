package apps.sarafrika.elimika.assessment.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "answer_option")
public class AnswerOption extends BaseEntity {

    @Column(name = "option_text")
    private String optionText;

    @Column(name = "correct")
    private boolean correct;

    @Column(name = "order_in_question")
    private int orderInQuestion;

    @Column(name = "question_uuid")
    private UUID questionUuid;

}


