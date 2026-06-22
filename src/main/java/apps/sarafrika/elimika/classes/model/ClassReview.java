package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "class_reviews")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClassReview extends BaseEntity {

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "headline")
    private String headline;

    @Column(name = "comments")
    private String comments;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous;
}
