package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.UserRecipientDTO;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Resolves a user from their exact user number so they can be picked as a transfer recipient.
 * <p>
 * Exact match only: no partial matching and no other fields, so the route cannot be used to browse
 * the user table. Unknown, malformed and inactive numbers all answer the same not-found.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRecipientLookupService {

    /** User numbers are nine digits (see the {@code users_user_no_format} check constraint). */
    private static final Pattern USER_NO_FORMAT = Pattern.compile("^[0-9]{9}$");
    private static final String NOT_FOUND = "No active user with that user number";

    private final UserRepository userRepository;

    public UserRecipientDTO lookupByUserNo(String userNo) {
        if (userNo == null || !USER_NO_FORMAT.matcher(userNo).matches()) {
            throw new ResourceNotFoundException(NOT_FOUND);
        }
        return userRepository.findByUserNo(userNo)
                .filter(User::isActive)
                .map(user -> new UserRecipientDTO(user.getUuid(), maskedName(user.getFirstName(), user.getLastName())))
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    /** "Wilfred N." from Wilfred Njuguna; the first name alone when there is no last name. */
    static String maskedName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (last.isEmpty()) {
            return first;
        }
        String initial = last.substring(0, last.offsetByCodePoints(0, 1)) + ".";
        return first.isEmpty() ? initial : first + " " + initial;
    }
}
