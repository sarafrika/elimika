package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import apps.sarafrika.elimika.student.dto.LearnerSkillGoalDTO;
import apps.sarafrika.elimika.student.internal.LearnerSkillGoalStore;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.service.LearnerSkillGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearnerSkillGoalServiceImpl implements LearnerSkillGoalService {

    private final LearnerSkillGoalStore store;
    private final StudentRepository studentRepository;
    private final SkillLookupService skillLookupService;
    private final DomainSecurityService domainSecurityService;

    @Override
    public List<LearnerSkillGoalDTO> getGoals(UUID studentUuid) {
        requireStudent(studentUuid);
        return toDtos(store.findByStudent(studentUuid));
    }

    @Override
    @Transactional
    public List<LearnerSkillGoalDTO> replaceGoals(UUID studentUuid, List<UUID> skillUuids) {
        requireStudent(studentUuid);
        List<UUID> requested = skillUuids == null ? List.of() : skillUuids;
        if (requested.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("skill_uuids cannot contain null");
        }
        Set<UUID> distinct = new LinkedHashSet<>(requested);
        if (distinct.size() != requested.size()) {
            throw new IllegalArgumentException("skill_uuids contains duplicates");
        }
        Map<UUID, SkillSummary> skills = distinct.isEmpty() ? Map.of()
                : skillLookupService.findByUuids(distinct).stream()
                .collect(Collectors.toMap(SkillSummary::uuid, Function.identity(), (a, b) -> a));
        Set<UUID> existing = new HashSet<>(store.findSkillUuids(studentUuid));
        for (UUID skillUuid : distinct) {
            SkillSummary skill = skills.get(skillUuid);
            if (skill == null) {
                throw new IllegalArgumentException("Unknown skill: " + skillUuid);
            }
            if (!skill.active() && !existing.contains(skillUuid)) {
                throw new IllegalArgumentException("Skill " + skill.name() + " is retired and cannot be added");
            }
        }
        UUID actor = domainSecurityService.getCurrentUserUuid();
        store.replace(studentUuid, List.copyOf(distinct), LearnerSkillGoalStore.SOURCE_SELF,
                actor == null ? "system" : actor.toString());
        return toDtos(store.findByStudent(studentUuid));
    }

    private void requireStudent(UUID studentUuid) {
        if (studentUuid == null || !studentRepository.existsByUuid(studentUuid)) {
            throw new ResourceNotFoundException("Student with UUID " + studentUuid + " not found");
        }
    }

    private List<LearnerSkillGoalDTO> toDtos(List<LearnerSkillGoalStore.Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, SkillSummary> skills = skillLookupService
                .findByUuids(rows.stream().map(LearnerSkillGoalStore.Row::skillUuid).toList()).stream()
                .collect(Collectors.toMap(SkillSummary::uuid, Function.identity(), (a, b) -> a));
        return rows.stream().map(row -> {
            SkillSummary skill = skills.get(row.skillUuid());
            return new LearnerSkillGoalDTO(row.skillUuid(),
                    skill == null ? null : skill.name(),
                    skill == null ? null : skill.slug(),
                    row.source() == null ? null : row.source().toLowerCase(Locale.ROOT),
                    row.createdDate());
        }).toList();
    }
}
