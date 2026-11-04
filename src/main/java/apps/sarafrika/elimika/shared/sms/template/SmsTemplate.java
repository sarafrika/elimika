package apps.sarafrika.elimika.shared.sms.template;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An admin's wording for one system SMS; no row means the built-in default is sent. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sms_templates")
public class SmsTemplate extends BaseEntity {

    @Column(name = "template_key")
    private String templateKey;

    @Column(name = "body")
    private String body;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "updated_by_name")
    private String updatedByName;
}
