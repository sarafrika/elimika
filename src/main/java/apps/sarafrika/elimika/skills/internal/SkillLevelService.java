package apps.sarafrika.elimika.skills.internal;

import apps.sarafrika.elimika.skills.dto.SkillLevelDTO;
import apps.sarafrika.elimika.skills.spi.SkillLevelLookupService;
import apps.sarafrika.elimika.skills.spi.SkillLevelSummary;
import apps.sarafrika.elimika.skills.spi.SkillLevelsChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The platform skill level scale: read by every programme, replaced as a whole by a system admin. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillLevelService implements SkillLevelLookupService {

    private final SkillLevelRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public List<SkillLevelDTO> list() {
        return repository.findAllByOrderByMinPercentageAsc().stream()
                .map(level -> new SkillLevelDTO(level.getUuid(), level.getName(), level.getMinPercentage(),
                        Boolean.TRUE.equals(level.getCountsAsAchieved())))
                .toList();
    }

    @Transactional
    public List<SkillLevelDTO> replace(List<SkillLevelDTO> requested) {
        List<SkillLevelDTO> sorted = validate(requested);
        repository.deleteAllInBatch();
        repository.flush();
        List<SkillLevel> levels = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            SkillLevelDTO dto = sorted.get(i);
            SkillLevel level = new SkillLevel();
            level.setName(dto.name().trim());
            level.setMinPercentage(dto.minPercentage());
            level.setCountsAsAchieved(dto.countsAsAchieved());
            level.setDisplayOrder(i + 1);
            levels.add(level);
        }
        repository.saveAll(levels);
        eventPublisher.publishEvent(new SkillLevelsChangedEvent());
        return list();
    }

    @Override
    public List<SkillLevelSummary> levels() {
        List<SkillLevel> levels = repository.findAllByOrderByMinPercentageAsc();
        List<SkillLevelSummary> summaries = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            summaries.add(levels.get(i).toSummary(i + 1));
        }
        return summaries;
    }

    @Override
    public SkillLevelSummary levelFor(BigDecimal score) {
        List<SkillLevelSummary> levels = levels();
        SkillLevelSummary hit = levels.isEmpty() ? null : levels.getFirst();
        for (SkillLevelSummary level : levels) {
            if (score != null && score.compareTo(level.minPercentage()) >= 0) {
                hit = level;
            }
        }
        return hit;
    }

    /** One level at 0, unique names and minimums, and achieved levels form an unbroken top band. */
    private static List<SkillLevelDTO> validate(List<SkillLevelDTO> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new IllegalArgumentException("The scale needs at least one level");
        }
        List<SkillLevelDTO> sorted = requested.stream().sorted(Comparator.comparing(SkillLevelDTO::minPercentage)).toList();
        if (sorted.getFirst().minPercentage().signum() != 0) {
            throw new IllegalArgumentException("The lowest level must start at 0%");
        }
        Set<String> names = new HashSet<>();
        Set<BigDecimal> minimums = new HashSet<>();
        boolean achievedSeen = false;
        for (SkillLevelDTO level : sorted) {
            if (!names.add(level.name().trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Level names must be unique: " + level.name().trim());
            }
            if (!minimums.add(level.minPercentage().stripTrailingZeros())) {
                throw new IllegalArgumentException("Two levels can't share the minimum " + level.minPercentage().stripTrailingZeros().toPlainString() + "%");
            }
            if (achievedSeen && !level.countsAsAchieved()) {
                throw new IllegalArgumentException("Achieved levels must be the top levels: " + level.name().trim() + " sits above an achieved level");
            }
            achievedSeen = achievedSeen || level.countsAsAchieved();
        }
        if (!achievedSeen) {
            throw new IllegalArgumentException("Mark at least one level as counting as achieved");
        }
        return sorted;
    }
}
