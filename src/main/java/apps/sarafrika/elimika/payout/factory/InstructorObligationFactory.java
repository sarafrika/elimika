package apps.sarafrika.elimika.payout.factory;

import apps.sarafrika.elimika.payout.dto.InstructorObligationDTO;
import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import apps.sarafrika.elimika.payout.model.InstructorObligation;
import apps.sarafrika.elimika.shared.spi.payout.InstructorPayableLookupService.OrganisationInstructorPayable;
import apps.sarafrika.elimika.payout.dto.InstructorStatementDTO;
import apps.sarafrika.elimika.payout.repository.InstructorPayableAggregate;
import apps.sarafrika.elimika.payout.repository.InstructorStatementAggregate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Assembly of obligation rows and their read models. No builders: every field an obligation is
 * created with is visible in one place, which for a money record is the point.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorObligationFactory {

    /**
     * A newly accrued obligation. The rate is passed in already resolved and is copied verbatim —
     * this method is the moment the historical rate is fixed, and nothing downstream recomputes it.
     */
    public static InstructorObligation accrue(
            UUID organisationUuid,
            UUID instructorUuid,
            UUID instructorUserUuid,
            UUID classDefinitionUuid,
            UUID sessionUuid,
            BigDecimal rateAmount,
            String currencyCode,
            LocalDateTime accruedAt,
            java.time.LocalDate sessionDate) {

        InstructorObligation obligation = new InstructorObligation();
        obligation.setOrganisationUuid(organisationUuid);
        obligation.setInstructorUuid(instructorUuid);
        obligation.setInstructorUserUuid(instructorUserUuid);
        obligation.setClassDefinitionUuid(classDefinitionUuid);
        obligation.setSessionUuid(sessionUuid);
        obligation.setSessionDate(sessionDate);
        obligation.setRateAmount(rateAmount);
        obligation.setCurrencyCode(currencyCode);
        obligation.setStatus(InstructorObligationStatus.ACCRUED);
        obligation.setAccruedAt(accruedAt);
        return obligation;
    }

    public static InstructorObligationDTO toDTO(InstructorObligation obligation) {
        return new InstructorObligationDTO(
                obligation.getUuid(),
                obligation.getOrganisationUuid(),
                obligation.getInstructorUuid(),
                obligation.getInstructorUserUuid(),
                obligation.getClassDefinitionUuid(),
                obligation.getSessionUuid(),
                obligation.getRateAmount(),
                obligation.getCurrencyCode(),
                obligation.getStatus(),
                obligation.getAccruedAt(),
                obligation.getSettledAt(),
                obligation.getSettlementReference(),
                obligation.getSettledBy(),
                obligation.getStatusNote()
        );
    }

    public static OrganisationInstructorPayable toPayable(InstructorPayableAggregate aggregate) {
        BigDecimal outstanding = zeroIfNull(aggregate.amountOutstanding());
        BigDecimal settled = zeroIfNull(aggregate.amountSettled());
        return new OrganisationInstructorPayable(
                aggregate.instructorUuid(),
                aggregate.currencyCode(),
                outstanding,
                settled,
                outstanding.add(settled),
                zeroIfNull(aggregate.classCount()),
                zeroIfNull(aggregate.sessionCount()),
                zeroIfNull(aggregate.outstandingSessionCount())
        );
    }

    public static InstructorStatementDTO.Line toStatementLine(InstructorStatementAggregate aggregate) {
        BigDecimal outstanding = zeroIfNull(aggregate.amountOutstanding());
        BigDecimal settled = zeroIfNull(aggregate.amountSettled());
        return new InstructorStatementDTO.Line(
                aggregate.organisationUuid(),
                aggregate.currencyCode(),
                outstanding,
                settled,
                outstanding.add(settled),
                zeroIfNull(aggregate.sessionCount()),
                zeroIfNull(aggregate.outstandingSessionCount())
        );
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }
}
