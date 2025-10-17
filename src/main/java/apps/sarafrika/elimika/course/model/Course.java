package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.course.util.converter.ContentStatusConverter;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "course_creator_uuid")
    private UUID courseCreatorUuid;

    @Column(name = "difficulty_uuid")
    private UUID difficultyUuid;

    @Column(name = "description")
    private String description;

    @Column(name = "objectives")
    private String objectives;

    @Column(name = "prerequisites")
    private String prerequisites;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "class_limit")
    private Integer classLimit;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "minimum_training_fee")
    private BigDecimal minimumTrainingFee;

    @Column(name = "creator_share_percentage")
    private BigDecimal creatorSharePercentage;

    @Column(name = "instructor_share_percentage")
    private BigDecimal instructorSharePercentage;

    @Column(name = "revenue_share_notes")
    private String revenueShareNotes;

    @Column(name = "age_lower_limit")
    private Integer ageLowerLimit;

    @Column(name = "age_upper_limit")
    private Integer ageUpperLimit;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "intro_video_url")
    private String introVideoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "status")
    @Convert(converter = ContentStatusConverter.class)
    private ContentStatus status;

    @Column(name = "active")
    private Boolean active;

    // Many-to-many relationship with categories through junction table
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CourseCategoryMapping> categoryMappings = new HashSet<>();

    /**
     * Relationship to difficulty level for querying purposes.
     * Read-only relationship (insertable=false, updatable=false).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "difficulty_uuid", referencedColumnName = "uuid",
            insertable = false, updatable = false)
    private DifficultyLevel difficulty;

    /**
     * Relationship to course enrollments for querying purposes.
     * Read-only relationship fetched lazily.
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_uuid", referencedColumnName = "uuid",
            insertable = false, updatable = false)
    private java.util.List<CourseEnrollment> enrollments;

    // Helper methods for managing categories
    public void addCategory(UUID categoryUuid) {
        CourseCategoryMapping mapping = CourseCategoryMapping.builder()
                .courseUuid(this.getUuid())
                .categoryUuid(categoryUuid)
                .build();
        this.categoryMappings.add(mapping);
    }

    public void removeCategory(UUID categoryUuid) {
        this.categoryMappings.removeIf(mapping ->
                mapping.getCategoryUuid().equals(categoryUuid));
    }

    public void clearCategories() {
        this.categoryMappings.clear();
    }

    public Set<UUID> getCategoryUuids() {
        return categoryMappings.stream()
                .map(CourseCategoryMapping::getCategoryUuid)
                .collect(java.util.stream.Collectors.toSet());
    }
}
