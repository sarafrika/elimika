package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserSkill;
import apps.sarafrika.elimika.profile.internal.repository.UserSkillRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileChangedEvent;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Profile skills: one claim per user, verification reset on a changed claim")
class UserSkillSectionTest {

    private final UserSkillRepository repository = mock(UserSkillRepository.class);
    @SuppressWarnings("unchecked")
    private final GenericSpecificationBuilder<UserSkill> specs = mock(GenericSpecificationBuilder.class);
    private final org.springframework.context.ApplicationEventPublisher events =
            mock(org.springframework.context.ApplicationEventPublisher.class);
    private final SkillLookupService skillLookupService = mock(SkillLookupService.class);
    private final UserSkillSection section = new UserSkillSection(repository, specs, events, skillLookupService);

    private final UUID userUuid = UUID.randomUUID();
    private UserSkill verifiedJava;

    @BeforeEach
    void setUp() {
        verifiedJava = new UserSkill();
        verifiedJava.setUuid(UUID.randomUUID());
        verifiedJava.setUserUuid(userUuid);
        verifiedJava.setSkillName("Java Programming");
        verifiedJava.setSkillUuid(UUID.randomUUID());
        verifiedJava.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        verifiedJava.setEvidence("github.com/me");
        verifiedJava.recordVerdict(WalletVerificationStatus.VERIFIED, "checked", LocalDateTime.now());
        when(repository.findByUserUuidOrderByIdAsc(userUuid)).thenReturn(List.of(verifiedJava));
        when(repository.findByUuid(verifiedJava.getUuid())).thenReturn(Optional.of(verifiedJava));
        when(repository.save(any(UserSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillLookupService.resolve(anyCollection())).thenReturn(Map.of());
    }

    @Test
    @DisplayName("re-adding a skill under another spelling updates the existing one and keeps its verdict")
    void identicalClaimUpdatesInsteadOfDuplicating() {
        UserSkillDTO saved = section.create(userUuid,
                UserSkillDTO.claim("  java   PROGRAMMING ", ProficiencyLevel.EXPERT, null, null));

        assertThat(saved.uuid()).isEqualTo(verifiedJava.getUuid());
        assertThat(saved.proficiencyLevel()).isEqualTo(ProficiencyLevel.EXPERT);
        assertThat(saved.evidence()).isEqualTo("github.com/me");
        assertThat(saved.verificationStatus()).isEqualTo(WalletVerificationStatus.VERIFIED);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.argThat(skill -> skill != verifiedJava));
    }

    @Test
    @DisplayName("new evidence sends a verified skill back to PENDING")
    void changedEvidenceResetsVerification() {
        UserSkillDTO saved = section.update(userUuid, verifiedJava.getUuid(),
                UserSkillDTO.claim("Java Programming", ProficiencyLevel.EXPERT, "a new portfolio", null));

        assertThat(saved.verificationStatus()).isEqualTo(WalletVerificationStatus.PENDING);
        assertThat(saved.verifiedAt()).isNull();
        assertThat(saved.verificationNotes()).isNull();
    }

    @Test
    @DisplayName("a renamed skill goes back to PENDING and is re-resolved against the taxonomy")
    void renameResetsVerification() {
        UserSkillDTO saved = section.update(userUuid, verifiedJava.getUuid(),
                UserSkillDTO.claim("Kotlin", ProficiencyLevel.EXPERT, "github.com/me", null));

        assertThat(saved.verificationStatus()).isEqualTo(WalletVerificationStatus.PENDING);
        assertThat(saved.skillUuid()).isNull();
    }

    @Test
    @DisplayName("changing only the level or assessment date keeps the verdict")
    void levelChangeKeepsVerification() {
        UserSkillDTO saved = section.update(userUuid, verifiedJava.getUuid(),
                UserSkillDTO.claim("java programming", ProficiencyLevel.ADVANCED, "github.com/me", LocalDate.now()));

        assertThat(saved.verificationStatus()).isEqualTo(WalletVerificationStatus.VERIFIED);
        assertThat(saved.proficiencyLevel()).isEqualTo(ProficiencyLevel.ADVANCED);
    }

    @Test
    @DisplayName("another user's skill reads as not found and is left alone")
    void foreignItemIsNotFound() {
        assertThatThrownBy(() -> section.update(UUID.randomUUID(), verifiedJava.getUuid(),
                UserSkillDTO.claim("Hijacked", ProficiencyLevel.EXPERT, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> section.delete(UUID.randomUUID(), verifiedJava.getUuid()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(verifiedJava.getSkillName()).isEqualTo("Java Programming");
    }

    @Test
    @DisplayName("every write announces the changed section for its owner")
    void writesPublishTheChange() {
        section.update(userUuid, verifiedJava.getUuid(),
                UserSkillDTO.claim("Java Programming", ProficiencyLevel.EXPERT, "github.com/me", null));

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());
        assertThat(published.getValue()).isEqualTo(
                new ProfessionalProfileChangedEvent(userUuid, ProfileSection.SKILLS, null));
    }
}
