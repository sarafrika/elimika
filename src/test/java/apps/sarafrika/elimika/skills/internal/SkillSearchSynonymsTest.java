package apps.sarafrika.elimika.skills.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Skill names and aliases as search synonyms")
class SkillSearchSynonymsTest {

    @Test
    @DisplayName("A name and its aliases map to each other, lower-cased; a skill without aliases adds nothing")
    void namesAndAliasesMapToEachOther() {
        Map<String, List<String>> synonyms = SkillSearchSynonyms.synonyms(List.of(
                List.of("JavaScript", "JS", "ECMAScript"),
                List.of("Kubernetes"),
                List.of("Machine  Learning", "ML")));

        assertThat(synonyms).containsOnlyKeys("javascript", "js", "ecmascript", "machine learning", "ml");
        assertThat(synonyms.get("js")).containsExactly("javascript", "ecmascript");
        assertThat(synonyms.get("javascript")).containsExactly("js", "ecmascript");
        assertThat(synonyms.get("ml")).containsExactly("machine learning");
    }
}
