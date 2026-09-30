package apps.sarafrika.elimika.skills.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read access to the skills taxonomy for the modules that tag with it.
 */
public interface SkillLookupService {

    /** The skills with these uuids, active or not, in name order. Unknown uuids are left out. */
    List<SkillSummary> findByUuids(Collection<UUID> uuids);

    /** The skills with these slugs, active or not, in name order. Unknown slugs are left out. */
    List<SkillSummary> findBySlugs(Collection<String> slugs);

    /**
     * Resolves free-text names to active skills by slug: a name matches the skill whose slug, or one
     * of whose aliases' slugs, equals {@link SkillSlugs#slugify} of the name. A slug match wins over
     * an alias match.
     *
     * @return input name to its skill; names that match nothing are absent
     */
    Map<String, SkillSummary> resolve(Collection<String> names);

    /** Every active skill's aliases keyed by its name, for search synonyms. Skills without aliases are absent. */
    Map<String, List<String>> aliases();
}
