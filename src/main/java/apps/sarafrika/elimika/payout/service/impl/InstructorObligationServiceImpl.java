package apps.sarafrika.elimika.payout.service.impl;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.payout.dto.InstructorObligationDTO;
import apps.sarafrika.elimika.payout.dto.InstructorStatementDTO;
import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import apps.sarafrika.elimika.payout.factory.InstructorObligationFactory;
import apps.sarafrika.elimika.payout.model.InstructorObligation;
import apps.sarafrika.elimika.payout.repository.InstructorObligationRepository;
import apps.sarafrika.elimika.payout.service.InstructorObligationService;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.payout.InstructorPayableLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Records and settles what organisations owe their instructors.
 * <p>
 * The one rule the rest of this class exists to protect: {@code rate_amount} is read once, at
 * accrual, and written onto the row. Every read afterwards sums stored rows. That is the difference
 * between a ledger and the expression this replaced, which multiplied completed sessions by
 * <em>today's</em> training fee and so quietly rewrote history whenever a class was re-rated.
 * <p>
 * Also implements {@link InstructorPayableLookupService} so the organisation payables endpoint —
 * which lives in {@code classes} and must not depend on {@code payout} — can read the aggregate
 * through {@code shared}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorObligationServiceImpl
        implements InstructorObligationService, InstructorPayableLookupService {

    /**
     * The statuses that represent delivered, uncontested work. Cancelled rows should never have
     * existed and disputed rows are contested, so neither counts as money owed nor as a session
     * delivered.
     */
    private static final Set<InstructorObligationStatus> COUNTED_STATUSES =
            Set.of(InstructorObligationStatus.ACCRUED, InstructorObligationStatus.SETTLED);

    private final InstructorObligationRepository obligationRepository;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final InstructorLookupService instructorLookupService;
    private final CurrencyService currencyService;

    @Override
    @Transactional
    public Optional<InstructorObligationDTO> accrueForCompletedSession(
            UUID classDefinitionUuid, UUID sessionUuid, UUID instructorUuid, LocalDateTime completedAt) {

        if (classDefinitionUuid == null || sessionUuid == null || instructorUuid == null) {
            log.debug("Ignoring session completion with incomplete identity: class={}, session={}, instructor={}",
                    classDefinitionUuid, sessionUuid, instructorUuid);
            return Optional.empty();
        }

        Optional<InstructorObligation> existing = obligationRepository
                .findByClassDefinitionUuidAndSessionUuidAndInstructorUuid(
                        classDefinitionUuid, sessionUuid, instructorUuid);
        if (existing.isPresent()) {
            log.debug("Obligation already accrued for session {} (instructor {})", sessionUuid, instructorUuid);
            return existing.map(InstructorObligationFactory::toDTO);
        }

        UUID organisationUuid = classDefinitionLookupService.findOrganisationUuid(classDefinitionUuid)
                .orElse(null);
        if (organisationUuid == null) {
            // An independent instructor's own class has no organisation behind it, so there is
            // nobody to owe the money. That is a normal outcome, not a failure.
            log.debug("Class {} has no owning organisation; no obligation accrues", classDefinitionUuid);
            return Optional.empty();
        }

        BigDecimal rate = classDefinitionLookupService.findByUuid(classDefinitionUuid)
                .map(ClassDefinitionSnapshot::trainingFee)
                .orElse(null);
        if (rate == null || rate.signum() <= 0) {
            log.debug("Class {} has no positive training fee; no obligation accrues", classDefinitionUuid);
            return Optional.empty();
        }

        UUID instructorUserUuid = instructorLookupService.getInstructorUserUuid(instructorUuid).orElse(null);
        if (instructorUserUuid == null) {
            // The debt is still real, and losing it because a profile lookup came back empty would
            // be the worst possible trade. The column is nullable precisely for this.
            log.warn("No user resolved for instructor {}; obligation for session {} is recorded without one",
                    instructorUuid, sessionUuid);
        }

        InstructorObligation obligation = InstructorObligationFactory.accrue(
                organisationUuid,
                instructorUuid,
                instructorUserUuid,
                classDefinitionUuid,
                sessionUuid,
                rate,
                currencyService.resolveCurrencyOrDefault(null).getCode(),
                completedAt == null ? nowUtc() : completedAt);

        try {
            InstructorObligation saved = obligationRepository.saveAndFlush(obligation);
            log.info("Accrued {} {} owed by organisation {} to instructor {} for session {}",
                    saved.getRateAmount(), saved.getCurrencyCode(), organisationUuid, instructorUuid, sessionUuid);
            return Optional.of(InstructorObligationFactory.toDTO(saved));
        } catch (DataIntegrityViolationException ex) {
            // A concurrent redelivery of the same completion raced past the pre-check and was stopped
            // by uq_instructor_obligations_session. The obligation exists either way, which is all
            // the caller asked for.
            log.debug("Obligation for session {} was accrued concurrently; keeping the existing row", sessionUuid);
            return obligationRepository
                    .findByClassDefinitionUuidAndSessionUuidAndInstructorUuid(
                            classDefinitionUuid, sessionUuid, instructorUuid)
                    .map(InstructorObligationFactory::toDTO);
        }
    }

    @Override
    @Transactional
    public InstructorObligationDTO settle(
            UUID organisationUuid, UUID obligationUuid, String settlementReference, String note, String settledBy) {

        if (!StringUtils.hasText(settlementReference)) {
            throw new IllegalArgumentException("A settlement reference is required");
        }
        if (!StringUtils.hasText(settledBy)) {
            throw new IllegalArgumentException("The settling user could not be determined");
        }

        InstructorObligation obligation = findOwnedObligation(organisationUuid, obligationUuid);
        if (obligation.getStatus() != InstructorObligationStatus.ACCRUED) {
            throw new IllegalStateException(
                    "Obligation " + obligationUuid + " is " + obligation.getStatus()
                            + " and cannot be settled; only an accrued obligation can be marked paid");
        }

        obligation.setStatus(InstructorObligationStatus.SETTLED);
        obligation.setSettledAt(nowUtc());
        obligation.setSettlementReference(settlementReference.trim());
        obligation.setSettledBy(settledBy);
        obligation.setStatusNote(StringUtils.hasText(note) ? note.trim() : null);

        InstructorObligation saved = obligationRepository.save(obligation);
        log.info("Organisation {} settled {} {} owed to instructor {} against reference {}",
                organisationUuid, saved.getRateAmount(), saved.getCurrencyCode(),
                saved.getInstructorUuid(), saved.getSettlementReference());
        return InstructorObligationFactory.toDTO(saved);
    }

    @Override
    @Transactional
    public InstructorObligationDTO cancel(
            UUID organisationUuid, UUID obligationUuid, String reason, String cancelledBy) {

        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("A cancellation reason is required");
        }

        InstructorObligation obligation = findOwnedObligation(organisationUuid, obligationUuid);
        if (obligation.getStatus() == InstructorObligationStatus.SETTLED) {
            throw new IllegalStateException(
                    "Obligation " + obligationUuid + " has already been settled and cannot be cancelled");
        }
        if (obligation.getStatus() == InstructorObligationStatus.CANCELLED) {
            throw new IllegalStateException("Obligation " + obligationUuid + " is already cancelled");
        }

        obligation.setStatus(InstructorObligationStatus.CANCELLED);
        obligation.setStatusNote(reason.trim());
        obligation.setSettledBy(cancelledBy);

        InstructorObligation saved = obligationRepository.save(obligation);
        log.info("Organisation {} cancelled obligation {} for session {}: {}",
                organisationUuid, obligationUuid, saved.getSessionUuid(), saved.getStatusNote());
        return InstructorObligationFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorObligationDTO> findForOrganisation(
            UUID organisationUuid, UUID instructorUuid, InstructorObligationStatus status, Pageable pageable) {

        Page<InstructorObligation> page;
        if (instructorUuid != null && status != null) {
            page = obligationRepository.findByOrganisationUuidAndInstructorUuidAndStatus(
                    organisationUuid, instructorUuid, status, pageable);
        } else if (instructorUuid != null) {
            page = obligationRepository.findByOrganisationUuidAndInstructorUuid(
                    organisationUuid, instructorUuid, pageable);
        } else if (status != null) {
            page = obligationRepository.findByOrganisationUuidAndStatus(organisationUuid, status, pageable);
        } else {
            page = obligationRepository.findByOrganisationUuid(organisationUuid, pageable);
        }
        return page.map(InstructorObligationFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorStatementDTO getStatement(UUID instructorUserUuid) {
        List<InstructorStatementDTO.Line> lines = obligationRepository
                .aggregateByInstructorUser(
                        instructorUserUuid,
                        COUNTED_STATUSES,
                        InstructorObligationStatus.ACCRUED,
                        InstructorObligationStatus.SETTLED)
                .stream()
                .map(InstructorObligationFactory::toStatementLine)
                .toList();
        return new InstructorStatementDTO(instructorUserUuid, lines);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorObligationDTO> findForInstructorUser(UUID instructorUserUuid, Pageable pageable) {
        return obligationRepository.findByInstructorUserUuid(instructorUserUuid, pageable)
                .map(InstructorObligationFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisationInstructorPayable> findPayablesForOrganisation(UUID organisationUuid) {
        if (organisationUuid == null) {
            return List.of();
        }
        return obligationRepository
                .aggregateByOrganisation(
                        organisationUuid,
                        COUNTED_STATUSES,
                        InstructorObligationStatus.ACCRUED,
                        InstructorObligationStatus.SETTLED)
                .stream()
                .map(InstructorObligationFactory::toPayable)
                .toList();
    }

    /**
     * Loads an obligation only if the named organisation actually owes it. A row belonging to
     * another tenant is reported as missing rather than forbidden, so the endpoint cannot be used to
     * discover that someone else's obligation exists.
     */
    private InstructorObligation findOwnedObligation(UUID organisationUuid, UUID obligationUuid) {
        InstructorObligation obligation = obligationRepository.findByUuid(obligationUuid)
                .filter(candidate -> candidate.getOrganisationUuid() != null
                        && candidate.getOrganisationUuid().equals(organisationUuid))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instructor obligation " + obligationUuid + " was not found for organisation "
                                + organisationUuid));
        return obligation;
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
