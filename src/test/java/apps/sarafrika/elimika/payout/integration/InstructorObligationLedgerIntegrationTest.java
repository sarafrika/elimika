package apps.sarafrika.elimika.payout.integration;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.payout.dto.InstructorObligationDTO;
import apps.sarafrika.elimika.payout.dto.InstructorStatementDTO;
import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import apps.sarafrika.elimika.payout.model.InstructorObligation;
import apps.sarafrika.elimika.payout.repository.InstructorObligationRepository;
import apps.sarafrika.elimika.payout.service.impl.InstructorObligationServiceImpl;
import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.payout.InstructorPayableLookupService.OrganisationInstructorPayable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Exercises the obligation ledger against a real PostgreSQL instance.
 * <p>
 * Three claims cannot be made with mocked repositories, and all three are the ones a finance person
 * would ask about. That the unique constraint really does stop a session being paid for twice; that
 * the payables aggregate really does add up to the rows behind it once some of them are settled; and
 * that the database refuses a settlement carrying no evidence. The last two only fail once the
 * migration and the entity model are put in the same room.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({InstructorObligationServiceImpl.class, JpaConfig.class})
@DisplayName("Instructor obligation ledger")
class InstructorObligationLedgerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        // The real migrations build the schema; Hibernate must not touch it.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private InstructorObligationRepository obligationRepository;
    @Autowired
    private InstructorObligationServiceImpl service;
    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ClassDefinitionLookupService classDefinitionLookupService;
    @MockitoBean
    private InstructorLookupService instructorLookupService;
    @MockitoBean
    private CurrencyService currencyService;

    private UUID organisationUuid;
    private UUID otherOrganisationUuid;
    private UUID pianoClassUuid;
    private UUID guitarClassUuid;
    private UUID instructorUuid;
    private UUID instructorUserUuid;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from instructor_obligations");

        organisationUuid = UUID.randomUUID();
        otherOrganisationUuid = UUID.randomUUID();
        pianoClassUuid = UUID.randomUUID();
        guitarClassUuid = UUID.randomUUID();
        instructorUuid = UUID.randomUUID();
        instructorUserUuid = UUID.randomUUID();

        when(currencyService.resolveCurrencyOrDefault(any()))
                .thenReturn(new PlatformCurrency("KES", 404, "Kenyan Shilling", "KES", 2, true, true));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));
        when(classDefinitionLookupService.findOrganisationUuid(any())).thenReturn(Optional.of(organisationUuid));
        classCharges(pianoClassUuid, "800.00");
        classCharges(guitarClassUuid, "500.00");
    }

    private void classCharges(UUID classDefinitionUuid, String fee) {
        when(classDefinitionLookupService.findByUuid(classDefinitionUuid))
                .thenReturn(Optional.of(new ClassDefinitionSnapshot(
                        classDefinitionUuid, UUID.randomUUID(), null, "Class", "desc",
                        new BigDecimal(fee), ClassVisibility.PRIVATE, LocationType.ONLINE,
                        20, Boolean.TRUE, 30)));
    }

    @Test
    @DisplayName("the same session cannot be paid for twice, even past the pre-check")
    void theUniqueConstraintStopsADoublePayment() {
        UUID sessionUuid = UUID.randomUUID();
        service.accrueForCompletedSession(pianoClassUuid, sessionUuid, instructorUuid, LocalDateTime.now());

        // Bypasses the service's pre-check entirely, which is the point: this proves the database
        // itself refuses, not just the happy path in Java.
        InstructorObligation duplicate = new InstructorObligation();
        duplicate.setOrganisationUuid(organisationUuid);
        duplicate.setInstructorUuid(instructorUuid);
        duplicate.setInstructorUserUuid(instructorUserUuid);
        duplicate.setClassDefinitionUuid(pianoClassUuid);
        duplicate.setSessionUuid(sessionUuid);
        duplicate.setRateAmount(new BigDecimal("800.00"));
        duplicate.setCurrencyCode("KES");
        duplicate.setStatus(InstructorObligationStatus.ACCRUED);
        duplicate.setAccruedAt(LocalDateTime.now());

        // Nothing is asserted after this: a failed flush poisons both the persistence context and the
        // surrounding transaction, so the "only one row survives" claim is made by
        // serviceLevelAccrualIsIdempotent instead, where the second attempt is refused cleanly.
        assertThatThrownBy(() -> obligationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("accruing the same session twice through the service yields one row")
    void serviceLevelAccrualIsIdempotent() {
        UUID sessionUuid = UUID.randomUUID();
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 1, 9, 0);

        Optional<InstructorObligationDTO> first =
                service.accrueForCompletedSession(pianoClassUuid, sessionUuid, instructorUuid, completedAt);
        Optional<InstructorObligationDTO> second =
                service.accrueForCompletedSession(pianoClassUuid, sessionUuid, instructorUuid, completedAt);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(second.get().uuid()).isEqualTo(first.get().uuid());
        assertThat(obligationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("re-rating the class does not change what was already accrued")
    void aRateChangeDoesNotRewriteHistory() {
        UUID firstSession = UUID.randomUUID();
        service.accrueForCompletedSession(pianoClassUuid, firstSession, instructorUuid, LocalDateTime.now());

        classCharges(pianoClassUuid, "200.00");
        UUID secondSession = UUID.randomUUID();
        service.accrueForCompletedSession(pianoClassUuid, secondSession, instructorUuid, LocalDateTime.now());

        BigDecimal firstRate = obligationRepository
                .findByClassDefinitionUuidAndSessionUuidAndInstructorUuid(pianoClassUuid, firstSession, instructorUuid)
                .orElseThrow()
                .getRateAmount();
        BigDecimal secondRate = obligationRepository
                .findByClassDefinitionUuidAndSessionUuidAndInstructorUuid(pianoClassUuid, secondSession, instructorUuid)
                .orElseThrow()
                .getRateAmount();

        assertThat(firstRate).isEqualByComparingTo("800.00");
        assertThat(secondRate).isEqualByComparingTo("200.00");

        // And the organisation's total is 1000, not the 400 the old recompute would have produced.
        OrganisationInstructorPayable payable = onlyPayable();
        assertThat(payable.amountOutstanding()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("payables match the persisted rows, including a partial settlement")
    void payablesAggregateMatchesTheRowsUnderThem() {
        UUID pianoOne = UUID.randomUUID();
        UUID pianoTwo = UUID.randomUUID();
        UUID guitarOne = UUID.randomUUID();
        service.accrueForCompletedSession(pianoClassUuid, pianoOne, instructorUuid, LocalDateTime.now());
        InstructorObligationDTO second =
                service.accrueForCompletedSession(pianoClassUuid, pianoTwo, instructorUuid, LocalDateTime.now())
                        .orElseThrow();
        service.accrueForCompletedSession(guitarClassUuid, guitarOne, instructorUuid, LocalDateTime.now());

        // 800 + 800 + 500 = 2100 accrued across two classes and three sessions.
        OrganisationInstructorPayable beforeSettlement = onlyPayable();
        assertThat(beforeSettlement.amountAccrued()).isEqualByComparingTo("2100.00");
        assertThat(beforeSettlement.amountOutstanding()).isEqualByComparingTo("2100.00");
        assertThat(beforeSettlement.amountSettled()).isEqualByComparingTo("0.00");
        assertThat(beforeSettlement.classCount()).isEqualTo(2);
        assertThat(beforeSettlement.sessionCount()).isEqualTo(3);
        assertThat(beforeSettlement.outstandingSessionCount()).isEqualTo(3);
        assertThat(beforeSettlement.currencyCode()).isEqualTo("KES");

        service.settle(organisationUuid, second.uuid(), "MPESA-QGH7XK2P1L", "part payment", "auditor-1");

        // One of the 800s has been paid. The lifetime total does not move; what is owed does.
        OrganisationInstructorPayable afterSettlement = onlyPayable();
        assertThat(afterSettlement.amountAccrued()).isEqualByComparingTo("2100.00");
        assertThat(afterSettlement.amountOutstanding()).isEqualByComparingTo("1300.00");
        assertThat(afterSettlement.amountSettled()).isEqualByComparingTo("800.00");
        assertThat(afterSettlement.sessionCount()).isEqualTo(3);
        assertThat(afterSettlement.outstandingSessionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("a cancelled obligation leaves what is owed and stays on the record")
    void cancellationRemovesTheDebtWithoutRemovingTheRow() {
        UUID sessionUuid = UUID.randomUUID();
        InstructorObligationDTO accrued = service
                .accrueForCompletedSession(pianoClassUuid, sessionUuid, instructorUuid, LocalDateTime.now())
                .orElseThrow();

        service.cancel(organisationUuid, accrued.uuid(), "Session was marked complete in error", "auditor-1");

        assertThat(service.findPayablesForOrganisation(organisationUuid)).isEmpty();
        assertThat(obligationRepository.findByUuid(accrued.uuid()))
                .get()
                .satisfies(row -> {
                    assertThat(row.getStatus()).isEqualTo(InstructorObligationStatus.CANCELLED);
                    assertThat(row.getStatusNote()).isEqualTo("Session was marked complete in error");
                });
    }

    @Test
    @DisplayName("the database refuses a settlement carrying no evidence")
    void theCheckConstraintRefusesAnUnevidencedSettlement() {
        UUID sessionUuid = UUID.randomUUID();
        service.accrueForCompletedSession(pianoClassUuid, sessionUuid, instructorUuid, LocalDateTime.now());

        assertThatThrownBy(() -> jdbc.update(
                "update instructor_obligations set status = 'SETTLED' where session_uuid = ?", sessionUuid))
                .hasMessageContaining("chk_instructor_obligations_settlement_evidence");
    }

    @Test
    @DisplayName("an obligation belongs to exactly one organisation's ledger")
    void obligationsAreScopedToTheirOrganisation() {
        service.accrueForCompletedSession(pianoClassUuid, UUID.randomUUID(), instructorUuid, LocalDateTime.now());

        assertThat(service.findPayablesForOrganisation(otherOrganisationUuid)).isEmpty();
        assertThat(service.findForOrganisation(otherOrganisationUuid, null, null, PageRequest.of(0, 20)))
                .isEmpty();
    }

    @Test
    @DisplayName("the instructor's statement is the same ledger read from their side")
    void theInstructorStatementMirrorsThePayables() {
        UUID pianoOne = UUID.randomUUID();
        InstructorObligationDTO first = service
                .accrueForCompletedSession(pianoClassUuid, pianoOne, instructorUuid, LocalDateTime.now())
                .orElseThrow();
        service.accrueForCompletedSession(guitarClassUuid, UUID.randomUUID(), instructorUuid, LocalDateTime.now());
        service.settle(organisationUuid, first.uuid(), "BANK-778812", null, "auditor-1");

        InstructorStatementDTO statement = service.getStatement(instructorUserUuid);

        assertThat(statement.instructorUserUuid()).isEqualTo(instructorUserUuid);
        assertThat(statement.lines()).hasSize(1);
        InstructorStatementDTO.Line line = statement.lines().getFirst();
        assertThat(line.organisationUuid()).isEqualTo(organisationUuid);
        assertThat(line.amountAccrued()).isEqualByComparingTo("1300.00");
        assertThat(line.amountSettled()).isEqualByComparingTo("800.00");
        assertThat(line.amountOutstanding()).isEqualByComparingTo("500.00");
        assertThat(line.sessionCount()).isEqualTo(2);
        assertThat(line.outstandingSessionCount()).isEqualTo(1);
    }

    private OrganisationInstructorPayable onlyPayable() {
        List<OrganisationInstructorPayable> payables = service.findPayablesForOrganisation(organisationUuid);
        assertThat(payables).hasSize(1);
        return payables.getFirst();
    }
}
