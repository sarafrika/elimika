package apps.sarafrika.elimika.skills.spi;

import java.util.List;
import java.util.UUID;

/**
 * One taxonomy entry, as other modules see it.
 *
 * @param aliases alternative names; matched by {@link SkillLookupService#resolve} and meant as search synonyms
 * @param active  false once an admin retires the skill: existing tags keep it, new tags may not pick it
 */
public record SkillSummary(
        UUID uuid,
        String name,
        String slug,
        UUID parentUuid,
        List<String> aliases,
        boolean active
) {
}
