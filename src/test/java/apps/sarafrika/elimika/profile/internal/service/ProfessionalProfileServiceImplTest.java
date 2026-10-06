package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserProfessionalProfile;
import apps.sarafrika.elimika.profile.internal.repository.UserCertificationRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserCompetencyRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserProfessionalProfileRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserSkillRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileChangedEvent;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.ProfileVerificationRequest;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Professional profile: shared basics and admin verification")
class ProfessionalProfileServiceImplTest {

    @Mock private UserProfessionalProfileRepository profileRepository;
    @Mock private UserSkillRepository skillRepository;
    @Mock private UserCertificationRepository certificationRepository;
    @Mock private UserCompetencyRepository competencyRepository;
    @Mock private UserSkillSection skillSection;
    @Mock private UserEducationSection educationSection;
    @Mock private UserExperienceSection experienceSection;
    @Mock private UserMembershipSection membershipSection;
    @Mock private UserCertificationSection certificationSection;
    @Mock private UserPortfolioSection portfolioSection;
    @Mock private UserCompetencySection competencySection;
    @Mock private UserAchievementSection achievementSection;
    @Mock private UserDocumentSection documentSection;
    @Mock private DomainSecurityService domainSecurityService;
    @Mock private ObjectProvider<DomainSecurityService> domainSecurityServiceProvider;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProfessionalProfileServiceImpl service;

    private final UUID userUuid = UUID.randomUUID();
    private final UUID itemUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(domainSecurityServiceProvider.getObject()).thenReturn(domainSecurityService);
        when(skillSection.section()).thenReturn(ProfileSection.SKILLS);
        when(educationSection.section()).thenReturn(ProfileSection.EDUCATION);
        when(experienceSection.section()).thenReturn(ProfileSection.EXPERIENCE);
        when(membershipSection.section()).thenReturn(ProfileSection.MEMBERSHIPS);
        when(certificationSection.section()).thenReturn(ProfileSection.CERTIFICATIONS);
        when(portfolioSection.section()).thenReturn(ProfileSection.PORTFOLIO);
        when(competencySection.section()).thenReturn(ProfileSection.COMPETENCIES);
        when(achievementSection.section()).thenReturn(ProfileSection.ACHIEVEMENTS);
        when(documentSection.section()).thenReturn(ProfileSection.DOCUMENTS);
        when(profileRepository.save(any(UserProfessionalProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("an admin verdict lands on the item in its section, named by the caller")
    void verifyRecordsTheVerdict() {
        UUID admin = UUID.randomUUID();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(admin);

        service.verify(userUuid, ProfileSection.SKILLS, itemUuid,
                new ProfileVerificationRequest(WalletVerificationStatus.VERIFIED, "seen the repo"));

        verify(domainSecurityService).enforceNotSelfApprovingProfile(userUuid, "profile");
        verify(skillSection).verify(userUuid, itemUuid, WalletVerificationStatus.VERIFIED, "seen the repo", admin.toString());
    }

    @Test
    @DisplayName("PENDING is not a verdict, and unverified sections cannot take one")
    void verifyRejectsPendingAndUnverifiableSections() {
        assertThatThrownBy(() -> service.verify(userUuid, ProfileSection.SKILLS, itemUuid,
                new ProfileVerificationRequest(WalletVerificationStatus.PENDING, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.verify(userUuid, ProfileSection.EDUCATION, itemUuid,
                new ProfileVerificationRequest(WalletVerificationStatus.VERIFIED, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(skillSection, never()).verify(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("an admin who also holds a domain cannot verify their own items")
    void selfApprovalIsRefused() {
        doThrow(new AccessDeniedException("own profile"))
                .when(domainSecurityService).enforceNotSelfApprovingProfile(eq(userUuid), any());

        assertThatThrownBy(() -> service.verify(userUuid, ProfileSection.COMPETENCIES, itemUuid,
                new ProfileVerificationRequest(WalletVerificationStatus.VERIFIED, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(competencySection, never()).verify(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("a domain's partial edit keeps the basics it did not send, and is announced with the result")
    void mergeBasicsKeepsUnsentValues() {
        UserProfessionalProfile stored = new UserProfessionalProfile();
        stored.setUserUuid(userUuid);
        stored.setBio("Teaches robotics");
        stored.setProfessionalHeadline("STEM trainer");
        when(profileRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(stored));

        ProfessionalProfileDTO saved = service.mergeBasics(userUuid,
                new ProfessionalProfileDTO(null, null, "Robotics lead", null, "Kisumu", null, null, null));

        assertThat(saved.bio()).isEqualTo("Teaches robotics");
        assertThat(saved.professionalHeadline()).isEqualTo("Robotics lead");
        assertThat(saved.locationName()).isEqualTo("Kisumu");
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new ProfessionalProfileChangedEvent(userUuid, ProfileSection.BASICS, saved));
    }
}
