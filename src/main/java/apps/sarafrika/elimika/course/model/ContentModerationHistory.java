package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.ModerationActionConverter;
import apps.sarafrika.elimika.course.util.converter.ModerationContentTypeConverter;
import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import apps.sarafrika.elimika.course.util.enums.ModerationContentType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "content_moderation_history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContentModerationHistory extends BaseEntity {

    @Column(name = "content_type")
    @Convert(converter = ModerationContentTypeConverter.class)
    private ModerationContentType contentType;

    @Column(name = "content_uuid")
    private UUID contentUuid;

    @Column(name = "action")
    @Convert(converter = ModerationActionConverter.class)
    private ModerationAction action;

    @Column(name = "reason")
    private String reason;

    @Column(name = "moderator_uuid")
    private UUID moderatorUuid;
}
