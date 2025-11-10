package apps.sarafrika.elimika.shared.service;

import apps.sarafrika.elimika.shared.exceptions.AgeRestrictionException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Period;
import java.util.UUID;

/**
 * Centralized service for enforcing age restrictions during enrollments.
 */
@Service
@RequiredArgsConstructor
public class AgeVerificationService {

    private final StudentLookupService studentLookupService;
    private final UserLookupService userLookupService;

    /**
     * Ensures the student's age falls within the provided bounds. When both limits are null, the
     * verification is skipped.
     *
     * @param studentUuid Student identifier
     * @param minimumAge  Optional minimum age (inclusive)
     * @param maximumAge  Optional maximum age (inclusive)
     * @param contextLabel Human readable context (e.g., course title) for error messaging
     */
    public void verifyStudentAge(UUID studentUuid,
                                 Integer minimumAge,
                                 Integer maximumAge,
                                 String contextLabel) {
        if (studentUuid == null || (minimumAge == null && maximumAge == null)) {
            return;
        }

        UUID userUuid = studentLookupService.getStudentUserUuid(studentUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Student with UUID %s not found", studentUuid)));

        LocalDate dateOfBirth = userLookupService.getUserDateOfBirth(userUuid)
                .orElseThrow(() -> new AgeRestrictionException(
                        String.format("Date of birth is required to enroll in %s", buildContext(contextLabel))));

        int age = Period.between(dateOfBirth, LocalDate.now(ZoneOffset.UTC)).getYears();
        String context = buildContext(contextLabel);

        if (minimumAge != null && age < minimumAge) {
            throw new AgeRestrictionException(String.format(
                    "Student age %d is below the minimum age %d required for %s", age, minimumAge, context));
        }

        if (maximumAge != null && age > maximumAge) {
            throw new AgeRestrictionException(String.format(
                    "Student age %d exceeds the maximum age %d allowed for %s", age, maximumAge, context));
        }
    }

    private String buildContext(String contextLabel) {
        if (StringUtils.hasText(contextLabel)) {
            return contextLabel.trim();
        }
        return "the selected enrollment";
    }
}
