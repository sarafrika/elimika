package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.CreateSkillsFundSourceRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.CreateSkillsFundTransactionRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundSourceDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundSummaryDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundTransactionDTO;
import apps.sarafrika.elimika.tenancy.entity.SkillsFundSource;
import apps.sarafrika.elimika.tenancy.entity.SkillsFundTransaction;
import apps.sarafrika.elimika.tenancy.factory.SkillsFundFactory;
import apps.sarafrika.elimika.tenancy.repository.SkillsFundSourceRepository;
import apps.sarafrika.elimika.tenancy.repository.SkillsFundTransactionRepository;
import apps.sarafrika.elimika.tenancy.util.enums.SkillsFundTransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules that make the skills fund hold money rather than describe it.
 * <p>
 * Three things are asserted here that the previous implementation could not have satisfied: that a
 * status outside the closed set cannot silently fall out of every total, that no row can be written
 * without saying what currency its amount is in, and that a funding source cannot be removed out from
 * under money the fund has already paid out.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Skills fund aggregation, currency and source removal")
class SkillsFundServiceImplTest {

    private static final String KES = "KES";

    @Mock
    private SkillsFundSourceRepository sourceRepository;
    @Mock
    private SkillsFundTransactionRepository transactionRepository;
    @Mock
    private CurrencyService currencyService;

    private SkillsFundServiceImpl service;

