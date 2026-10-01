package apps.sarafrika.elimika.skills.internal;

import apps.sarafrika.elimika.shared.search.SearchSynonymSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The skills taxonomy as engine synonyms ({@link SearchSynonymSource#SKILLS}): for every active skill,
 * its name and each of its aliases, lower-cased, map to all the others. "JavaScript" with alias "JS"
 * gives {@code javascript -> [js]} and {@code js -> [javascript]}, so {@code q=js} finds a job tagged
 * JavaScript. Retired skills are left out. The indexes that take these words name the source in their
 * definition; {@link SkillService} publishes {@code SearchSynonymsChanged} after every change.
 */
@Component
@RequiredArgsConstructor
public class SkillSearchSynonyms implements SearchSynonymSource {

    private final SkillRepository skillRepository;

    @Override
    public String name() {
        return SKILLS;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> synonyms() {
        List<List<String>> groups = new ArrayList<>();
        for (Skill skill : skillRepository.findByActiveTrueOrderByNameAsc()) {
            List<String> words = new ArrayList<>();
            words.add(skill.getName());
            words.addAll(skill.aliasList());
            groups.add(words);
        }
        return synonyms(groups);
    }

    /** Each group's words, lower-cased and trimmed, mapped to the group's other words. */
    static Map<String, List<String>> synonyms(List<List<String>> groups) {
        Map<String, Set<String>> merged = new LinkedHashMap<>();
        for (List<String> group : groups) {
            Set<String> words = new LinkedHashSet<>();
            for (String word : group) {
                if (word != null && !word.isBlank()) {
                    words.add(word.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT));
                }
            }
            if (words.size() < 2) {
                continue;
            }
            for (String word : words) {
                Set<String> others = merged.computeIfAbsent(word, key -> new LinkedHashSet<>());
                words.stream().filter(other -> !other.equals(word)).forEach(others::add);
            }
        }
        Map<String, List<String>> synonyms = new LinkedHashMap<>();
        merged.forEach((word, others) -> synonyms.put(word, List.copyOf(others)));
        return synonyms;
    }
}
