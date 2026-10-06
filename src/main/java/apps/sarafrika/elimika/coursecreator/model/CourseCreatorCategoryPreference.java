package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "course_creator_category_preferences")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseCreatorCategoryPreference extends BaseEntity {

    @Column(name = "course_creator_uuid")
    @Filterable
    private UUID courseCreatorUuid;

    @Column(name = "category_uuid")
    @Filterable
    private UUID categoryUuid;
}
