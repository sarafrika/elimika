package apps.sarafrika.elimika.timetabling.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.student.spi.StudentLookupService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassEnrolmentGateServiceImplTest {

    private static final UUID CLASS = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID STUDENT = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private TimetableService timetableService;
    @Mock
    private StudentLookupService studentLookupService;

    private ClassEnrolmentGateServiceImpl gate;

    @BeforeEach
    void setUp() {
        gate = new ClassEnrolmentGateServiceImpl(timetableService, studentLookupService);
        when(studentLookupService.findStudentUuidByUserUuid(USER)).thenReturn(Optional.of(STUDENT));
    }

    @Test
    void moneyAlreadyCollectedIsRecordedForALearnerWhoIsAlreadyEnrolled() {
        when(timetableService.getClassEnrolmentEligibility(CLASS, STUDENT))
                .thenReturn(eligibility(true, true, true, "You are already enrolled in this class."));

        assertThat(gate.findEnrolmentBlocker(CLASS, USER)).contains("You are already enrolled in this class.");
        assertThat(gate.findCaptureBlocker(CLASS, USER)).isEmpty();
    }

    @Test
    void aWindowThatClosedWhileTheLearnerWasPayingDoesNotRefuseThePayment() {
        when(timetableService.getClassEnrolmentEligibility(CLASS, STUDENT))
                .thenReturn(eligibility(true, true, false, "Registration for this class closed on 4 November 2026."));

        assertThat(gate.findCaptureBlocker(CLASS, USER)).isEmpty();
    }

    @Test
    void aLearnerOutsideTheAgeLimitsStillBlocksCapture() {
        when(timetableService.getClassEnrolmentEligibility(CLASS, STUDENT))
                .thenReturn(eligibility(true, false, false, "This class is for ages 18 and over. Your profile says you are 12."));

        assertThat(gate.findCaptureBlocker(CLASS, USER)).contains("This class is for ages 18 and over. Your profile says you are 12.");
    }

    private ClassEnrolmentEligibilityDTO eligibility(boolean dateOfBirthOnFile, boolean ageOk, boolean alreadyEnrolled, String reason) {
        return new ClassEnrolmentEligibilityDTO(false, 30, null, null, dateOfBirthOnFile, ageOk, true, alreadyEnrolled,
                true, null, reason);
    }
}
