package apps.sarafrika.elimika.skills.internal;

import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSlugs;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillLookupServiceImpl implements SkillLookupService {

    private final SkillRepository skillRepository;

    @Override
    public List<SkillSummary> findByUuids(Collection<UUID> uuids) {
        List<UUID> distinct = uuids == null ? List.of() : uuids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        return skillRepository.findByUuidInOrderByNameAsc(distinct).stream().map(Skill::toSummary).toList();
    }

    @Override
    public List<SkillSummary> findBySlugs(Collection<String> slugs) {
        List<String> distinct = slugs == null ? List.of() : slugs.stream()
                .map(SkillSlugs::slugify)
                .filter(slug -> !slug.isEmpty())
                .distinct()
                .toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        return skillRepository.findBySlugInOrderByNameAsc(distinct).stream().map(Skill::toSummary).toList();
    }

    @Override
    public Map<String, SkillSummary> resolve(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return Map.of();
        }
        Map<String, Skill> bySlug = new HashMap<>();
        Map<String, Skill> byAlias = new HashMap<>();
        for (Skill skill : skillRepository.findByActiveTrueOrderByNameAsc()) {
            bySlug.putIfAbsent(skill.getSlug(), skill);
            for (String alias : skill.aliasList()) {
                byAlias.putIfAbsent(SkillSlugs.slugify(alias), skill);
            }
        }
        Map<String, SkillSummary> resolved = new LinkedHashMap<>();
        for (String name : names) {
            String slug = SkillSlugs.slugify(name);
            if (slug.isEmpty()) {
                continue;
            }
            Skill match = bySlug.getOrDefault(slug, byAlias.get(slug));
            if (match != null) {
                resolved.put(name, match.toSummary());
            }
        }
        return resolved;
    }

    @Override
    public Map<String, List<String>> aliases() {
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        for (Skill skill : skillRepository.findByActiveTrueOrderByNameAsc()) {
            if (!skill.aliasList().isEmpty()) {
                aliases.put(skill.getName(), skill.aliasList());
            }
        }
        return aliases;
    }
}
