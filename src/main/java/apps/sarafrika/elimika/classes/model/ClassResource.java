package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "class_resources")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClassResource extends BaseEntity {

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_url")
    private String resourceUrl;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "is_active")
    private Boolean isActive = true;
}