package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.event.user.DomainApprovalRequestedEvent;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.config.RegistrationProperties;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.AccountStatusDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminDomainApplicationDTO;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.internal.DomainApprovalNotifier;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.services.DomainApprovalService;
import apps.sarafrika.elimika.tenancy.util.enums.AccountState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DomainApprovalServiceImpl implements DomainApprovalService {

    private final UserDomainMappingRepository mappingRepository;
    private final UserDomainRepository domainRepository;
    private final UserOrganisationDomainMappingRepository organisationMappingRepository;
    private final RegistrationProperties registrationProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final DomainApprovalNotifier notifier;
    private final UserRepository userRepository;

    @Override
    public DomainApplicationDTO request(UUID userUuid, UserDomain domain) {
        UUID domainUuid = domainUuid(domain);
        return existing(userUuid, domainUuid)
                .map(mapping -> toDTO(mapping, domain.name()))
                .orElseGet(() -> {
                    DomainApprovalStatus status = registrationProperties.requiresApproval(domain)
                            ? DomainApprovalStatus.PENDING
                            : DomainApprovalStatus.APPROVED;
                    UserDomainMapping saved = mappingRepository.save(UserDomainMapping.of(userUuid, domainUuid, status));
                    if (status == DomainApprovalStatus.PENDING) {
                        log.info("User {} requested domain {}; awaiting approval", userUuid, domain);
                        eventPublisher.publishEvent(new DomainApprovalRequestedEvent(userUuid, domain.name()));
                        notifier.requested(userUuid, domain.name());
                    }
                    return toDTO(saved, domain.name());
                });
    }

    @Override
    public DomainApplicationDTO grant(UUID userUuid, UserDomain domain) {
        UUID domainUuid = domainUuid(domain);
        UserDomainMapping mapping = existing(userUuid, domainUuid)
                .orElseGet(() -> UserDomainMapping.of(userUuid, domainUuid, DomainApprovalStatus.APPROVED));
        if (mapping.getStatus() == DomainApprovalStatus.PENDING) {
            mapping.setStatus(DomainApprovalStatus.APPROVED);
            mapping.setReviewedAt(now());
        }
        return toDTO(mappingRepository.save(mapping), domain.name());
    }

    @Override
    public DomainApplicationDTO decide(UUID userUuid, UserDomain domain, DomainApprovalStatus status,
                                       String reason, UUID reviewedBy) {
        if (status == null || status == DomainApprovalStatus.PENDING) {
            throw new IllegalArgumentException("A decision must approve, reject or suspend the domain");
        }
        UUID domainUuid = domainUuid(domain);
        UserDomainMapping mapping = existing(userUuid, domainUuid)
                .orElseGet(() -> UserDomainMapping.of(userUuid, domainUuid, DomainApprovalStatus.PENDING));
        mapping.setStatus(status);
        mapping.setReviewedAt(now());
        mapping.setReviewedBy(reviewedBy);
        mapping.setReviewReason(reason);
        log.info("Domain {} for user {} is now {}", domain, userUuid, status);
        UserDomainMapping saved = mappingRepository.save(mapping);
        notifier.decided(userUuid, domain.name(), status, reason);
        return toDTO(saved, domain.name());
    }

    @Override
    public DomainApplicationDTO moderate(UUID userUuid, UserDomain domain, String action, String reason,
                                         UUID reviewedBy) {
        if (domain == UserDomain.course_creator || domain == UserDomain.organisation_user) {
            throw new IllegalStateException("The " + domain + " domain is approved through its profile review");
        }
        if (userUuid.equals(reviewedBy)) {
            throw new IllegalStateException("Administrators cannot approve their own account");
        }
        if (existing(userUuid, domainUuid(domain)).isEmpty()) {
            throw new ResourceNotFoundException("User " + userUuid + " has not requested the " + domain + " domain");
        }
        DomainApprovalStatus status = switch (action == null ? "" : action.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "approve" -> DomainApprovalStatus.APPROVED;
            case "reject" -> DomainApprovalStatus.REJECTED;
            case "revoke" -> DomainApprovalStatus.SUSPENDED;
            default -> throw new IllegalArgumentException("Unsupported moderation action: " + action);
        };
        return decide(userUuid, domain, status, reason, reviewedBy);
    }

    @Override
    public void approveReviewedProfile(UUID userUuid, UserDomain domain, UUID reviewedBy) {
        boolean alreadyApproved = existing(userUuid, domainUuid(domain))
                .map(UserDomainMapping::isApproved)
                .orElse(false);
        if (!alreadyApproved) {
            decide(userUuid, domain, DomainApprovalStatus.APPROVED, null, reviewedBy);
        }
    }

    @Override
    public void approveOrganisationAdmins(UUID organisationUuid, UUID reviewedBy) {
        UUID adminDomain = domainUuid(UserDomain.admin);
        organisationMappingRepository.findActiveByOrganisationAndDomain(organisationUuid, adminDomain)
                .forEach(member -> approveReviewedProfile(member.getUserUuid(), UserDomain.organisation_user, reviewedBy));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDomainApplicationDTO> queue(DomainApprovalStatus status, UserDomain domain) {
        Map<UUID, String> names = domainNames();
        UUID domainUuid = domain == null ? null : domainUuid(domain);
        List<UserDomainMapping> mappings = mappingRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .filter(mapping -> domainUuid == null || domainUuid.equals(mapping.getUserDomainUuid()))
                .toList();
        Map<UUID, User> users = userRepository.findByUuidIn(mappings.stream().map(UserDomainMapping::getUserUuid)
                        .distinct().toList()).stream()
                .collect(Collectors.toMap(User::getUuid, user -> user, (first, second) -> first));
        return mappings.stream()
                .map(mapping -> {
                    User user = users.get(mapping.getUserUuid());
                    return new AdminDomainApplicationDTO(mapping.getUserUuid(),
                            user == null ? null : (nullToEmpty(user.getFirstName()) + " " + nullToEmpty(user.getLastName())).trim(),
                            user == null ? null : user.getEmail(),
                            names.get(mapping.getUserDomainUuid()), mapping.getStatus(), mapping.getCreatedAt(),
                            mapping.getReviewedAt(), mapping.getReviewReason());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countWithStatus(DomainApprovalStatus status) {
        return mappingRepository.countByStatus(status);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainApplicationDTO> applicationsFor(UUID userUuid) {
        Map<UUID, String> names = domainNames();
        return mappingRepository.findByUserUuid(userUuid).stream()
                .sorted(Comparator.comparing(UserDomainMapping::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(mapping -> toDTO(mapping, names.get(mapping.getUserDomainUuid())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountStatusDTO accountStatus(UUID userUuid) {
        List<DomainApplicationDTO> applications = applicationsFor(userUuid);
        List<String> approved = applications.stream()
                .filter(application -> application.status() == DomainApprovalStatus.APPROVED)
                .map(DomainApplicationDTO::domain)
                .filter(Objects::nonNull)
                .toList();
        // An organisation membership is vouched for by the organisation, so it counts as access
        // even when the user holds no approved global domain of their own.
        boolean organisationMember = !organisationMappingRepository.findActiveByUser(userUuid).isEmpty();
        return new AccountStatusDTO(userUuid, accountState(applications, !approved.isEmpty() || organisationMember),
                approved, applications);
    }

    private static AccountState accountState(List<DomainApplicationDTO> applications, boolean hasAccess) {
        if (hasAccess) {
            return AccountState.ACTIVE;
        }
        if (has(applications, DomainApprovalStatus.PENDING)) {
            return AccountState.PENDING_APPROVAL;
        }
        if (has(applications, DomainApprovalStatus.SUSPENDED)) {
            return AccountState.SUSPENDED;
        }
        if (has(applications, DomainApprovalStatus.REJECTED)) {
            return AccountState.REJECTED;
        }
        return AccountState.NO_DOMAIN;
    }

    private static boolean has(List<DomainApplicationDTO> applications, DomainApprovalStatus status) {
        return applications.stream().anyMatch(application -> application.status() == status);
    }

    private java.util.Optional<UserDomainMapping> existing(UUID userUuid, UUID domainUuid) {
        return mappingRepository.findByUserUuidAndUserDomainUuid(userUuid, domainUuid).stream().findFirst();
    }

    private UUID domainUuid(UserDomain domain) {
        if (domain == null) {
            throw new IllegalArgumentException("Domain is required");
        }
        return domainRepository.findByDomainName(domain.name())
                .orElseThrow(() -> new IllegalStateException("Domain " + domain + " is not configured"))
                .getUuid();
    }

    private Map<UUID, String> domainNames() {
        return domainRepository.findAll().stream()
                .collect(Collectors.toMap(apps.sarafrika.elimika.tenancy.entity.UserDomain::getUuid,
                        apps.sarafrika.elimika.tenancy.entity.UserDomain::getDomainName,
                        (first, second) -> first));
    }

    private static DomainApplicationDTO toDTO(UserDomainMapping mapping, String domainName) {
        return new DomainApplicationDTO(
                mapping.getUserUuid(),
                domainName,
                mapping.getStatus(),
                mapping.getCreatedAt(),
                mapping.getReviewedAt(),
                mapping.getReviewReason()
        );
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
