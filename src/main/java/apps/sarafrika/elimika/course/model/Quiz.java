package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.ContentStatusConverter;
import apps.sarafrika.elimika.course.util.converter.QuizScopeConverter;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.QuizScope;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "quizzes")
public class Quiz extends BaseEntity {

    @Column(name = "lesson_uuid")
    @Filterable
    private UUID lessonUuid;

    @Convert(converter = QuizScopeConverter.class)
    @Column(name = "scope")
    private QuizScope scope = QuizScope.COURSE_TEMPLATE;

    @Column(name = "class_definition_uuid")
    @Filterable
    private UUID classDefinitionUuid;

    @Column(name = "source_quiz_uuid")
    private UUID sourceQuizUuid;

    @Column(name = "title")
    @Filterable
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "attempts_allowed")
    private Integer attemptsAllowed;

    @Column(name = "passing_score")
    private BigDecimal passingScore;

    @Column(name = "rubric_uuid")
    private UUID rubricUuid;

    @Column(name = "status")
    @Convert(converter = ContentStatusConverter.class)
    @Filterable
    private ContentStatus status;

    @Column(name = "active")
    @Filterable
    private Boolean active;
}
