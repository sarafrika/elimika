package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.CreateSkillsFundSourceRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.CreateSkillsFundTransactionRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundSourceDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundSummaryDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundTransactionDTO;

import java.util.List;
import java.util.UUID;

/**
 * An organisation's skills fund — funding sources, transactions and computed KPIs.
 */
public interface SkillsFundService {

    SkillsFundSummaryDTO getSummary(UUID organisationUuid);

    List<SkillsFundSourceDTO> getSources(UUID organisationUuid);

    SkillsFundSourceDTO addSource(UUID organisationUuid, CreateSkillsFundSourceRequestDTO request);

    void deleteSource(UUID sourceUuid);

    List<SkillsFundTransactionDTO> getTransactions(UUID organisationUuid);

    SkillsFundTransactionDTO addTransaction(UUID organisationUuid, CreateSkillsFundTransactionRequestDTO request);

    void deleteTransaction(UUID transactionUuid);
}
