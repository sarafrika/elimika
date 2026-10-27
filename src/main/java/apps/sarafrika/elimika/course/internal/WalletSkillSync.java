package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.skills.spi.SkillLevelLookupService;
import apps.sarafrika.elimika.skills.spi.SkillLevelSummary;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Puts an achieved programme skill in the learner's skills wallet as verified. Earned skills are never removed. */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletSkillSync {

    private static final List<ProficiencyLevel> ACHIEVED_PROFICIENCY =
            List.of(ProficiencyLevel.INTERMEDIATE, ProficiencyLevel.ADVANCED, ProficiencyLevel.EXPERT);

    private final LearnerProfileLookupService learnerProfiles;
    private final ProfessionalProfileService professionalProfiles;
    private final SkillLookupService skillLookupService;
    private final SkillLevelLookupService skillLevels;
    private final TrainingProgramRepository programRepository;

    @EventListener
    public void onSkillAchieved(LearnerSkillAchievedEvent event) {
        UUID userUuid = learnerProfiles.findUserUuidByStudentUuid(event.studentUuid()).orElse(null);
        SkillSummary skill = skillLookupService.findByUuids(List.of(event.skillUuid())).stream().findFirst().orElse(null);
        if (userUuid == null || skill == null) {
            log.warn("Skipping wallet sync for student {} skill {}: user or skill missing", event.studentUuid(), event.skillUuid());
            return;
        }
        String programme = programRepository.findByUuid(event.programUuid()).map(TrainingProgram::getTitle).orElse("a programme");
        professionalProfiles.recordVerifiedSkill(userUuid, skill.uuid(), skill.name(), proficiency(event.levelRank()),
                event.levelName() + " in " + programme, event.programUuid());
    }

    /** The lowest achieved level maps to Intermediate, each higher achieved level one step up, capped at Expert. */
    private ProficiencyLevel proficiency(int rank) {
        List<SkillLevelSummary> achieved = skillLevels.levels().stream().filter(SkillLevelSummary::countsAsAchieved).toList();
        int step = 0;
        for (int i = 0; i < achieved.size(); i++) {
            if (achieved.get(i).rank() == rank) {
                step = i;
            }
        }
        return ACHIEVED_PROFICIENCY.get(Math.min(step, ACHIEVED_PROFICIENCY.size() - 1));
    }
}