    private final UUID organisationUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SkillsFundServiceImpl(sourceRepository, transactionRepository, currencyService);
        when(currencyService.resolveCurrencyOrDefault(null)).thenReturn(currency(KES));
        when(currencyService.resolveCurrencyOrDefault("")).thenReturn(currency(KES));
        when(currencyService.resolveCurrencyOrDefault(KES)).thenReturn(currency(KES));
        when(currencyService.resolveCurrencyOrDefault("USD")).thenReturn(currency("USD"));
        when(currencyService.resolveCurrencyOrDefault("usd")).thenReturn(currency("USD"));
        when(sourceRepository.save(any(SkillsFundSource.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(SkillsFundTransaction.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("summary aggregation")
    class Summary {

        @Test
        @DisplayName("splits the fund across pending, committed and disbursed totals")
        void aggregatesByStatus() {
            givenSources(source("Grant", "1000"));
            givenTransactions(
                    txn(SkillsFundTransactionStatus.PENDING, "10"),
                    txn(SkillsFundTransactionStatus.ALLOCATED, "20"),
                    txn(SkillsFundTransactionStatus.APPROVED, "30"),
                    txn(SkillsFundTransactionStatus.DISBURSED, "40"));

            SkillsFundSummaryDTO summary = service.getSummary(organisationUuid);

            assertThat(summary.totalBalance()).isEqualByComparingTo("1000");
            assertThat(summary.pending()).isEqualByComparingTo("10");
            assertThat(summary.disbursed()).isEqualByComparingTo("40");
            assertThat(summary.remaining()).isEqualByComparingTo("960");
            assertThat(summary.currencyCode()).isEqualTo(KES);
        }

        @Test
        @DisplayName("counts disbursed money into allocated as well — it was committed before it left")
        void disbursedIsCountedTwice() {
            givenSources(source("Grant", "1000"));
            givenTransactions(
                    txn(SkillsFundTransactionStatus.ALLOCATED, "20"),
                    txn(SkillsFundTransactionStatus.APPROVED, "30"),
                    txn(SkillsFundTransactionStatus.DISBURSED, "40"));

            SkillsFundSummaryDTO summary = service.getSummary(organisationUuid);

            // 20 + 30 + 40: the legacy implementation gave the same answer for a 'Completed' row and a
            // different one for a 'Disbursed' row. Both now mean the same thing and count the same way.
            assertThat(summary.allocated()).isEqualByComparingTo("90");
            assertThat(summary.disbursed()).isEqualByComparingTo("40");
        }

        @Test
        @DisplayName("a pending request is never counted as committed")
        void pendingIsNotCommitted() {
            givenSources(source("Grant", "500"));
            givenTransactions(txn(SkillsFundTransactionStatus.PENDING, "500"));

            SkillsFundSummaryDTO summary = service.getSummary(organisationUuid);

            assertThat(summary.allocated()).isEqualByComparingTo("0");
            assertThat(summary.disbursed()).isEqualByComparingTo("0");
            assertThat(summary.remaining()).isEqualByComparingTo("500");
        }

        @Test
        @DisplayName("soft-deleted sources are outside the balance entirely")
        void softDeletedSourcesAreExcluded() {
            // The repository query already filters them; asserting the service reads through that
            // query rather than findAll is the point.
            givenSources(source("Live grant", "600"));
            givenTransactions();

            assertThat(service.getSummary(organisationUuid).totalBalance()).isEqualByComparingTo("600");
            verify(sourceRepository).findByOrganisationUuidAndDeletedFalseOrderByNameAsc(organisationUuid);
        }

        @Test
        @DisplayName("an empty fund reports the platform currency rather than nothing")
        void emptyFundStillNamesACurrency() {
            givenSources();
            givenTransactions();

            SkillsFundSummaryDTO summary = service.getSummary(organisationUuid);

            assertThat(summary.totalBalance()).isEqualByComparingTo("0");
            assertThat(summary.currencyCode()).isEqualTo(KES);
        }

        @Test
        @DisplayName("refuses to publish one balance for a fund holding two currencies")
        void mixedCurrenciesHaveNoSingleBalance() {
            SkillsFundSource kes = source("Grant", "1000");
            SkillsFundSource usd = source("Donor", "500");
            usd.setCurrencyCode("USD");
            givenSources(kes, usd);
            givenTransactions();

            assertThatThrownBy(() -> service.getSummary(organisationUuid))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("more than one currency");
        }
    }

    @Nested
    @DisplayName("currency on new rows")
    class Currency {

        @Test
        @DisplayName("a source with no stated currency takes the platform default, never null")
        void sourceDefaultsToPlatformCurrency() {
            SkillsFundSourceDTO created = service.addSource(organisationUuid,
                    new CreateSkillsFundSourceRequestDTO("Grant", "Government Grant", new BigDecimal("1000"), null));

            assertThat(created.currencyCode()).isEqualTo(KES);
            assertThat(persistedSource().getCurrencyCode()).isEqualTo(KES);
        }

        @Test
        @DisplayName("a transaction with no stated currency takes the platform default, never null")
        void transactionDefaultsToPlatformCurrency() {
            SkillsFundTransactionDTO created = service.addTransaction(organisationUuid, request(null, null));

            assertThat(created.currencyCode()).isEqualTo(KES);
            assertThat(persistedTransaction().getCurrencyCode()).isEqualTo(KES);
        }

        @Test
        @DisplayName("a stated currency is validated against the platform, not taken on trust")
        void statedCurrencyIsResolved() {
            service.addSource(organisationUuid,
                    new CreateSkillsFundSourceRequestDTO("Donor", "Sponsor", new BigDecimal("50"), "usd"));

            verify(currencyService).resolveCurrencyOrDefault("usd");
        }

        @Test
        @DisplayName("an unknown currency is rejected rather than stored")
        void unknownCurrencyIsRejected() {
            when(currencyService.resolveCurrencyOrDefault("XYZ"))
                    .thenThrow(new ResourceNotFoundException("Currency XYZ was not found"));

            assertThatThrownBy(() -> service.addSource(organisationUuid,
                    new CreateSkillsFundSourceRequestDTO("Grant", null, BigDecimal.ONE, "XYZ")))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(sourceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("recording a transaction")
    class Recording {

        @Test
        @DisplayName("defaults to PENDING when no status is given")
        void defaultsToPending() {
            service.addTransaction(organisationUuid, request(null, null));

            assertThat(persistedTransaction().getStatus()).isEqualTo(SkillsFundTransactionStatus.PENDING);
        }

        @Test
        @DisplayName("carries the beneficiary through, so the recipient is identifiable")
        void carriesTheBeneficiary() {
            UUID beneficiary = UUID.randomUUID();

            SkillsFundTransactionDTO created = service.addTransaction(organisationUuid,
                    request(SkillsFundTransactionStatus.APPROVED, beneficiary));

            assertThat(created.beneficiaryUserUuid()).isEqualTo(beneficiary);
            assertThat(persistedTransaction().getBeneficiaryUserUuid()).isEqualTo(beneficiary);
        }

        @Test
        @DisplayName("keeps the display name alongside the beneficiary")
        void keepsTheDisplayName() {
            SkillsFundTransactionDTO created = service.addTransaction(organisationUuid,
                    request(SkillsFundTransactionStatus.APPROVED, UUID.randomUUID()));

            assertThat(created.targetName()).isEqualTo("John Doe");
        }
    }

    @Nested
    @DisplayName("removing a funding source")
    class Removal {

        @Test
        @DisplayName("hides the source instead of erasing it")
        void softDeletes() {
            SkillsFundSource grant = source("Grant", "1000");
            SkillsFundSource donor = source("Donor", "500");
            when(sourceRepository.findByUuidAndDeletedFalse(grant.getUuid())).thenReturn(Optional.of(grant));
            givenSources(grant, donor);
            givenTransactions(txn(SkillsFundTransactionStatus.DISBURSED, "100"));

            service.deleteSource(grant.getUuid());

            assertThat(grant.isDeleted()).isTrue();
            verify(sourceRepository).save(grant);
            verify(sourceRepository, never()).delete(any());
        }

        @Test
        @DisplayName("refuses when the surviving sources could not cover money already disbursed")
        void refusesWhenItWouldUnderfundWhatIsAlreadySpent() {
            SkillsFundSource grant = source("Government Grant", "1000");
            SkillsFundSource donor = source("Donor", "200");
            when(sourceRepository.findByUuidAndDeletedFalse(grant.getUuid())).thenReturn(Optional.of(grant));
            givenSources(grant, donor);
            // 900 has already left the fund; without the grant only 200 remains to account for it.
            givenTransactions(txn(SkillsFundTransactionStatus.DISBURSED, "900"));

            assertThatThrownBy(() -> service.deleteSource(grant.getUuid()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Government Grant")
                    .hasMessageContaining("900");

            assertThat(grant.isDeleted()).isFalse();
            verify(sourceRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows removal when another source still covers what has been disbursed")
        void allowsWhenCovered() {
            SkillsFundSource grant = source("Grant", "1000");
            SkillsFundSource donor = source("Donor", "900");
            when(sourceRepository.findByUuidAndDeletedFalse(grant.getUuid())).thenReturn(Optional.of(grant));
            givenSources(grant, donor);
            givenTransactions(txn(SkillsFundTransactionStatus.DISBURSED, "900"));

            service.deleteSource(grant.getUuid());

            assertThat(grant.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("pending and allocated money does not block removal — nothing has left the fund")
        void committedButUnpaidMoneyDoesNotBlock() {
            SkillsFundSource grant = source("Grant", "1000");
            when(sourceRepository.findByUuidAndDeletedFalse(grant.getUuid())).thenReturn(Optional.of(grant));
            givenSources(grant);
            givenTransactions(
                    txn(SkillsFundTransactionStatus.PENDING, "800"),
                    txn(SkillsFundTransactionStatus.ALLOCATED, "700"),
                    txn(SkillsFundTransactionStatus.APPROVED, "600"));

            service.deleteSource(grant.getUuid());

            assertThat(grant.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("an already-removed source is not found a second time")
        void alreadyRemovedIsNotFound() {
            UUID sourceUuid = UUID.randomUUID();
            when(sourceRepository.findByUuidAndDeletedFalse(sourceUuid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteSource(sourceUuid))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------------------------

    private void givenSources(SkillsFundSource... sources) {
        when(sourceRepository.findByOrganisationUuidAndDeletedFalseOrderByNameAsc(organisationUuid))
                .thenReturn(List.of(sources));
    }

    private void givenTransactions(SkillsFundTransaction... txns) {
        when(transactionRepository.findByOrganisationUuidOrderByTransactionDateDesc(organisationUuid))
                .thenReturn(List.of(txns));
    }

    private SkillsFundSource persistedSource() {
        ArgumentCaptor<SkillsFundSource> captor = ArgumentCaptor.forClass(SkillsFundSource.class);
        verify(sourceRepository).save(captor.capture());
        return captor.getValue();
    }

    private SkillsFundTransaction persistedTransaction() {
        ArgumentCaptor<SkillsFundTransaction> captor = ArgumentCaptor.forClass(SkillsFundTransaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }

    private SkillsFundSource source(String name, String amount) {
        SkillsFundSource source = SkillsFundFactory.newSource(
                organisationUuid, name, "Grant", new BigDecimal(amount), KES);
        source.setUuid(UUID.randomUUID());
        return source;
    }

    private SkillsFundTransaction txn(SkillsFundTransactionStatus status, String amount) {
        SkillsFundTransaction txn = SkillsFundFactory.newTransaction(
                organisationUuid, "Disbursement", "John Doe", null,
                new BigDecimal(amount), KES, "Allocation", status, LocalDateTime.now());
        txn.setUuid(UUID.randomUUID());
        return txn;
    }

    private CreateSkillsFundTransactionRequestDTO request(SkillsFundTransactionStatus status, UUID beneficiary) {
        return new CreateSkillsFundTransactionRequestDTO(
                "Disbursement - Basic Coding", "John Doe", beneficiary,
                new BigDecimal("250"), null, null, status, null);
    }

    private static PlatformCurrency currency(String code) {
        return new PlatformCurrency(code, 404, code, code, 2, Boolean.TRUE, "KES".equals(code));
    }
}
