package apps.sarafrika.elimika.tenancy.repository.projection;

import java.util.UUID;

/**
 * Where an organisation says it is based, in words.
 * <p>
 * Two columns, and pointedly not the two next to them: {@code organisation.lat} and
 * {@code organisation.long} are on the same row, and a directory answering "where does this
 * provider operate?" must not be able to answer it to six decimal places. Selecting the town alone
 * means no coordinate is loaded, so none can be forwarded by accident.
 *
 * @param organisationUuid the organisation identifier
 * @param town             the free-text town or locality the organisation gives as its base
 */
public record OrganisationTownView(UUID organisationUuid, String town) {
}
