package apps.sarafrika.elimika.notifications.model;

import apps.sarafrika.elimika.notifications.api.NotificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<UserNotificationPreferences, Long> {
    
    List<UserNotificationPreferences> findByUserUuid(UUID userUuid);
    
    Optional<UserNotificationPreferences> findByUserUuidAndCategory(UUID userUuid, NotificationCategory category);
    
    @Query("SELECT p FROM UserNotificationPreferences p WHERE p.userUuid = :userUuid AND p.category = :category")
    Optional<UserNotificationPreferences> findUserPreference(@Param("userUuid") UUID userUuid, 
                                                            @Param("category") NotificationCategory category);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM UserNotificationPreferences p " +
           "WHERE p.userUuid = :userUuid AND p.category = :category AND p.emailEnabled = true")
    boolean isEmailEnabledForCategory(@Param("userUuid") UUID userUuid, 
                                    @Param("category") NotificationCategory category);
    
    boolean existsByUserUuid(UUID userUuid);
}