package apps.sarafrika.elimika.shared.service;

import apps.sarafrika.elimika.shared.exceptions.AgeRestrictionException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgeVerificationServiceTest {

    @Mock
    private StudentLookupService studentLookupService;

    @Mock
    private UserLookupService userLookupService;

    private AgeVerificationService ageVerificationService;

    @BeforeEach
    void setUp() {
        ageVerificationService = new AgeVerificationService(studentLookupService, userLookupService);
    }

    @Test
    void shouldSkipVerificationWhenNoLimitsConfigured() {
        ageVerificationService.verifyStudentAge(UUID.randomUUID(), null, null, "context");
        verifyNoInteractions(studentLookupService, userLookupService);
    }

    @Test
    void shouldAllowEnrollmentWhenAgeWithinRange() {
        UUID studentUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        when(studentLookupService.getStudentUserUuid(studentUuid)).thenReturn(Optional.of(userUuid));
        LocalDate dob = LocalDate.now(ZoneOffset.UTC).minusYears(15);
        when(userLookupService.getUserDateOfBirth(userUuid)).thenReturn(Optional.of(dob));

        assertThatCode(() -> ageVerificationService.verifyStudentAge(studentUuid, 12, 18, "course Algebra"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectEnrollmentWhenBelowMinimumAge() {
        UUID studentUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        when(studentLookupService.getStudentUserUuid(studentUuid)).thenReturn(Optional.of(userUuid));
        LocalDate dob = LocalDate.now(ZoneOffset.UTC).minusYears(12);
        when(userLookupService.getUserDateOfBirth(userUuid)).thenReturn(Optional.of(dob));

        assertThatThrownBy(() -> ageVerificationService.verifyStudentAge(studentUuid, 16, null, "course Algebra"))
                .isInstanceOf(AgeRestrictionException.class)
                .hasMessageContaining("minimum age 16");
    }

    @Test
    void shouldThrowWhenStudentNotFound() {
        UUID studentUuid = UUID.randomUUID();
        when(studentLookupService.getStudentUserUuid(studentUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ageVerificationService.verifyStudentAge(studentUuid, 10, 20, "course Algebra"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
