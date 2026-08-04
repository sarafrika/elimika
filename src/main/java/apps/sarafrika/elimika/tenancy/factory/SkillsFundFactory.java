package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.SkillsFundSourceDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundTransactionDTO;
import apps.sarafrika.elimika.tenancy.entity.SkillsFundSource;
import apps.sarafrika.elimika.tenancy.entity.SkillsFundTransaction;
import apps.sarafrika.elimika.tenancy.util.enums.SkillsFundTransactionStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Assembly of skills fund rows and their read models.
 * <p>
 * No builders: every field a money row is created with is visible in one place, which is the point
 * for anything that will later move real value. Amount and currency are always set together —
 * neither method offers a way to write one without the other.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkillsFundFactory {

    public static SkillsFundSource newSource(
            UUID organisationUuid,
            String name,
            String sourceType,
            BigDecimal amount,
            String currencyCode) {

        SkillsFundSource source = new SkillsFundSource();
        source.setOrganisationUuid(organisationUuid);
        source.setName(name);
        source.setSourceType(sourceType);
        source.setAmount(zeroIfNull(amount));
        source.setCurrencyCode(currencyCode);
        source.setDeleted(false);
        return source;
    }

    public static SkillsFundTransaction newTransaction(
            UUID organisationUuid,
            String description,
            String targetName,
            UUID beneficiaryUserUuid,
            BigDecimal amount,
            String currencyCode,
            String transactionType,
            SkillsFundTransactionStatus status,
            LocalDateTime transactionDate) {

        SkillsFundTransaction transaction = new SkillsFundTransaction();
        transaction.setOrganisationUuid(organisationUuid);
        transaction.setDescription(description);
        transaction.setTargetName(targetName);
        transaction.setBeneficiaryUserUuid(beneficiaryUserUuid);
        transaction.setAmount(zeroIfNull(amount));
        transaction.setCurrencyCode(currencyCode);
        transaction.setTransactionType(transactionType);
        transaction.setStatus(status);
        transaction.setTransactionDate(transactionDate);
        return transaction;
    }

    public static SkillsFundSourceDTO toDTO(SkillsFundSource source) {
        return new SkillsFundSourceDTO(
                source.getUuid(),
                source.getOrganisationUuid(),
                source.getName(),
                source.getSourceType(),
                zeroIfNull(source.getAmount()),
                source.getCurrencyCode(),
                source.getCreatedDate());
    }

    public static SkillsFundTransactionDTO toDTO(SkillsFundTransaction transaction) {
        return new SkillsFundTransactionDTO(
                transaction.getUuid(),
                transaction.getOrganisationUuid(),
                transaction.getDescription(),
                transaction.getTargetName(),
                transaction.getBeneficiaryUserUuid(),
                zeroIfNull(transaction.getAmount()),
                transaction.getCurrencyCode(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getTransactionDate(),
                transaction.getCreatedDate());
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
