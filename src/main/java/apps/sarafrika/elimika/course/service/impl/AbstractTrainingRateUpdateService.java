package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateDTO;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateDecisionRequest;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateRequest;
import apps.sarafrika.elimika.course.factory.TrainingRateCardFactory;
import apps.sarafrika.elimika.course.factory.TrainingRateUpdateFactory;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicantNames;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationAccess;
import apps.sarafrika.elimika.course.internal.training.TrainingRateUpdateNotifier;
import apps.sarafrika.elimika.course.model.TrainingApplicationRecord;
import apps.sarafrika.elimika.course.model.TrainingRateUpdate;
import apps.sarafrika.elimika.course.repository.TrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.service.TrainingRateUpdateService;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The rate update workflow for course and program applications; subclasses find the application, owner and floor. */
@Transactional
public abstract class AbstractTrainingRateUpdateService<A extends TrainingApplicationRecord, U extends TrainingRateUpdate>
        implements TrainingRateUpdateService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final TrainingRateUpdateRepository<U> updateRepository;
    private final CourseTrainingRateCardValidator rateCardValidator;
    private final CurrencyService currencyService;
    private final TrainingApplicationAccess access;
    private final TrainingApplicantNames applicantNames;
    private final TrainingRateUpdateNotifier notifier;

    protected AbstractTrainingRateUpdateService(TrainingRateUpdateRepository<U> updateRepository,
                                                CourseTrainingRateCardValidator rateCardValidator,
                                                CurrencyService currencyService,
                                                TrainingApplicationAccess access,
                                                TrainingApplicantNames applicantNames,
                                                TrainingRateUpdateNotifier notifier) {
        this.updateRepository = updateRepository;
        this.rateCardValidator = rateCardValidator;
        this.currencyService = currencyService;
        this.access = access;
        this.applicantNames = applicantNames;
        this.notifier = notifier;
    }

    protected abstract TrainingApplicationType applicationType();

    /** The application, or a 404 when it does not exist under this parent. */
    protected abstract A findApplication(UUID parentUuid, UUID applicationUuid);

    protected abstract List<A> findApplications(Collection<UUID> applicationUuids);

    protected abstract void saveApplication(A application);

    protected abstract boolean ownsParent(UUID parentUuid);

    protected abstract String ownerLabel();

    protected abstract BigDecimal minimumTrainingFee(UUID parentUuid);

    protected abstract TrainingRateUpdateNotifier.Subject subject(UUID parentUuid);

    protected abstract Page<U> findUpdatesForParent(UUID parentUuid, TrainingRateUpdateStatus status, Pageable pageable);

    protected abstract U newUpdate();

    protected abstract ResourceNotFoundException applicationNotFound(UUID parentUuid, UUID applicationUuid);

    @Override
    public TrainingRateUpdateDTO submitRateUpdate(UUID parentUuid, UUID applicationUuid, TrainingRateUpdateRequest request) {
        A application = findApplication(parentUuid, applicationUuid);
        ensureApplicant(application, "Only the applicant can propose new rates on this training application.");
        if (application.getStatus() != CourseTrainingApplicationStatus.APPROVED) {
            throw new IllegalStateException("Rates can only be updated on an approved training application.");
        }
        if (updateRepository.existsByApplicationUuidAndStatus(application.getUuid(), TrainingRateUpdateStatus.PENDING)) {
            throw new DuplicateResourceException(
                    "A rate update is already awaiting review on this training application; withdraw it before proposing another.");
        }

        CourseTrainingRateCardDTO rateCard = request == null ? null : request.rateCard();
        rateCardValidator.validateAgainstMinimum(rateCard, minimumTrainingFee(parentUuid));

        U update = newUpdate();
        update.setApplicationUuid(application.getUuid());
        TrainingRateCardFactory.apply(update, rateCard, currencyService.resolveCurrencyOrDefault(rateCard.currency()).getCode());
        update.setNote(request.note());
        update.setStatus(TrainingRateUpdateStatus.PENDING);
        U saved = savePending(update);

        String applicantName = applicantNames.resolve(application);
        notifier.submitted(subject(parentUuid), application, saved, applicantName);
        return TrainingRateUpdateFactory.toDTO(saved, application, applicationType(), parentUuid, applicantName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRateUpdateDTO> getRateUpdates(UUID parentUuid, UUID applicationUuid) {
        A application = findApplication(parentUuid, applicationUuid);
        if (!isApplicant(application) && !ownsParent(parentUuid)) {
            throw applicationNotFound(parentUuid, applicationUuid);
        }
        String applicantName = applicantNames.resolve(application);
        return updateRepository.findByApplicationUuidOrderByCreatedDateDesc(application.getUuid()).stream()
                .map(update -> TrainingRateUpdateFactory.toDTO(update, application, applicationType(), parentUuid, applicantName))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingRateUpdateDTO> getRateUpdatesForReview(UUID parentUuid,
                                                               Optional<TrainingRateUpdateStatus> status,
                                                               Pageable pageable) {
        ensureOwner(parentUuid);
        Page<U> updates = findUpdatesForParent(parentUuid, status.orElse(null), pageable);
        Map<UUID, A> applications = findApplications(updates.map(TrainingRateUpdate::getApplicationUuid).toList()).stream()
                .collect(Collectors.toMap(TrainingApplicationRecord::getUuid, Function.identity()));
        Map<UUID, String> names = applicantNames.resolve(applications.values());
        return updates.map(update -> {
            A application = applications.get(update.getApplicationUuid());
            return TrainingRateUpdateFactory.toDTO(update, application, applicationType(), parentUuid,
                    names.get(application.getApplicantUuid()));
        });
    }

    @Override
    public TrainingRateUpdateDTO approveRateUpdate(UUID parentUuid, UUID applicationUuid, UUID updateUuid,
                                                   TrainingRateUpdateDecisionRequest request) {
        ensureOwner(parentUuid);
        A application = findApplication(parentUuid, applicationUuid);
        U update = findPendingUpdate(application, updateUuid, "approved");
        if (application.getStatus() != CourseTrainingApplicationStatus.APPROVED) {
            throw new IllegalStateException("Rates can only change on an approved training application.");
        }

        // Re-checked at approval: the minimum fee may have risen since the update was proposed.
        CourseTrainingRateCardDTO proposed = TrainingRateCardFactory.toDTO(update);
        rateCardValidator.validateAgainstMinimum(proposed, minimumTrainingFee(parentUuid));
        TrainingRateCardFactory.apply(application, proposed, update.getRateCurrency());
        saveApplication(application);

        return decide(parentUuid, application, update, TrainingRateUpdateStatus.APPROVED, request);
    }

    @Override
    public TrainingRateUpdateDTO rejectRateUpdate(UUID parentUuid, UUID applicationUuid, UUID updateUuid,
                                                  TrainingRateUpdateDecisionRequest request) {
        ensureOwner(parentUuid);
        A application = findApplication(parentUuid, applicationUuid);
        U update = findPendingUpdate(application, updateUuid, "rejected");
        return decide(parentUuid, application, update, TrainingRateUpdateStatus.REJECTED, request);
    }

    @Override
    public void withdrawRateUpdate(UUID parentUuid, UUID applicationUuid, UUID updateUuid) {
        A application = findApplication(parentUuid, applicationUuid);
        ensureApplicant(application, "Only the applicant can withdraw this rate update.");
        U update = findPendingUpdate(application, updateUuid, "withdrawn");
        update.setStatus(TrainingRateUpdateStatus.WITHDRAWN);
        updateRepository.save(update);
    }

    @Override
    public void closePendingRateUpdate(UUID applicationUuid, String reason) {
        updateRepository.findFirstByApplicationUuidAndStatus(applicationUuid, TrainingRateUpdateStatus.PENDING)
                .ifPresent(update -> {
                    close(update, TrainingRateUpdateStatus.REJECTED, reason);
                    updateRepository.save(update);
                });
    }

    private TrainingRateUpdateDTO decide(UUID parentUuid, A application, U update, TrainingRateUpdateStatus outcome,
                                         TrainingRateUpdateDecisionRequest request) {
        close(update, outcome, request == null ? null : request.reviewNotes());
        U saved = updateRepository.save(update);
        notifier.decided(subject(parentUuid), application, saved);
        return TrainingRateUpdateFactory.toDTO(saved, application, applicationType(), parentUuid,
                applicantNames.resolve(application));
    }

    private void close(U update, TrainingRateUpdateStatus outcome, String reviewNotes) {
        update.setStatus(outcome);
        update.setReviewNotes(reviewNotes);
        update.setReviewedBy(currentReviewer());
        update.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
    }

    private U findPendingUpdate(A application, UUID updateUuid, String action) {
        U update = updateRepository.findByUuid(updateUuid)
                .filter(candidate -> application.getUuid().equals(candidate.getApplicationUuid()))
                .orElseThrow(() -> new ResourceNotFoundException(String.format(
                        "Rate update %s not found for training application %s", updateUuid, application.getUuid())));
        if (update.getStatus() != TrainingRateUpdateStatus.PENDING) {
            throw new IllegalStateException("Only pending rate updates can be " + action + ".");
        }
        return update;
    }

    private U savePending(U update) {
        try {
            return updateRepository.saveAndFlush(update);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("one_pending")) {
                throw new DuplicateResourceException(
                        "A rate update is already awaiting review on this training application; withdraw it before proposing another.");
            }
            throw ex;
        }
    }

    private boolean isApplicant(A application) {
        return access.isApplicant(application.getApplicantType(), application.getApplicantUuid());
    }

    private void ensureApplicant(A application, String message) {
        if (!isApplicant(application)) {
            throw new AccessDeniedException(message);
        }
    }

    private void ensureOwner(UUID parentUuid) {
        if (!ownsParent(parentUuid)) {
            throw new AccessDeniedException("Only the " + ownerLabel() + " can review rate updates.");
        }
    }

    private static String currentReviewer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_USER;
        }
        return authentication.getName();
    }
}
