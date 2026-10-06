package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorProfileBridge;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.ProfileSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Counts the wallet tabs that have content on the owner's shared wallet; Verification counts once any item is verified. */
@Component
@RequiredArgsConstructor
class CourseCreatorSkillsWallet {

    static final int SECTION_TOTAL = 7;

    private static final List<ProfileSection> TABS = List.of(ProfileSection.SKILLS, ProfileSection.PORTFOLIO,
            ProfileSection.CERTIFICATIONS, ProfileSection.COMPETENCIES, ProfileSection.EXPERIENCE,
            ProfileSection.ACHIEVEMENTS);

    private final ProfessionalProfileService profileService;
    private final CourseCreatorProfileBridge bridge;

    int completedSections(UUID creatorUuid) {
        UUID userUuid = bridge.findUserUuid(creatorUuid).orElse(null);
        if (userUuid == null) {
            return 0;
        }
        ProfileSummaryDTO summary = profileService.getSummary(userUuid);
        Map<String, Long> counts = summary.sectionCounts();
        int completed = (int) TABS.stream().filter(tab -> counts.getOrDefault(tab.path(), 0L) > 0).count();
        return completed + (summary.verifiedItems() > 0 ? 1 : 0);
    }

    boolean hasSkills(UUID creatorUuid) {
        return bridge.findUserUuid(creatorUuid).map(userUuid -> profileService.skills().count(userUuid) > 0).orElse(false);
    }
}
