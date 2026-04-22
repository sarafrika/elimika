package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.classes.util.converter.ClassMarketplaceJobApplicationStatusConverter;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "class_marketplace_job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassMarketplaceJobApplication extends BaseEntity {

    @Column(name = "job_uuid")
    private UUID jobUuid;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Convert(converter = ClassMarketplaceJobApplicationStatusConverter.class)
    @Column(name = "status")
    private ClassMarketplaceJobApplicationStatus status;

    @Column(name = "application_note")
    private String applicationNote;

    @Column(name = "review_notes")
    private String reviewNotes;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
