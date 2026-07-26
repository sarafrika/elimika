package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.CreateSkillsFundSourceRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.CreateSkillsFundTransactionRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundSourceDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundSummaryDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundTransactionDTO;
import apps.sarafrika.elimika.tenancy.entity.SkillsFundSource;
import apps.sarafrika.elimika.tenancy.entity.SkillsFundTransaction;
import apps.sarafrika.elimika.tenancy.repository.SkillsFundSourceRepository;
import apps.sarafrika.elimika.tenancy.repository.SkillsFundTransactionRepository;
import apps.sarafrika.elimika.tenancy.services.SkillsFundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SkillsFundServiceImpl implements SkillsFundService {

    private final SkillsFundSourceRepository sourceRepository;
    private final SkillsFundTransactionRepository transactionRepository;

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static boolean isStatus(SkillsFundTransaction t, String... statuses) {
        String s = t.getStatus() == null ? "" : t.getStatus().toLowerCase();
        for (String status : statuses) {
            if (s.equals(status.toLowerCase())) return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public SkillsFundSummaryDTO getSummary(UUID organisationUuid) {
        BigDecimal totalBalance = sourceRepository.findByOrganisationUuidOrderByNameAsc(organisationUuid).stream()
                .map(s -> nz(s.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SkillsFundTransaction> txns = transactionRepository.findByOrganisationUuidOrderByTransactionDateDesc(organisationUuid);
        BigDecimal disbursed = txns.stream().filter(t -> isStatus(t, "Completed", "Disbursed"))
                .map(t -> nz(t.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allocated = txns.stream().filter(t -> isStatus(t, "Allocated", "Approved", "Completed"))
                .map(t -> nz(t.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = txns.stream().filter(t -> isStatus(t, "Pending"))
                .map(t -> nz(t.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = totalBalance.subtract(disbursed);

        return new SkillsFundSummaryDTO(totalBalance, allocated, disbursed, pending, remaining);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillsFundSourceDTO> getSources(UUID organisationUuid) {
        return sourceRepository.findByOrganisationUuidOrderByNameAsc(organisationUuid).stream()
                .map(this::toSourceDTO)
                .toList();
    }

    @Override
    public SkillsFundSourceDTO addSource(UUID organisationUuid, CreateSkillsFundSourceRequestDTO request) {
        SkillsFundSource source = SkillsFundSource.builder()
                .organisationUuid(organisationUuid)
                .name(request.name())
                .sourceType(request.sourceType())
                .amount(nz(request.amount()))
                .build();
        return toSourceDTO(sourceRepository.save(source));
    }

    @Override
    public void deleteSource(UUID sourceUuid) {
        SkillsFundSource source = sourceRepository.findByUuid(sourceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Funding source not found: " + sourceUuid));
        sourceRepository.delete(source);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillsFundTransactionDTO> getTransactions(UUID organisationUuid) {
        return transactionRepository.findByOrganisationUuidOrderByTransactionDateDesc(organisationUuid).stream()
                .map(this::toTransactionDTO)
                .toList();
    }

    @Override
    public SkillsFundTransactionDTO addTransaction(UUID organisationUuid, CreateSkillsFundTransactionRequestDTO request) {
        SkillsFundTransaction txn = SkillsFundTransaction.builder()
                .organisationUuid(organisationUuid)
                .description(request.description())
                .targetName(request.targetName())
                .amount(nz(request.amount()))
                .transactionType(request.transactionType() != null && !request.transactionType().isBlank() ? request.transactionType() : "Allocation")
                .status(request.status() != null && !request.status().isBlank() ? request.status() : "Pending")
                .transactionDate(request.transactionDate() != null ? request.transactionDate() : java.time.LocalDateTime.now())
                .build();
        return toTransactionDTO(transactionRepository.save(txn));
    }

    @Override
    public void deleteTransaction(UUID transactionUuid) {
        SkillsFundTransaction txn = transactionRepository.findByUuid(transactionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionUuid));
        transactionRepository.delete(txn);
    }

    private SkillsFundSourceDTO toSourceDTO(SkillsFundSource s) {
        return new SkillsFundSourceDTO(s.getUuid(), s.getOrganisationUuid(), s.getName(), s.getSourceType(), nz(s.getAmount()), s.getCreatedDate());
    }

    private SkillsFundTransactionDTO toTransactionDTO(SkillsFundTransaction t) {
        return new SkillsFundTransactionDTO(
                t.getUuid(), t.getOrganisationUuid(), t.getDescription(), t.getTargetName(),
                nz(t.getAmount()), t.getTransactionType(), t.getStatus(), t.getTransactionDate(), t.getCreatedDate());
    }
}
