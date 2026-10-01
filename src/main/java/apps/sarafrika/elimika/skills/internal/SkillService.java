package apps.sarafrika.elimika.skills.internal;

import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.search.SearchSynonymSource;
import apps.sarafrika.elimika.shared.search.SearchSynonymsChanged;
import apps.sarafrika.elimika.skills.dto.SkillDTO;
import apps.sarafrika.elimika.skills.dto.SkillRequest;
import apps.sarafrika.elimika.skills.spi.SkillSlugs;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Curation of the skills taxonomy (platform admins) and the picker listing (everyone signed in).
 * <p>
 * Name matching for {@code q} is done in memory over the whole taxonomy. The list is small and
 * admin-curated, and SQL text matching was removed from the platform on purpose: free text is
 * otherwise served only by the search engine.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SkillService {

    static final int MAX_QUERY_LENGTH = 200;
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    private final SkillRepository skillRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<SkillDTO> listAll(String q, Boolean active) {
        List<Skill> skills = skillRepository.findAllByOrderByNameAsc().stream()
                .filter(skill -> active == null || skill.isActive() == active)
                .toList();
        return filter(skills, q, Integer.MAX_VALUE);
    }

    @Transactional(readOnly = true)
    public List<SkillDTO> listActive(String q, Integer limit) {
        int cap = limit == null ? DEFAULT_LIMIT : limit;
        if (cap < 1 || cap > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return filter(skillRepository.findByActiveTrueOrderByNameAsc(), q, cap);
    }

    @Transactional(readOnly = true)
    public SkillDTO get(UUID uuid) {
        return toDto(find(uuid));
    }

    public SkillDTO create(SkillRequest request) {
        Skill skill = new Skill();
        apply(skill, request);
        SkillDTO created = toDto(skillRepository.save(skill));
        synonymsChanged();
        return created;
    }

    public SkillDTO update(UUID uuid, SkillRequest request) {
        Skill skill = find(uuid);
        apply(skill, request);
        SkillDTO updated = toDto(skillRepository.save(skill));
        synonymsChanged();
        return updated;
    }

    /**
     * Deletes the skill. The database clears it from instructor skills and drops it from course and
     * job tags (foreign keys), and child skills lose their parent. Retiring it ({@code active=false})
     * keeps existing tags instead.
     */
    public void delete(UUID uuid) {
        skillRepository.delete(find(uuid));
        synonymsChanged();
    }

    /** Names and aliases are search synonyms; the search module re-applies them after commit. */
    private void synonymsChanged() {
        eventPublisher.publishEvent(new SearchSynonymsChanged(SearchSynonymSource.SKILLS));
    }

    private Skill find(UUID uuid) {
        return skillRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Skill with UUID " + uuid + " not found"));
    }

    private void apply(Skill skill, SkillRequest request) {
        String name = normaliseSpaces(request.name());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        String slug = SkillSlugs.slugify(request.slug() == null || request.slug().isBlank() ? name : request.slug());
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("A skill name or slug must contain at least one letter a-z or digit");
        }
        List<Skill> others = skillRepository.findAll().stream()
                .filter(other -> !Objects.equals(other.getUuid(), skill.getUuid()))
                .toList();
        List<String> aliases = normaliseAliases(request.aliases(), name, slug);
        checkUniqueness(slug, aliases, others);
        checkParent(skill.getUuid(), request.parentUuid(), others);

        skill.setName(name);
        skill.setSlug(slug);
        skill.setParentUuid(request.parentUuid());
        skill.setAliases(aliases.toArray(String[]::new));
        skill.setActive(request.active() == null || request.active());
    }

    private static void checkUniqueness(String slug, List<String> aliases, List<Skill> others) {
        Map<String, Skill> taken = new LinkedHashMap<>();
        for (Skill other : others) {
            taken.putIfAbsent(other.getSlug(), other);
            for (String alias : other.aliasList()) {
                taken.putIfAbsent(SkillSlugs.slugify(alias), other);
            }
        }
        Skill clash = taken.get(slug);
        if (clash != null) {
            throw new DuplicateResourceException("Slug '" + slug + "' is already used by skill '" + clash.getName() + "'");
        }
        for (String alias : aliases) {
            Skill aliasClash = taken.get(SkillSlugs.slugify(alias));
            if (aliasClash != null) {
                throw new DuplicateResourceException("Alias '" + alias + "' already names skill '" + aliasClash.getName() + "'");
            }
        }
    }

    private static void checkParent(UUID self, UUID parentUuid, List<Skill> others) {
        if (parentUuid == null) {
            return;
        }
        if (parentUuid.equals(self)) {
            throw new IllegalArgumentException("A skill cannot be its own parent");
        }
        Map<UUID, UUID> parents = new LinkedHashMap<>();
        others.forEach(other -> parents.put(other.getUuid(), other.getParentUuid()));
        if (!parents.containsKey(parentUuid)) {
            throw new IllegalArgumentException("Parent skill " + parentUuid + " not found");
        }
        if (self == null) {
            return;
        }
        Set<UUID> seen = new HashSet<>();
        UUID cursor = parentUuid;
        while (cursor != null && seen.add(cursor)) {
            if (cursor.equals(self)) {
                throw new IllegalArgumentException("A skill cannot sit under one of its own descendants");
            }
            cursor = parents.get(cursor);
        }
    }

    private static List<String> normaliseAliases(List<String> aliases, String name, String slug) {
        if (aliases == null) {
            return List.of();
        }
        Map<String, String> bySlug = new LinkedHashMap<>();
        for (String alias : aliases) {
            String value = normaliseSpaces(alias);
            String aliasSlug = SkillSlugs.slugify(value);
            if (aliasSlug.isEmpty() || aliasSlug.equals(slug) || value.equalsIgnoreCase(name)) {
                continue;
            }
            bySlug.putIfAbsent(aliasSlug, value);
        }
        return List.copyOf(bySlug.values());
    }

    private static String normaliseSpaces(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    /**
     * The skills matching {@code q} in memory: an exact name, slug or alias first, then names
     * starting with it, then any name, slug or alias containing it; ties in name order.
     */
    static List<SkillDTO> filter(List<Skill> skills, String q, int limit) {
        if (q == null || q.isBlank()) {
            return skills.stream().limit(limit).map(SkillService::toDto).toList();
        }
        String needle = q.trim().toLowerCase(Locale.ROOT);
        if (needle.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("q must not exceed " + MAX_QUERY_LENGTH + " characters");
        }
        String slugNeedle = SkillSlugs.slugify(needle);
        List<Ranked> ranked = new ArrayList<>();
        for (Skill skill : skills) {
            int rank = rank(skill, needle, slugNeedle);
            if (rank >= 0) {
                ranked.add(new Ranked(skill, rank));
            }
        }
        return ranked.stream()
                .sorted(Comparator.comparingInt(Ranked::rank)
                        .thenComparing(entry -> entry.skill().getName(), String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .map(entry -> toDto(entry.skill()))
                .toList();
    }

    private static int rank(Skill skill, String needle, String slugNeedle) {
        String name = skill.getName() == null ? "" : skill.getName().toLowerCase(Locale.ROOT);
        List<String> aliases = skill.aliasList().stream().map(alias -> alias.toLowerCase(Locale.ROOT)).toList();
        boolean slugUsable = !slugNeedle.isEmpty();
        if (name.equals(needle) || aliases.contains(needle) || (slugUsable && slugNeedle.equals(skill.getSlug()))) {
            return 0;
        }
        if (name.startsWith(needle)) {
            return 1;
        }
        if (name.contains(needle)
                || aliases.stream().anyMatch(alias -> alias.contains(needle))
                || (slugUsable && skill.getSlug() != null && skill.getSlug().contains(slugNeedle))) {
            return 2;
        }
        return -1;
    }

    static SkillDTO toDto(Skill skill) {
        return new SkillDTO(skill.getUuid(), skill.getName(), skill.getSlug(), skill.getParentUuid(),
                skill.aliasList(), skill.isActive(), skill.getCreatedDate(), skill.getLastModifiedDate());
    }

    private record Ranked(Skill skill, int rank) {
    }
}
