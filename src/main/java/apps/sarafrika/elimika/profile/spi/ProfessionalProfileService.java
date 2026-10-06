package apps.sarafrika.elimika.profile.spi;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The user-owned professional profile (basics, wallet sections, verification) every domain reads and writes. */
public interface ProfessionalProfileService {

    /** The user's basics; all fields null when nothing was saved yet. */
    ProfessionalProfileDTO getBasics(UUID userUuid);

    Map<UUID, ProfessionalProfileDTO> getBasics(Collection<UUID> userUuids);

    /** Replaces the basics with exactly these values. */
    ProfessionalProfileDTO saveBasics(UUID userUuid, ProfessionalProfileDTO basics);

    /** Overlays the non-null values onto the saved basics. */
    ProfessionalProfileDTO mergeBasics(UUID userUuid, ProfessionalProfileDTO changes);

    ProfileSummaryDTO getSummary(UUID userUuid);

    ProfileSectionService<UserSkillDTO> skills();

    ProfileSectionService<UserEducationDTO> education();

    ProfileSectionService<UserExperienceDTO> experience();

    ProfileSectionService<UserMembershipDTO> memberships();

    ProfileSectionService<UserCertificationDTO> certifications();

    ProfileSectionService<UserPortfolioItemDTO> portfolio();

    ProfileSectionService<UserCompetencyDTO> competencies();

    ProfileSectionService<UserAchievementDTO> achievements();

    ProfileSectionService<UserDocumentDTO> documents();

    /** Records a platform admin's verdict on one item; it holds for every domain the user has. */
    default void verify(UUID userUuid, ProfileSection section, UUID itemUuid, ProfileVerificationRequest request) {
        verify(userUuid, section, itemUuid, request, null);
    }

    /** As above, naming the verifier; null means the current caller. */
    void verify(UUID userUuid, ProfileSection section, UUID itemUuid, ProfileVerificationRequest request,
                String verifiedBy);

    /** The owner of an item in a section, if the item exists. */
    Optional<UUID> ownerOf(ProfileSection section, UUID itemUuid);

    /** Whether this storage key is the file of one of the user's documents. */
    boolean ownsDocumentFile(UUID userUuid, String filePath);

    long countUnverifiedDocuments();

    long countDocumentsExpiringBetween(LocalDate start, LocalDate end);
}
