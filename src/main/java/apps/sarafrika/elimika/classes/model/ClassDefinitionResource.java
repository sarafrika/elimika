package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "class_definition_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassDefinitionResource extends BaseEntity {

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "resource_uuid")
    private UUID resourceUuid;

    @Column(name = "quantity")
    private Integer quantity;
}
