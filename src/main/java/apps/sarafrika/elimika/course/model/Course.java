package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor @Table(name = "courses")
public class Course extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration_hours")
    private BigDecimal durationHours;

    @Column(name = "difficulty_level")
    private String difficultyLevel;

    @Column(name = "is_free")
    private boolean isFree;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "sale_price")
    private BigDecimal salePrice;

    @Column(name = "min_age")
    private int minAge;

    @Column(name = "max_age")
    private int maxAge;

    @Column(name = "class_size")
    private int classSize;
}
