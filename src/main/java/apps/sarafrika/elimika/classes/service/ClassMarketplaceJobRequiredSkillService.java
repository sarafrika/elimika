package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequiredSkillsDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequiredSkillsRequest;

import java.util.UUID;

/**
 * A marketplace job's required skills: optional tags set by the posting organisation. A job with no
 * tags of its own inherits its course's skills.
 */
public interface ClassMarketplaceJobRequiredSkillService {

    ClassMarketplaceJobRequiredSkillsDTO getRequiredSkills(UUID jobUuid);

    ClassMarketplaceJobRequiredSkillsDTO replaceRequiredSkills(UUID jobUuid, ClassMarketplaceJobRequiredSkillsRequest request);
}
