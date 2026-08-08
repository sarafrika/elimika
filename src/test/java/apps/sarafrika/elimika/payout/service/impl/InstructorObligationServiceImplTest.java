package apps.sarafrika.elimika.payout.service.impl;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.payout.dto.InstructorObligationDTO;
import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import apps.sarafrika.elimika.payout.model.InstructorObligation;
import apps.sarafrika.elimika.payout.repository.InstructorObligationRepository;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules that make an obligation an obligation rather than a recomputation.
 * <p>
 * The rate-change test is the one that matters most: it is the failure the persisted ledger exists
 * to prevent. The old implementation multiplied completed sessions by the class' <em>current</em>
 * training fee, so re-rating a class silently changed what had been owed for work already delivered.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Instructor obligation accrual and settlement")
class InstructorObligationServiceImplTest {

    @Mock
    private InstructorObligationRepository obligationRepository;
    @Mock
    private ClassDefinitionLookupService classDefinitionLookupService;
    @Mock
    private InstructorLookupService instructorLookupService;
    @Mock
    private CurrencyService currencyService;

    private InstructorObligationServiceImpl service;

    private final UUID organisationUuid = UUID.randomUUID();
    private final UUID classDefinitionUuid = UUID.randomUUID();
    private final UUID instructorUuid = UUID.randomUUID();
    private final UUID instructorUserUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new InstructorObligationServiceImpl(
                obligationRepository, classDefinitionLookupService, instructorLookupService, currencyService);

