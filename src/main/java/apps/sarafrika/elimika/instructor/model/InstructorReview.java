package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "instructor_reviews")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class InstructorReview extends BaseEntity {

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "enrollment_uuid")
    private UUID enrollmentUuid;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "headline")
    private String headline;

    @Column(name = "comments")
    private String comments;

    @Column(name = "clarity_rating")
    private Integer clarityRating;

    @Column(name = "engagement_rating")
    private Integer engagementRating;

    @Column(name = "punctuality_rating")
    private Integer punctualityRating;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous;
}

*** Add File: src/main/java/apps/sarafrika/elimika/instructor/dto/InstructorReviewDTO.java
package apps.sarafrika.elimika.instructor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "InstructorReview",
        description = "Student review and rating for an instructor, scoped to a specific enrollment.",
        example = """
        {
          "uuid": "rev-1234-5678-90ab-cdef12345678",
          "instructor_uuid": "inst-1234-5678-90ab-cdef12345678",
          "student_uuid": "stud-1234-5678-90ab-cdef12345678",
          "enrollment_uuid": "enr-1234-5678-90ab-cdef12345678",
          "rating": 5,
          "headline": "Incredible instructor!",
          "comments": "Very clear explanations and engaging sessions.",
          "clarity_rating": 5,
          "engagement_rating": 5,
          "punctuality_rating": 4,
          "is_anonymous": false,
          "created_date": "2025-11-18T09:00:00",
          "created_by": "student@example.com",
          "updated_date": "2025-11-18T09:00:00",
          "updated_by": "student@example.com"
        }
        """
)
public record InstructorReviewDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the review.",
                example = "rev-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[REQUIRED]** Instructor being reviewed.",
                example = "inst-1234-5678-90ab-cdef12345678"
        )
        @NotNull(message = "Instructor UUID is required")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[REQUIRED]** Student leaving the review.",
                example = "stud-1234-5678-90ab-cdef12345678"
        )
        @NotNull(message = "Student UUID is required")
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(
                description = "**[REQUIRED]** Enrollment that this review is tied to.",
                example = "enr-1234-5678-90ab-cdef12345678"
        )
        @NotNull(message = "Enrollment UUID is required")
        @JsonProperty("enrollment_uuid")
        UUID enrollmentUuid,

        @Schema(
                description = "Overall rating for the instructor (1-5).",
                example = "5",
                minimum = "1",
                maximum = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        @JsonProperty("rating")
        Integer rating,

        @Schema(
                description = "Optional short headline for the review.",
                example = "Incredible instructor!",
                maxLength = 255
        )
        @Size(max = 255, message = "Headline must not exceed 255 characters")
        @JsonProperty("headline")
        String headline,

        @Schema(
                description = "Detailed feedback from the student.",
                example = "Very clear explanations and engaging sessions.",
                maxLength = 5000
        )
        @Size(max = 5000, message = "Comments must not exceed 5000 characters")
        @JsonProperty("comments")
        String comments,

        @Schema(
                description = "Optional clarity rating (1-5).",
                minimum = "1",
                maximum = "5",
                nullable = true
        )
        @Min(value = 1, message = "Clarity rating must be at least 1")
        @Max(value = 5, message = "Clarity rating must be at most 5")
        @JsonProperty("clarity_rating")
        Integer clarityRating,

        @Schema(
                description = "Optional engagement rating (1-5).",
                minimum = "1",
                maximum = "5",
                nullable = true
        )
        @Min(value = 1, message = "Engagement rating must be at least 1")
        @Max(value = 5, message = "Engagement rating must be at most 5")
        @JsonProperty("engagement_rating")
        Integer engagementRating,

        @Schema(
                description = "Optional punctuality rating (1-5).",
                minimum = "1",
                maximum = "5",
                nullable = true
        )
        @Min(value = 1, message = "Punctuality rating must be at least 1")
        @Max(value = 5, message = "Punctuality rating must be at most 5")
        @JsonProperty("punctuality_rating")
        Integer punctualityRating,

        @Schema(
                description = "Whether the review should be shown anonymously in public views.",
                example = "false"
        )
        @JsonProperty("is_anonymous")
        Boolean isAnonymous,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the review was created.",
                example = "2025-11-18T09:00:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** Created by identifier (typically the student email or system).",
                example = "student@example.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the review was last updated.",
                example = "2025-11-18T09:00:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** Updated by identifier.",
                example = "student@example.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}

*** Add File: src/main/java/apps/sarafrika/elimika/instructor/factory/InstructorReviewFactory.java
package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorReviewDTO;
import apps.sarafrika.elimika.instructor.model.InstructorReview;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorReviewFactory {

    public static InstructorReviewDTO toDTO(InstructorReview entity) {
        if (entity == null) {
            return null;
        }
        return new InstructorReviewDTO(
                entity.getUuid(),
                entity.getInstructorUuid(),
                entity.getStudentUuid(),
                entity.getEnrollmentUuid(),
                entity.getRating(),
                entity.getHeadline(),
                entity.getComments(),
                entity.getClarityRating(),
                entity.getEngagementRating(),
                entity.getPunctualityRating(),
                entity.getIsAnonymous(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }

    public static InstructorReview toEntity(InstructorReviewDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorReview entity = new InstructorReview();
        entity.setUuid(dto.uuid());
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setStudentUuid(dto.studentUuid());
        entity.setEnrollmentUuid(dto.enrollmentUuid());
        entity.setRating(dto.rating());
        entity.setHeadline(dto.headline());
        entity.setComments(dto.comments());
        entity.setClarityRating(dto.clarityRating());
        entity.setEngagementRating(dto.engagementRating());
        entity.setPunctualityRating(dto.punctualityRating());
        entity.setIsAnonymous(dto.isAnonymous());
        return entity;
    }
}

*** Add File: src/main/java/apps/sarafrika/elimika/instructor/repository/InstructorReviewRepository.java
package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstructorReviewRepository extends JpaRepository<InstructorReview, Long> {

    Optional<InstructorReview> findByUuid(UUID uuid);

    boolean existsByInstructorUuidAndEnrollmentUuid(UUID instructorUuid, UUID enrollmentUuid);

    List<InstructorReview> findByInstructorUuid(UUID instructorUuid);

    @Query("SELECT AVG(r.rating) FROM InstructorReview r WHERE r.instructorUuid = :instructorUuid")
    Double findAverageRatingForInstructor(@Param("instructorUuid") UUID instructorUuid);
}

*** Add File: src/main/java/apps/sarafrika/elimika/instructor/service/InstructorReviewService.java
package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.InstructorReviewDTO;

import java.util.List;
import java.util.UUID;

public interface InstructorReviewService {

    InstructorReviewDTO createReview(InstructorReviewDTO reviewDTO);

    List<InstructorReviewDTO> getReviewsForInstructor(UUID instructorUuid);
}

*** Add File: src/main/java/apps/sarafrika/elimika/instructor/service/impl/InstructorReviewServiceImpl.java
package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorReviewDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorReviewFactory;
import apps.sarafrika.elimika.instructor.model.InstructorReview;
import apps.sarafrika.elimika.instructor.repository.InstructorReviewRepository;
import apps.sarafrika.elimika.instructor.service.InstructorReviewService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorReviewServiceImpl implements InstructorReviewService {

    private final InstructorReviewRepository instructorReviewRepository;
    private final EnrollmentRepository enrollmentRepository;

    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Enrollment with ID %s not found";

    @Override
    public InstructorReviewDTO createReview(InstructorReviewDTO reviewDTO) {
        Enrollment enrollment = enrollmentRepository.findByUuid(reviewDTO.enrollmentUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, reviewDTO.enrollmentUuid())));

        if (!enrollment.getStudentUuid().equals(reviewDTO.studentUuid())) {
            throw new IllegalArgumentException("Review student_uuid does not match enrollment student.");
        }

        if (instructorReviewRepository.existsByInstructorUuidAndEnrollmentUuid(
                reviewDTO.instructorUuid(), reviewDTO.enrollmentUuid())) {
            throw new IllegalStateException("A review for this instructor and enrollment already exists.");
        }

        InstructorReview entity = InstructorReviewFactory.toEntity(reviewDTO);
        InstructorReview saved = instructorReviewRepository.save(entity);
        return InstructorReviewFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorReviewDTO> getReviewsForInstructor(UUID instructorUuid) {
        return instructorReviewRepository.findByInstructorUuid(instructorUuid)
                .stream()
                .map(InstructorReviewFactory::toDTO)
                .collect(Collectors.toList());
    }
}

