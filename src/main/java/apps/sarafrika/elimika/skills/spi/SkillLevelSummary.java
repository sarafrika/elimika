package apps.sarafrika.elimika.skills.spi;

import java.math.BigDecimal;
import java.util.UUID;

/** One level of the platform scale; rank 1 is the lowest. */
public record SkillLevelSummary(
        UUID uuid,
        String name,
        BigDecimal minPercentage,
        boolean countsAsAchieved,
        int rank
) {
}