        when(classDefinitionLookupService.findOrganisationUuid(classDefinitionUuid))
                .thenReturn(Optional.of(organisationUuid));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));
        when(currencyService.resolveCurrencyOrDefault(null))
                .thenReturn(new PlatformCurrency("KES", 404, "Kenyan Shilling", "KES", 2, true, true));
        when(obligationRepository.findByClassDefinitionUuidAndSessionUuidAndInstructorUuid(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(obligationRepository.saveAndFlush(any(InstructorObligation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void trainingFeeIs(String fee) {
        BigDecimal instructorPay = new BigDecimal(fee);
        when(classDefinitionLookupService.findByUuid(classDefinitionUuid))
                .thenReturn(Optional.of(new ClassDefinitionSnapshot(
                        classDefinitionUuid, UUID.randomUUID(), null, "Piano Grade 3", "desc",
                        instructorPay.add(new BigDecimal("500.00")), instructorPay,
                        ClassVisibility.PRIVATE, LocationType.ONLINE,
                        20, Boolean.TRUE, 30)));
    }

    @Test
    @DisplayName("one row per session, at the rate that stood when it completed")
    void accrualWritesOneRowAtTheLiveRate() {
        trainingFeeIs("800.00");
        UUID sessionUuid = UUID.randomUUID();
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 1, 10, 0);

        Optional<InstructorObligationDTO> accrued =
                service.accrueForCompletedSession(classDefinitionUuid, sessionUuid, instructorUuid, completedAt, 60);

        ArgumentCaptor<InstructorObligation> captor = ArgumentCaptor.forClass(InstructorObligation.class);
        verify(obligationRepository).saveAndFlush(captor.capture());
        InstructorObligation saved = captor.getValue();

        assertThat(accrued).isPresent();
        assertThat(saved.getRateAmount()).isEqualByComparingTo("800.00");
        assertThat(saved.getCurrencyCode()).isEqualTo("KES");
        assertThat(saved.getOrganisationUuid()).isEqualTo(organisationUuid);
        assertThat(saved.getInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(saved.getInstructorUserUuid()).isEqualTo(instructorUserUuid);
        assertThat(saved.getSessionUuid()).isEqualTo(sessionUuid);
        assertThat(saved.getStatus()).isEqualTo(InstructorObligationStatus.ACCRUED);
        assertThat(saved.getAccruedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("a longer session earns proportionally more at the same hourly rate")
    void durationScalesTheAccruedAmount() {
        trainingFeeIs("2000.00");

        service.accrueForCompletedSession(
                classDefinitionUuid, UUID.randomUUID(), instructorUuid, LocalDateTime.now(), 120);

        ArgumentCaptor<InstructorObligation> captor = ArgumentCaptor.forClass(InstructorObligation.class);
        verify(obligationRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRateAmount()).isEqualByComparingTo("4000.00");
    }

    @Test
    @DisplayName("a half-hour session earns half the hourly rate")
    void aShortSessionEarnsAFraction() {
        trainingFeeIs("2000.00");

        service.accrueForCompletedSession(
                classDefinitionUuid, UUID.randomUUID(), instructorUuid, LocalDateTime.now(), 30);

        ArgumentCaptor<InstructorObligation> captor = ArgumentCaptor.forClass(InstructorObligation.class);
        verify(obligationRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRateAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("a missing duration falls back to the flat rate rather than paying nothing")
    void anUnknownDurationDoesNotZeroThePay() {
        trainingFeeIs("2000.00");

        service.accrueForCompletedSession(
                classDefinitionUuid, UUID.randomUUID(), instructorUuid, LocalDateTime.now(), null);

        ArgumentCaptor<InstructorObligation> captor = ArgumentCaptor.forClass(InstructorObligation.class);
        verify(obligationRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRateAmount()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("re-rating a class leaves what was already earned alone")
    void aLaterRateChangeDoesNotAlterAnEarlierObligation() {
        trainingFeeIs("800.00");
        UUID firstSession = UUID.randomUUID();
        service.accrueForCompletedSession(classDefinitionUuid, firstSession, instructorUuid, LocalDateTime.now(), 60);

        ArgumentCaptor<InstructorObligation> captor = ArgumentCaptor.forClass(InstructorObligation.class);
        verify(obligationRepository).saveAndFlush(captor.capture());
        InstructorObligation firstRow = captor.getValue();

        // The organisation halves the rate card. Nothing goes back and touches the row above.
        trainingFeeIs("400.00");
        UUID secondSession = UUID.randomUUID();
        service.accrueForCompletedSession(classDefinitionUuid, secondSession, instructorUuid, LocalDateTime.now(), 60);

        ArgumentCaptor<InstructorObligation> secondCaptor = ArgumentCaptor.forClass(InstructorObligation.class);
        verify(obligationRepository, org.mockito.Mockito.times(2)).saveAndFlush(secondCaptor.capture());

        assertThat(firstRow.getRateAmount()).isEqualByComparingTo("800.00");
        assertThat(secondCaptor.getAllValues().get(1).getRateAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("the same session accruing twice yields one row")
    void accrualIsIdempotent() {
        trainingFeeIs("800.00");
        UUID sessionUuid = UUID.randomUUID();
        InstructorObligation existing = existingObligation(sessionUuid, InstructorObligationStatus.ACCRUED);
        when(obligationRepository.findByClassDefinitionUuidAndSessionUuidAndInstructorUuid(
                classDefinitionUuid, sessionUuid, instructorUuid))
                .thenReturn(Optional.of(existing));

        Optional<InstructorObligationDTO> accrued =
                service.accrueForCompletedSession(classDefinitionUuid, sessionUuid, instructorUuid, LocalDateTime.now(), 60);

        assertThat(accrued).isPresent();
        assertThat(accrued.get().uuid()).isEqualTo(existing.getUuid());
        verify(obligationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("a concurrent redelivery that races past the pre-check is absorbed by the unique constraint")
    void accrualSurvivesTheIdempotencyRace() {
        trainingFeeIs("800.00");
        UUID sessionUuid = UUID.randomUUID();
        InstructorObligation winner = existingObligation(sessionUuid, InstructorObligationStatus.ACCRUED);

        when(obligationRepository.findByClassDefinitionUuidAndSessionUuidAndInstructorUuid(
                classDefinitionUuid, sessionUuid, instructorUuid))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(obligationRepository.saveAndFlush(any(InstructorObligation.class)))
                .thenThrow(new DataIntegrityViolationException("uq_instructor_obligations_session"));

        Optional<InstructorObligationDTO> accrued =
                service.accrueForCompletedSession(classDefinitionUuid, sessionUuid, instructorUuid, LocalDateTime.now(), 60);

        assertThat(accrued).isPresent();
        assertThat(accrued.get().uuid()).isEqualTo(winner.getUuid());
    }

    @Test
    @DisplayName("a class nobody owns accrues nothing")
    void noOrganisationMeansNoObligation() {
        trainingFeeIs("800.00");
        when(classDefinitionLookupService.findOrganisationUuid(classDefinitionUuid)).thenReturn(Optional.empty());

        assertThat(service.accrueForCompletedSession(
                classDefinitionUuid, UUID.randomUUID(), instructorUuid, LocalDateTime.now(), 60)).isEmpty();
        verify(obligationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("a class with no training fee accrues nothing")
    void noRateMeansNoObligation() {
        trainingFeeIs("0.00");

        assertThat(service.accrueForCompletedSession(
                classDefinitionUuid, UUID.randomUUID(), instructorUuid, LocalDateTime.now(), 60)).isEmpty();
        verify(obligationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("the debt is still recorded when the instructor's user cannot be resolved")
    void anUnresolvableUserDoesNotLoseTheDebt() {
        trainingFeeIs("800.00");
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.empty());

        Optional<InstructorObligationDTO> accrued = service.accrueForCompletedSession(
                classDefinitionUuid, UUID.randomUUID(), instructorUuid, LocalDateTime.now(), 60);

        assertThat(accrued).isPresent();
        assertThat(accrued.get().instructorUserUuid()).isNull();
        assertThat(accrued.get().rateAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("settling transitions state and records the reference and the actor")
    void settlementRecordsEvidence() {
        InstructorObligation obligation = existingObligation(UUID.randomUUID(), InstructorObligationStatus.ACCRUED);
        when(obligationRepository.findByUuid(obligation.getUuid())).thenReturn(Optional.of(obligation));
        when(obligationRepository.save(any(InstructorObligation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstructorObligationDTO settled = service.settle(
                organisationUuid, obligation.getUuid(), "MPESA-QGH7XK2P1L", "August payroll", "auditor-1");

        assertThat(settled.status()).isEqualTo(InstructorObligationStatus.SETTLED);
        assertThat(settled.settlementReference()).isEqualTo("MPESA-QGH7XK2P1L");
        assertThat(settled.settledBy()).isEqualTo("auditor-1");
        assertThat(settled.statusNote()).isEqualTo("August payroll");
        assertThat(settled.settledAt()).isNotNull();
    }

    @Test
    @DisplayName("settling twice is refused")
    void settlingTwiceIsRefused() {
        InstructorObligation obligation = existingObligation(UUID.randomUUID(), InstructorObligationStatus.SETTLED);
        when(obligationRepository.findByUuid(obligation.getUuid())).thenReturn(Optional.of(obligation));

        assertThatThrownBy(() -> service.settle(
                organisationUuid, obligation.getUuid(), "MPESA-SECOND-GO", null, "auditor-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be settled");
        verify(obligationRepository, never()).save(any());
    }

    @Test
    @DisplayName("a settlement with no reference is refused")
    void settlementWithoutAReferenceIsRefused() {
        InstructorObligation obligation = existingObligation(UUID.randomUUID(), InstructorObligationStatus.ACCRUED);
        when(obligationRepository.findByUuid(obligation.getUuid())).thenReturn(Optional.of(obligation));

        assertThatThrownBy(() -> service.settle(organisationUuid, obligation.getUuid(), "  ", null, "auditor-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("another organisation cannot settle this organisation's obligation")
    void crossTenantSettlementIsRefused() {
        InstructorObligation obligation = existingObligation(UUID.randomUUID(), InstructorObligationStatus.ACCRUED);
        when(obligationRepository.findByUuid(obligation.getUuid())).thenReturn(Optional.of(obligation));
        UUID otherOrganisation = UUID.randomUUID();

        // Reported as missing rather than forbidden, so the endpoint cannot be used to discover that
        // another tenant's obligation exists.
        assertThatThrownBy(() -> service.settle(
                otherOrganisation, obligation.getUuid(), "MPESA-NOT-MINE", null, "intruder"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(obligationRepository, never()).save(any());
    }

    @Test
    @DisplayName("an already-settled obligation cannot be cancelled")
    void cancellingASettledObligationIsRefused() {
        InstructorObligation obligation = existingObligation(UUID.randomUUID(), InstructorObligationStatus.SETTLED);
        when(obligationRepository.findByUuid(obligation.getUuid())).thenReturn(Optional.of(obligation));

        assertThatThrownBy(() -> service.cancel(
                organisationUuid, obligation.getUuid(), "changed my mind", "auditor-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been settled");
    }

    @Test
    @DisplayName("cancelling keeps the row and the reason")
    void cancellationKeepsTheRow() {
        InstructorObligation obligation = existingObligation(UUID.randomUUID(), InstructorObligationStatus.ACCRUED);
        when(obligationRepository.findByUuid(obligation.getUuid())).thenReturn(Optional.of(obligation));
        when(obligationRepository.save(any(InstructorObligation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstructorObligationDTO cancelled = service.cancel(
                organisationUuid, obligation.getUuid(), "Session was rescheduled", "auditor-1");

        assertThat(cancelled.status()).isEqualTo(InstructorObligationStatus.CANCELLED);
        assertThat(cancelled.statusNote()).isEqualTo("Session was rescheduled");
        assertThat(cancelled.rateAmount()).isEqualByComparingTo("800.00");
    }

    private InstructorObligation existingObligation(UUID sessionUuid, InstructorObligationStatus status) {
        InstructorObligation obligation = new InstructorObligation();
        obligation.setUuid(UUID.randomUUID());
        obligation.setOrganisationUuid(organisationUuid);
        obligation.setInstructorUuid(instructorUuid);
        obligation.setInstructorUserUuid(instructorUserUuid);
        obligation.setClassDefinitionUuid(classDefinitionUuid);
        obligation.setSessionUuid(sessionUuid);
        obligation.setRateAmount(new BigDecimal("800.00"));
        obligation.setCurrencyCode("KES");
        obligation.setStatus(status);
        obligation.setAccruedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        return obligation;
    }
}
