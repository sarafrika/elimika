package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.profile.spi.PortfolioItemType;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_portfolio_items")
@Getter
@Setter
@NoArgsConstructor
public class UserPortfolioItem extends UserOwnedEntity {

    @Column(name = "title")
    @Filterable
    private String title;

    @Column(name = "item_type")
    @Filterable
    private PortfolioItemType itemType;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "completed_on")
    @Filterable
    private LocalDate completedOn;

    @Column(name = "description")
    private String description;
}
