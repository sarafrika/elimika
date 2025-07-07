package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.enums.TemplateType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "certificate_templates")
public class CertificateTemplate extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "template_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TemplateType templateType;

    @Column(name = "template_html")
    private String templateHtml;

    @Column(name = "template_css")
    private String templateCss;

    @Column(name = "background_image_url")
    private String backgroundImageUrl;

    @Column(name = "is_active")
    private Boolean active;
}