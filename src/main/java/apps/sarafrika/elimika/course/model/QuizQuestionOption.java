package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
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
    @Filterable
    private UUID questionUuid;

    @Column(name = "option_text")
    private String optionText;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    // NOT NULL in the schema with a DB default. Hibernate emits every mapped column on insert, so a
    // null here would be sent explicitly and defeat that default — initialise it on the Java side.
    @Column(name = "display_order", nullable = false)
    @Filterable
    private Integer displayOrder = 1;

    /**
     * On a draft option, the live option it will be promoted onto. NULL means the edit adds
     * it. Preserves live option uuids so quiz_responses.selected_option_uuid stays valid.
     */
    @Column(name = "source_option_uuid")
    private UUID sourceOptionUuid;
}
