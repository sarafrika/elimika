package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.services.UserService;
import apps.sarafrika.elimika.tenancy.spi.UserProfileLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Delegates to {@link UserService} so the SPI and {@code /users/me} can never drift apart. */
@Service
@RequiredArgsConstructor
class UserProfileLookupServiceImpl implements UserProfileLookupService {

    private final UserService userService;

    @Override
    public UserDTO getUserProfile(UUID userUuid) {
        return userService.getUserByUuid(userUuid);
    }
}
