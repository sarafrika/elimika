package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.enums.CourseDifficulty;
import apps.sarafrika.elimika.course.util.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "courses")
@Getter
@Setter @AllArgsConstructor @NoArgsConstructor
public class Course extends BaseEntity {


    @Column(name = "course_code")
    private String courseCode;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "course_description")
    private String courseDescription;

    @Column(name = "course_thumbnail")
    private String courseThumbnail;

    @Column(name = "initial_price")
    private BigDecimal initialPrice;

    @Column(name = "current_price")
    private BigDecimal currentPrice;

    @Column(name = "access_start_date")
    private ZonedDateTime accessStartDate;

    @Column(name = "class_limit")
    private Integer classLimit;

    @Column(name = "age_upper_limit")
    private Integer ageUpperLimit;

    @Column(name = "age_lower_limit")
    private Integer ageLowerLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", columnDefinition = "difficulty_level")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CourseDifficulty difficulty;

    @Column(name = "course_objectives")
    private String courseObjectives;

    @Column(name = "course_status",columnDefinition = "status_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CourseStatus courseStatus;
}

