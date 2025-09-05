package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.course.util.converter.TemplateTypeConverter;
import apps.sarafrika.elimika.course.util.enums.TemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Convert(converter = TemplateTypeConverter.class)
    private TemplateType templateType;

    @Column(name = "template_html")
    private String templateHtml;

    @Column(name = "template_css")
    private String templateCss;

    @Column(name = "background_image_url")
    private String backgroundImageUrl;

    @Column(name = "is_active")
    private Boolean isActive;
}