package apps.sarafrika.elimika.skills.spi;

import java.math.BigDecimal;
import java.util.List;

/** Reads the platform-wide skill level scale. */
public interface SkillLevelLookupService {

    /** Every level, lowest minimum first. */
    List<SkillLevelSummary> levels();

    /** The highest level whose minimum the score reaches. */
    SkillLevelSummary levelFor(BigDecimal score);
}
