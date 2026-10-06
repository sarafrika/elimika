package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.event.user.DomainApprovalRequestedEvent;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.config.RegistrationProperties;
import apps.sarafrika.elimika.tenancy.dto.AccountStatusDTO;
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
        return toDTO(mappingRepository.save(mapping), domain.name());
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
    public List<DomainApplicationDTO> applicationsWithStatus(DomainApprovalStatus status, UserDomain domain) {
        Map<UUID, String> names = domainNames();
        UUID domainUuid = domain == null ? null : domainUuid(domain);
        return mappingRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .filter(mapping -> domainUuid == null || domainUuid.equals(mapping.getUserDomainUuid()))
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
