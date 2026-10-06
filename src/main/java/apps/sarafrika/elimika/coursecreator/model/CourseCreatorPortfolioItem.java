package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.profile.spi.PortfolioItemType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "course_creator_portfolio_items")
@Getter
@Setter
@NoArgsConstructor
public class CourseCreatorPortfolioItem extends BaseEntity {

    @Column(name = "course_creator_uuid")
    private UUID courseCreatorUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "item_type")
    private PortfolioItemType itemType;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "completed_on")
    private LocalDate completedOn;

    @Column(name = "description")
    private String description;
}
