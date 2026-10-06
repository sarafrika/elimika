package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserProfessionalProfile;
import apps.sarafrika.elimika.profile.internal.repository.UserCertificationRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserCompetencyRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserProfessionalProfileRepository;
import apps.sarafrika.elimika.profile.internal.repository.UserSkillRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileChangedEvent;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.ProfileSectionService;
import apps.sarafrika.elimika.profile.spi.ProfileSummaryDTO;
import apps.sarafrika.elimika.profile.spi.ProfileVerificationRequest;
import apps.sarafrika.elimika.profile.spi.UserAchievementDTO;
import apps.sarafrika.elimika.profile.spi.UserCertificationDTO;
import apps.sarafrika.elimika.profile.spi.UserCompetencyDTO;
import apps.sarafrika.elimika.profile.spi.UserDocumentDTO;
import apps.sarafrika.elimika.profile.spi.UserEducationDTO;
import apps.sarafrika.elimika.profile.spi.UserExperienceDTO;
import apps.sarafrika.elimika.profile.spi.UserMembershipDTO;
import apps.sarafrika.elimika.profile.spi.UserPortfolioItemDTO;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfessionalProfileServiceImpl implements ProfessionalProfileService {

    private final UserProfessionalProfileRepository profileRepository;
    private final UserSkillRepository skillRepository;
    private final UserCertificationRepository certificationRepository;
    private final UserCompetencyRepository competencyRepository;
    private final UserSkillSection skillSection;
    private final UserEducationSection educationSection;
    private final UserExperienceSection experienceSection;
    private final UserMembershipSection membershipSection;
    private final UserCertificationSection certificationSection;
    private final UserPortfolioSection portfolioSection;
    private final UserCompetencySection competencySection;
    private final UserAchievementSection achievementSection;
    private final UserDocumentSection documentSection;
    private final DomainSecurityService domainSecurityService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public ProfessionalProfileDTO getBasics(UUID userUuid) {
        if (userUuid == null) {
            return ProfessionalProfileDTO.empty(null);
        }
        return profileRepository.findByUserUuid(userUuid)
                .map(ProfessionalProfileServiceImpl::toDto)
                .orElseGet(() -> ProfessionalProfileDTO.empty(userUuid));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ProfessionalProfileDTO> getBasics(Collection<UUID> userUuids) {
        if (userUuids == null || userUuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ProfessionalProfileDTO> result = new HashMap<>();
        profileRepository.findByUserUuidIn(userUuids)
                .forEach(profile -> result.put(profile.getUserUuid(), toDto(profile)));
        return result;
    }

    @Override
    public ProfessionalProfileDTO saveBasics(UUID userUuid, ProfessionalProfileDTO basics) {
        Objects.requireNonNull(userUuid, "userUuid");
        UserProfessionalProfile profile = profileRepository.findByUserUuid(userUuid).orElseGet(() -> {
            UserProfessionalProfile created = new UserProfessionalProfile();
            created.setUserUuid(userUuid);
            return created;
        });
        ProfessionalProfileDTO values = basics == null ? ProfessionalProfileDTO.empty(userUuid) : basics;
        profile.setBio(values.bio());
        profile.setProfessionalHeadline(values.professionalHeadline());
        profile.setWebsite(values.website());
        profile.setLocationName(values.locationName());
        profile.setLatitude(values.latitude());
        profile.setLongitude(values.longitude());
        ProfessionalProfileDTO saved = toDto(profileRepository.save(profile));
        eventPublisher.publishEvent(new ProfessionalProfileChangedEvent(userUuid, ProfileSection.BASICS, saved));
        return saved;
    }

    @Override
    public ProfessionalProfileDTO mergeBasics(UUID userUuid, ProfessionalProfileDTO changes) {
        ProfessionalProfileDTO current = getBasics(userUuid);
        return saveBasics(userUuid, changes == null ? current : changes.orElse(current));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileSummaryDTO getSummary(UUID userUuid) {
        ProfessionalProfileDTO basics = getBasics(userUuid);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ProfileSectionSupport<?, ?> section : sections()) {
            counts.put(section.section().path(), section.count(userUuid));
        }
        long verified = skillRepository.countByUserUuidAndVerificationStatus(userUuid, WalletVerificationStatus.VERIFIED)
                + certificationRepository.countByUserUuidAndVerificationStatus(userUuid, WalletVerificationStatus.VERIFIED)
                + competencyRepository.countByUserUuidAndVerificationStatus(userUuid, WalletVerificationStatus.VERIFIED)
                + documentSection.countVerified(userUuid);
        int total = counts.size() + 1;
        int completed = (basics.isBasicsComplete() ? 1 : 0)
                + (int) counts.values().stream().filter(count -> count > 0).count();
        return new ProfileSummaryDTO(userUuid, basics, counts, verified, completed, total, completed * 100 / total);
    }

    @Override
    public ProfileSectionService<UserSkillDTO> skills() {
        return skillSection;
    }

    @Override
    public ProfileSectionService<UserEducationDTO> education() {
        return educationSection;
    }

    @Override
    public ProfileSectionService<UserExperienceDTO> experience() {
        return experienceSection;
    }

    @Override
    public ProfileSectionService<UserMembershipDTO> memberships() {
        return membershipSection;
    }

    @Override
    public ProfileSectionService<UserCertificationDTO> certifications() {
        return certificationSection;
    }

    @Override
    public ProfileSectionService<UserPortfolioItemDTO> portfolio() {
        return portfolioSection;
    }

    @Override
    public ProfileSectionService<UserCompetencyDTO> competencies() {
        return competencySection;
    }

    @Override
    public ProfileSectionService<UserAchievementDTO> achievements() {
        return achievementSection;
    }

    @Override
    public ProfileSectionService<UserDocumentDTO> documents() {
        return documentSection;
    }

    /**
     * Only admins reach this (route guard); an admin who also holds a domain cannot verify their own items.
     */
    @Override
    public void verify(UUID userUuid, ProfileSection section, UUID itemUuid, ProfileVerificationRequest request,
                       String verifiedBy) {
        if (request == null || request.status() == null || request.status() == WalletVerificationStatus.PENDING) {
            throw new IllegalArgumentException("A verification must mark the item VERIFIED or REJECTED");
        }
        if (section == null || !section.verifiable()) {
            throw new IllegalArgumentException("Items in this section are not verified: " + section);
        }
        domainSecurityService.enforceNotSelfApprovingProfile(userUuid, "profile");
        String verifier = verifiedBy;
        if (verifier == null || verifier.isBlank()) {
            UUID caller = domainSecurityService.getCurrentUserUuid();
            verifier = caller == null ? "system" : caller.toString();
        }
        support(section).verify(userUuid, itemUuid, request.status(), request.notes(), verifier);
        if (section == ProfileSection.DOCUMENTS && request.status() == WalletVerificationStatus.VERIFIED) {
            documentSection.find(itemUuid).ifPresent(this::notifyDocumentVerified);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> ownerOf(ProfileSection section, UUID itemUuid) {
        return support(section).ownerOf(itemUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ownsDocumentFile(UUID userUuid, String filePath) {
        return documentSection.ownsFile(userUuid, filePath);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnverifiedDocuments() {
        return documentSection.countUnverified();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDocumentsExpiringBetween(LocalDate start, LocalDate end) {
        return documentSection.countExpiringBetween(start, end);
    }

    private List<ProfileSectionSupport<?, ?>> sections() {
        return List.of(skillSection, educationSection, experienceSection, membershipSection, certificationSection,
                portfolioSection, competencySection, achievementSection, documentSection);
    }

    private ProfileSectionSupport<?, ?> support(ProfileSection section) {
        return sections().stream()
                .filter(candidate -> candidate.section() == section)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown profile section: " + section));
    }

    private void notifyDocumentVerified(UserDocumentDTO document) {
        String title = document.title() == null ? "Your document" : document.title();
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                document.userUuid(),
                "PROFILE_DOCUMENT_VERIFIED",
                "POPUP",
                "Document verified",
                title + " has been verified.",
                "/dashboard/profile/documents",
                Map.of("document_uuid", document.uuid(), "document_title", title, "profile_type", "user"),
                "profile-document-verified:user:" + document.uuid()
        ));
    }

    private static ProfessionalProfileDTO toDto(UserProfessionalProfile profile) {
        return new ProfessionalProfileDTO(profile.getUserUuid(), profile.getBio(), profile.getProfessionalHeadline(),
                profile.getWebsite(), profile.getLocationName(), profile.getLatitude(), profile.getLongitude(),
                profile.getLastModifiedDate() != null ? profile.getLastModifiedDate() : profile.getCreatedDate());
    }
}
