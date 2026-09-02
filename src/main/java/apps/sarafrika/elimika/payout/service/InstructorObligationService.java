package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.payout.dto.InstructorObligationDTO;
import apps.sarafrika.elimika.payout.dto.InstructorStatementDTO;
import apps.sarafrika.elimika.payout.dto.MonthlyPayoutPointDTO;
import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The ledger of what organisations owe their instructors for delivered sessions.
 * <p>
 * Settlement here is off-platform: the organisation pays by its own means and records that it did.
 * Nothing on this interface moves money.
 */
public interface InstructorObligationService {

    /**
     * Records what an organisation owes for one delivered session, at the rate that stands now.
     * <p>
     * Idempotent per {@code (classDefinition, session, instructor)} — the completion event that
     * drives this is redelivered on retry and on restart, and a session must not be paid for twice.
     *
     * @return the obligation that now exists, whether this call created it or a previous one did;
     *         empty when the session is not payable at all (no instructor, no organisation, no rate)
     */
    Optional<InstructorObligationDTO> accrueForCompletedSession(
            UUID classDefinitionUuid, UUID sessionUuid, UUID instructorUuid, LocalDateTime completedAt,
            Integer durationMinutes);

    /**
     * Records that the organisation has paid this obligation outside the platform.
     *
     * @param organisationUuid    the organisation claiming to have paid; an obligation belonging to a
     *                            different organisation is reported as not found rather than refused,
     *                            so one tenant cannot probe another's ledger
     * @param settlementReference the organisation's own reference for the payment
     * @param settledBy           the acting user, recorded so a disputed payment has a name on it
     * @throws IllegalStateException when the obligation is not {@code ACCRUED} — settling twice, or
     *                               settling something cancelled, is refused rather than absorbed
     */
    InstructorObligationDTO settle(
            UUID organisationUuid, UUID obligationUuid, String settlementReference, String note, String settledBy);

    /**
     * Withdraws an obligation that should never have accrued. The row stays, excluded from what is
     * owed, carrying the reason.
     *
     * @throws IllegalStateException when the obligation has already been settled — money that has
     *                               been paid cannot be un-owed
     */
    InstructorObligationDTO cancel(UUID organisationUuid, UUID obligationUuid, String reason, String cancelledBy);

    /**
     * An organisation's obligation rows, newest first, optionally narrowed to one instructor or one
     * status.
     */
    Page<InstructorObligationDTO> findForOrganisation(
            UUID organisationUuid, UUID instructorUuid, InstructorObligationStatus status, Pageable pageable);

    /** What an instructor is owed and what has been paid, per organisation. */
    InstructorStatementDTO getStatement(UUID instructorUserUuid);

    /** An instructor's own obligation rows, newest first. */
    Page<InstructorObligationDTO> findForInstructorUser(UUID instructorUserUuid, Pageable pageable);

    /**
     * Money the organisation has actually paid out to instructors, one figure per calendar
     * month over the trailing {@code months} window (inclusive of the current month), oldest
     * month first. Each figure sums settled obligations in that month.
     */
    List<MonthlyPayoutPointDTO> getMonthlySettlements(UUID organisationUuid, int months);
}
