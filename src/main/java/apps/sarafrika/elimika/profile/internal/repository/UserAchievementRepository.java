package apps.sarafrika.elimika.profile.internal.repository;

import apps.sarafrika.elimika.profile.internal.model.UserAchievement;
import apps.sarafrika.elimika.profile.internal.model.UserCertification;
import apps.sarafrika.elimika.profile.internal.model.UserCompetency;
import apps.sarafrika.elimika.profile.internal.model.UserDocument;
import apps.sarafrika.elimika.profile.internal.model.UserEducation;
import apps.sarafrika.elimika.profile.internal.model.UserExperience;
import apps.sarafrika.elimika.profile.internal.model.UserMembership;
import apps.sarafrika.elimika.profile.internal.model.UserPortfolioItem;
import apps.sarafrika.elimika.profile.internal.model.UserProfessionalProfile;
import apps.sarafrika.elimika.profile.internal.model.UserSkill;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAchievementRepository extends UserOwnedRepository<UserAchievement> {
}
