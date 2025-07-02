package apps.sarafrika.elimika.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "document_types")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class DocumentType extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_file_size_mb")
    private Integer maxFileSizeMb;

    @Column(name = "allowed_extensions", columnDefinition = "jsonb")
    private String allowedExtensions;

    @Column(name = "is_required")
    private Boolean isRequired;
}