package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.shared.spi.ClassEnrolmentGateService;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Timetabling-backed implementation of {@link ClassEnrolmentGateService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassEnrolmentGateServiceImpl implements ClassEnrolmentGateService {

    private final TimetableService timetableService;
    private final StudentLookupService studentLookupService;

    @Override
    public Optional<String> findEnrolmentBlocker(UUID classDefinitionUuid, UUID userUuid) {
        if (classDefinitionUuid == null || userUuid == null) {
            return Optional.empty();
        }
        UUID studentUuid = studentLookupService.findStudentUuidByUserUuid(userUuid).orElse(null);
        if (studentUuid == null) {
            // Somebody without a student profile cannot be judged against student rules, and this
            // gate exists to stop wrongful charges, not to invent new reasons to refuse one.
            return Optional.empty();
        }
        ClassEnrolmentEligibilityDTO eligibility =
                timetableService.getClassEnrolmentEligibility(classDefinitionUuid, studentUuid);
        return eligibility.eligible() ? Optional.empty() : Optional.ofNullable(eligibility.reason());
    }

    @Override
    public boolean reserveSeats(UUID classDefinitionUuid, UUID userUuid, java.time.LocalDateTime reservedUntil) {
        UUID studentUuid = resolveStudent(userUuid);
        if (classDefinitionUuid == null || studentUuid == null) {
            return false;
        }
        return timetableService.reserveSeatsForClass(classDefinitionUuid, studentUuid, reservedUntil);
    }

    @Override
    public void releaseSeats(UUID classDefinitionUuid, UUID userUuid) {
        UUID studentUuid = resolveStudent(userUuid);
        if (classDefinitionUuid == null || studentUuid == null) {
            return;
        }
        timetableService.releaseSeatsForClass(classDefinitionUuid, studentUuid);
    }

    private UUID resolveStudent(UUID userUuid) {
        return userUuid == null
                ? null
                : studentLookupService.findStudentUuidByUserUuid(userUuid).orElse(null);
    }
}
