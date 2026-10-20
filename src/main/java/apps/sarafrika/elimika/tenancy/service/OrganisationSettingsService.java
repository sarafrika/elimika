package apps.sarafrika.elimika.tenancy.service;

import apps.sarafrika.elimika.tenancy.dto.OrganisationSettingsDTO;
import apps.sarafrika.elimika.tenancy.entity.OrganisationSettings;
import apps.sarafrika.elimika.tenancy.repository.OrganisationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Reads and changes an organisation's own settings; an organisation without a row gets the defaults. */
@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationSettingsService {

    private final OrganisationSettingsRepository repository;

    @Value("${app.session-reports.default-grace-hours:48}")
    private int defaultGraceHours;

    @Transactional(readOnly = true)
    public OrganisationSettingsDTO get(UUID organisationUuid) {
        return new OrganisationSettingsDTO(organisationUuid, graceHours(organisationUuid));
    }

    public OrganisationSettingsDTO update(UUID organisationUuid, OrganisationSettingsDTO settings) {
        OrganisationSettings row = repository.findByOrganisationUuid(organisationUuid).orElseGet(() -> {
            OrganisationSettings created = new OrganisationSettings();
            created.setOrganisationUuid(organisationUuid);
            return created;
        });
        row.setSessionReportGraceHours(settings.sessionReportGraceHours());
        repository.save(row);
        return get(organisationUuid);
    }

    @Transactional(readOnly = true)
    public int graceHours(UUID organisationUuid) {
        if (organisationUuid == null) {
            return defaultGraceHours;
        }
        return repository.findByOrganisationUuid(organisationUuid)
                .map(OrganisationSettings::getSessionReportGraceHours)
                .orElse(defaultGraceHours);
    }
}
