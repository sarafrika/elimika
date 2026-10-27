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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletSkillSyncTest {

    @Mock private LearnerProfileLookupService learnerProfiles;
    @Mock private ProfessionalProfileService professionalProfiles;
    @Mock private SkillLookupService skillLookupService;
    @Mock private SkillLevelLookupService skillLevels;
    @Mock private TrainingProgramRepository programRepository;
    @InjectMocks private WalletSkillSync sync;

    private final UUID student = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private final UUID skill = UUID.randomUUID();
    private final UUID program = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(learnerProfiles.findUserUuidByStudentUuid(student)).thenReturn(Optional.of(user));
        when(skillLookupService.findByUuids(List.of(skill)))
                .thenReturn(List.of(new SkillSummary(skill, "Performance technique", "performance-technique", null, List.of(), true)));
        TrainingProgram programme = new TrainingProgram();
        programme.setTitle("Foundation Music Certificate");
        when(programRepository.findByUuid(program)).thenReturn(Optional.of(programme));
        when(skillLevels.levels()).thenReturn(List.of(
                new SkillLevelSummary(UUID.randomUUID(), "Foundation", BigDecimal.ZERO, false, 1),
                new SkillLevelSummary(UUID.randomUUID(), "Developing", new BigDecimal("50"), false, 2),
                new SkillLevelSummary(UUID.randomUUID(), "Proficient", new BigDecimal("70"), true, 3),
                new SkillLevelSummary(UUID.randomUUID(), "Advanced", new BigDecimal("85"), true, 4)));
    }

    @Test
    void lowestAchievedLevelIsIntermediateAndTheNextIsAdvanced() {
        sync.onSkillAchieved(new LearnerSkillAchievedEvent(student, program, skill, "Proficient", 3, UUID.randomUUID()));
        verify(professionalProfiles).recordVerifiedSkill(eq(user), eq(skill), eq("Performance technique"),
                eq(ProficiencyLevel.INTERMEDIATE), eq("Proficient in Foundation Music Certificate"), eq(program));

        sync.onSkillAchieved(new LearnerSkillAchievedEvent(student, program, skill, "Advanced", 4, UUID.randomUUID()));
        verify(professionalProfiles).recordVerifiedSkill(eq(user), eq(skill), eq("Performance technique"),
                eq(ProficiencyLevel.ADVANCED), eq("Advanced in Foundation Music Certificate"), eq(program));
    }

    @Test
    void skipsWhenTheStudentHasNoAccount() {
        when(learnerProfiles.findUserUuidByStudentUuid(student)).thenReturn(Optional.empty());
        sync.onSkillAchieved(new LearnerSkillAchievedEvent(student, program, skill, "Proficient", 3, UUID.randomUUID()));
        verify(professionalProfiles, never()).recordVerifiedSkill(any(), any(), any(), any(), any(), any());
    }
}
