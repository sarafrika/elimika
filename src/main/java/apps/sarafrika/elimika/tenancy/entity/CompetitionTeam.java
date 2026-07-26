package apps.sarafrika.elimika.tenancy.entity;

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

/**
 * A team registered for a {@link Competition}.
 */
@Entity
@Table(name = "competition_teams")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionTeam extends BaseEntity {

    @Column(name = "competition_uuid")
    private UUID competitionUuid;

    @Column(name = "team_name")
    private String teamName;
}
