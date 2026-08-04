package apps.sarafrika.elimika.tenancy.services.impl;

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
import apps.sarafrika.elimika.tenancy.services.SkillsFundService;
import apps.sarafrika.elimika.tenancy.util.enums.SkillsFundTransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SkillsFundServiceImpl implements SkillsFundService {

    private static final String DEFAULT_TRANSACTION_TYPE = "Allocation";

    private final SkillsFundSourceRepository sourceRepository;
    private final SkillsFundTransactionRepository transactionRepository;
    private final CurrencyService currencyService;

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal sum(List<SkillsFundTransaction> txns, Predicate<SkillsFundTransaction> filter) {
        return txns.stream().filter(filter).map(t -> nz(t.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean is(SkillsFundTransaction t, SkillsFundTransactionStatus status) {
        return t.getStatus() == status;
    }

    @Override
    @Transactional(readOnly = true)
    public SkillsFundSummaryDTO getSummary(UUID organisationUuid) {
        List<SkillsFundSource> sources = sourceRepository
                .findByOrganisationUuidAndDeletedFalseOrderByNameAsc(organisationUuid);
        List<SkillsFundTransaction> txns = transactionRepository
                .findByOrganisationUuidOrderByTransactionDateDesc(organisationUuid);

        String currencyCode = fundCurrency(organisationUuid, sources, txns);

        BigDecimal totalBalance = sources.stream()
                .map(s -> nz(s.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // `allocated` is cumulative: money that has been disbursed was committed first. That was the
        // previous behaviour for the legacy status 'Completed', which counted into both totals; it now
        // applies to every committed state rather than depending on which synonym the caller typed.
        BigDecimal allocated = sum(txns, t -> t.getStatus() != null && t.getStatus().isCommitted());
        BigDecimal disbursed = sum(txns, t -> is(t, SkillsFundTransactionStatus.DISBURSED));
        BigDecimal pending = sum(txns, t -> is(t, SkillsFundTransactionStatus.PENDING));
        BigDecimal remaining = totalBalance.subtract(disbursed);

        return new SkillsFundSummaryDTO(totalBalance, allocated, disbursed, pending, remaining, currencyCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillsFundSourceDTO> getSources(UUID organisationUuid) {
        return sourceRepository.findByOrganisationUuidAndDeletedFalseOrderByNameAsc(organisationUuid).stream()
                .map(SkillsFundFactory::toDTO)
                .toList();
    }

    @Override
    public SkillsFundSourceDTO addSource(UUID organisationUuid, CreateSkillsFundSourceRequestDTO request) {
        SkillsFundSource source = SkillsFundFactory.newSource(
                organisationUuid,
                request.name(),
                request.sourceType(),
                nz(request.amount()),
                resolveCurrency(request.currencyCode()));
        return SkillsFundFactory.toDTO(sourceRepository.save(source));
    }

    /**
     * Removes a funding source without erasing it, and refuses when removing it would make the
     * fund's published arithmetic impossible.
     * <p>
     * {@code remaining} is {@code sum(sources) - sum(disbursed)}. Nothing in this schema links a
     * transaction to the source it was drawn from — transactions are scoped only to the organisation
     * — so "does anything reference this source" has no answer to give. What can be answered is
     * whether the fund would still be able to account for money it has already paid out. If taking
     * this source away leaves the surviving sources unable to cover what has been disbursed, the
     * removal is refused: a fund cannot report having spent more than it ever held.
     * <p>
     * Otherwise the row is soft-deleted, following the {@code deleted} flag convention already used
     * by organisations, training branches and domain mappings. A funding source is an input to a
     * balance the organisation has already seen; hiding it is honest, erasing it is not.
     */
    @Override
    public void deleteSource(UUID sourceUuid) {
        SkillsFundSource source = sourceRepository.findByUuidAndDeletedFalse(sourceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Funding source not found: " + sourceUuid));

        UUID organisationUuid = source.getOrganisationUuid();
        BigDecimal survivingBalance = sourceRepository
                .findByOrganisationUuidAndDeletedFalseOrderByNameAsc(organisationUuid).stream()
                .filter(s -> !s.getUuid().equals(sourceUuid))
                .map(s -> nz(s.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal disbursed = sum(
                transactionRepository.findByOrganisationUuidOrderByTransactionDateDesc(organisationUuid),
                t -> is(t, SkillsFundTransactionStatus.DISBURSED));

        if (survivingBalance.compareTo(disbursed) < 0) {
            throw new IllegalStateException(
                    "Funding source '" + source.getName() + "' cannot be removed: the fund has already disbursed "
                            + disbursed + " and the remaining sources would only account for " + survivingBalance
                            + ". Reverse or reassign the disbursements first.");
        }

        source.setDeleted(true);
        sourceRepository.save(source);
        log.info("Soft-deleted skills fund source {} for organisation {}", sourceUuid, organisationUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillsFundTransactionDTO> getTransactions(UUID organisationUuid) {
        return transactionRepository.findByOrganisationUuidOrderByTransactionDateDesc(organisationUuid).stream()
                .map(SkillsFundFactory::toDTO)
                .toList();
    }

    @Override
    public SkillsFundTransactionDTO addTransaction(UUID organisationUuid, CreateSkillsFundTransactionRequestDTO request) {
        String transactionType = request.transactionType() != null && !request.transactionType().isBlank()
                ? request.transactionType()
                : DEFAULT_TRANSACTION_TYPE;

        SkillsFundTransaction txn = SkillsFundFactory.newTransaction(
                organisationUuid,
                request.description(),
                request.targetName(),
                request.beneficiaryUserUuid(),
                nz(request.amount()),
                resolveCurrency(request.currencyCode()),
                transactionType,
                request.status() != null ? request.status() : SkillsFundTransactionStatus.PENDING,
                request.transactionDate() != null ? request.transactionDate() : LocalDateTime.now());
        return SkillsFundFactory.toDTO(transactionRepository.save(txn));
    }

    @Override
    public void deleteTransaction(UUID transactionUuid) {
        SkillsFundTransaction txn = transactionRepository.findByUuid(transactionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionUuid));
        transactionRepository.delete(txn);
    }

    /**
     * The currency a new row is denominated in.
     * <p>
     * A blank code falls back to the platform's declared default rather than a literal, so the answer
     * follows configuration instead of a hard-coded assumption. A supplied code must name a currency
     * the platform actually recognises and has active — an unknown or disabled code is rejected
     * rather than stored.
     */
    private String resolveCurrency(String requested) {
        return currencyService.resolveCurrencyOrDefault(requested).getCode();
    }

    /**
     * The single currency an organisation's fund is denominated in.
     * <p>
     * The KPI roll-up adds sources and transactions together, so those figures only mean something if
     * everything in the fund shares one currency. Before this migration nothing recorded a currency at
     * all and the summary added the numbers regardless; now the condition is at least detectable. A
     * fund holding two currencies has no single balance, so rather than return one that is quietly
     * wrong, this refuses. It cannot fire on today's data — KES is the only active currency and
     * {@link #resolveCurrency} rejects anything else — which is exactly what makes it a usable
     * tripwire for the day a second currency is switched on without this code being revisited.
     */
    private String fundCurrency(UUID organisationUuid,
                                List<SkillsFundSource> sources,
                                List<SkillsFundTransaction> txns) {
        Set<String> currencies = new LinkedHashSet<>();
        sources.stream().map(SkillsFundSource::getCurrencyCode).filter(Objects::nonNull).forEach(currencies::add);
        txns.stream().map(SkillsFundTransaction::getCurrencyCode).filter(Objects::nonNull).forEach(currencies::add);

        if (currencies.isEmpty()) {
            return currencyService.resolveCurrencyOrDefault(null).getCode();
        }
        if (currencies.size() > 1) {
            throw new IllegalStateException(
                    "Skills fund for organisation " + organisationUuid + " holds more than one currency "
                            + currencies + ", so it has no single balance. Split the fund by currency before "
                            + "requesting a summary.");
        }
        return currencies.iterator().next();
    }
}
