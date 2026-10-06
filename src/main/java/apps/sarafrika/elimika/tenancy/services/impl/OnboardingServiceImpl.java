package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStepProvider;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubject;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubmissionService;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;
import apps.sarafrika.elimika.tenancy.dto.OnboardingDTO;
import apps.sarafrika.elimika.tenancy.dto.OnboardingSummaryDTO;
import apps.sarafrika.elimika.tenancy.internal.onboarding.AccountOnboardingSteps;
import apps.sarafrika.elimika.tenancy.services.DomainApprovalService;
import apps.sarafrika.elimika.tenancy.services.OnboardingService;
import apps.sarafrika.elimika.tenancy.util.enums.OnboardingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OnboardingServiceImpl implements OnboardingService, OnboardingSubmissionService {

    static final Set<UserDomain> ONBOARDABLE = EnumSet.of(UserDomain.student, UserDomain.instructor,
            UserDomain.course_creator, UserDomain.organisation_user, UserDomain.parent);

    private static final Set<OnboardingStatus> SUBMITTABLE = EnumSet.of(OnboardingStatus.NOT_STARTED,
            OnboardingStatus.IN_PROGRESS, OnboardingStatus.REJECTED, OnboardingStatus.SUSPENDED);

    private final DomainApprovalService domainApprovalService;
    private final ObjectProvider<OnboardingStepProvider> stepProviders;
    private final ObjectProvider<InstructorLookupService> instructorLookup;
    private final ObjectProvider<CourseCreatorLookupService> courseCreatorLookup;
    private final ObjectProvider<LearnerProfileLookupService> learnerLookup;

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingSummaryDTO> domainsOf(UUID userUuid) {
        return domainApprovalService.applicationsFor(userUuid).stream()
                .map(application -> parse(application.domain()))
                .flatMap(Optional::stream)
                .filter(ONBOARDABLE::contains)
                .distinct()
                .map(domain -> OnboardingSummaryDTO.of(onboarding(userUuid, domain)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingDTO onboarding(UUID userUuid, UserDomain domain) {
        requireOnboardable(domain);
        OnboardingSubject subject = subject(userUuid, domain);
        return evaluate(subject, steps(subject), domainApprovalService.application(userUuid, domain));
    }

    @Override
    public OnboardingDTO submit(UUID userUuid, UserDomain domain) {
        requireOnboardable(domain);
        OnboardingSubject subject = subject(userUuid, domain);
        Optional<DomainApplicationDTO> application = domainApprovalService.application(userUuid, domain);
        if (application.isEmpty()) {
            throw new ResourceNotFoundException("Request the " + domain + " domain before submitting its onboarding");
        }
        List<OnboardingStep> steps = steps(subject);
        OnboardingDTO current = evaluate(subject, steps, application);
        if (current.requiresApproval()) {
            if (current.status() == OnboardingStatus.APPROVED) {
                throw new IllegalStateException("The " + domain + " domain is already approved");
            }
            if (current.status() == OnboardingStatus.SUBMITTED) {
                throw new IllegalStateException("The " + domain + " onboarding is already awaiting review");
            }
        }
        List<String> outstanding = steps.stream()
                .filter(step -> step.required() && !step.complete())
                .map(OnboardingStep::key)
                .toList();
        if (!outstanding.isEmpty()) {
            throw new IllegalStateException("Complete the required onboarding steps first: "
                    + String.join(", ", outstanding));
        }
        stepProviders.orderedStream()
                .filter(provider -> provider.supports(domain))
                .forEach(provider -> provider.onSubmitted(subject));
        domainApprovalService.recordSubmission(userUuid, domain);
        return evaluate(subject, steps(subject), domainApprovalService.application(userUuid, domain));
    }

    @Override
    public void submitOnboarding(UUID userUuid, UserDomain domain) {
        submit(userUuid, domain);
    }

    private OnboardingDTO evaluate(OnboardingSubject subject, List<OnboardingStep> steps,
                                   Optional<DomainApplicationDTO> application) {
        boolean requiresApproval = domainApprovalService.requiresApproval(subject.domain());
        OnboardingStatus status = status(application, steps);
        boolean requiredDone = steps.stream().filter(OnboardingStep::required).allMatch(OnboardingStep::complete);
        boolean ready = application.isPresent() && requiredDone
                && (requiresApproval ? SUBMITTABLE.contains(status) : status == OnboardingStatus.APPROVED);
        int completed = (int) steps.stream().filter(OnboardingStep::complete).count();
        return new OnboardingDTO(
                subject.domain().name(),
                status,
                application.isPresent(),
                requiresApproval,
                application.map(app -> app.status() == DomainApprovalStatus.APPROVED).orElse(false),
                steps,
                completed,
                steps.size(),
                ready,
                application.map(DomainApplicationDTO::submittedAt).orElse(null),
                application.map(DomainApplicationDTO::reviewedAt).orElse(null),
                application.map(DomainApplicationDTO::reviewReason).orElse(null));
    }

    static OnboardingStatus status(Optional<DomainApplicationDTO> application, List<OnboardingStep> steps) {
        if (application.isEmpty() || application.get().status() == null) {
            return OnboardingStatus.NOT_STARTED;
        }
        return switch (application.get().status()) {
            case APPROVED -> OnboardingStatus.APPROVED;
            case REJECTED -> OnboardingStatus.REJECTED;
            case SUSPENDED -> OnboardingStatus.SUSPENDED;
            case PENDING -> {
                if (application.get().submittedAt() != null) {
                    yield OnboardingStatus.SUBMITTED;
                }
                boolean started = steps.stream()
                        .anyMatch(step -> step.complete() && !AccountOnboardingSteps.KEY.equals(step.key()));
                yield started ? OnboardingStatus.IN_PROGRESS : OnboardingStatus.NOT_STARTED;
            }
        };
    }

    private List<OnboardingStep> steps(OnboardingSubject subject) {
        return stepProviders.orderedStream()
                .filter(provider -> provider.supports(subject.domain()))
                .flatMap(provider -> provider.steps(subject).stream())
                .sorted(Comparator.comparingInt(OnboardingStep::position))
                .toList();
    }

    private OnboardingSubject subject(UUID userUuid, UserDomain domain) {
        UUID profileUuid = switch (domain) {
            case instructor -> instructorLookup.getObject().findInstructorUuidByUserUuid(userUuid).orElse(null);
            case course_creator -> courseCreatorLookup.getObject().findCourseCreatorUuidByUserUuid(userUuid).orElse(null);
            case student -> learnerLookup.getObject().findStudentUuidByUserUuid(userUuid).orElse(null);
            default -> null;
        };
        return new OnboardingSubject(userUuid, domain, profileUuid);
    }

    private static void requireOnboardable(UserDomain domain) {
        if (domain == null || !ONBOARDABLE.contains(domain)) {
            throw new IllegalArgumentException("The " + domain + " domain has no onboarding");
        }
    }

    private static Optional<UserDomain> parse(String domain) {
        try {
            return domain == null ? Optional.empty() : Optional.of(UserDomain.valueOf(domain.toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
