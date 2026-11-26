package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.notifications.preferences.spi.NotificationPreferencesService;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserNumberService;
import apps.sarafrika.elimika.tenancy.spi.ManagedLearnerAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManagedLearnerAccountServiceImpl implements ManagedLearnerAccountService {

    private final UserRepository userRepository;
    private final UserNumberService userNumberService;
    private final NotificationPreferencesService notificationPreferencesService;

    /** No email, username or Keycloak id: {@code chk_users_email_required_for_login} keeps such rows sign-in-less. */
    @Override
    @Transactional
    public UUID createManagedLearnerAccount(ManagedLearnerAccount account) {
        User user = new User();
        user.setFirstName(account.firstName().trim());
        user.setMiddleName(blankToNull(account.middleName()));
        user.setLastName(account.lastName().trim());
        user.setDob(account.dob());
        user.setUserNo(userNumberService.nextUserNo());
        user.setActive(true);
        User saved = userRepository.save(user);

        notificationPreferencesService.initializeUserPreferences(saved.getUuid());
        log.info("Created guardian-managed learner account {}", saved.getUuid());
        return saved.getUuid();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
