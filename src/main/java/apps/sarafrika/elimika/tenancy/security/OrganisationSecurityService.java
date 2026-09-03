package apps.sarafrika.elimika.tenancy.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.Competition;
import apps.sarafrika.elimika.tenancy.entity.SkillsFundSource;
import apps.sarafrika.elimika.tenancy.entity.SkillsFundTransaction;
import apps.sarafrika.elimika.tenancy.entity.StudentGroup;
import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import apps.sarafrika.elimika.tenancy.repository.CompetitionRepository;
import apps.sarafrika.elimika.tenancy.repository.SkillsFundSourceRepository;
import apps.sarafrika.elimika.tenancy.repository.SkillsFundTransactionRepository;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Tenant-scoped authorization for organisation-owned resources.
 * <p>
 * Every organisation endpoint needs the same two questions answered — may this caller
 * <em>read</em> this organisation's data, and may they <em>change</em> it — and the answer must be
 * scoped to the organisation named in the request, never to a role the caller happens to hold
 * somewhere else. Holding {@code admin} in organisation A says nothing about organisation B; that
 * distinction is the whole point of this class.
 * <p>
 * Lives in {@code tenancy} rather than {@code shared} because resolving a student group or a
 * training branch back to its owning organisation reads tenancy's own repositories. The module
 * already depends on {@code shared}, so the direction of the dependency is unchanged.
 * <p>
 * Every method fails closed: a null identifier, a missing entity or a lookup failure denies.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-31
 */
@Service("organisationSecurityService")
@RequiredArgsConstructor
@Slf4j
public class OrganisationSecurityService {

    private static final String CACHE_GROUP_ORG_PREFIX = "security.groupOrg.";
    private static final String CACHE_BRANCH_ORG_PREFIX = "security.branchOrg.";
    private static final String CACHE_FUND_SOURCE_ORG_PREFIX = "security.fundSourceOrg.";
    private static final String CACHE_FUND_TRANSACTION_ORG_PREFIX = "security.fundTransactionOrg.";
    private static final String CACHE_COMPETITION_ORG_PREFIX = "security.competitionOrg.";

    private final StudentGroupRepository studentGroupRepository;
    private final TrainingBranchRepository trainingBranchRepository;
    private final SkillsFundSourceRepository skillsFundSourceRepository;
    private final SkillsFundTransactionRepository skillsFundTransactionRepository;
    private final CompetitionRepository competitionRepository;
    private final DomainSecurityService domainSecurityService;
    private final RequestScopedCache requestScopedCache;

    /**
     * True when the caller may act on behalf of this organisation.
     * <p>
     * Mirrors the manager predicate the invitation endpoints already enforce: platform admins are
     * allowed through for support, everyone else needs an org-scoped {@code organisation_user} or
     * {@code admin} mapping <em>for this organisation specifically</em>.
     */
    public boolean canManageOrganisation(UUID organisationUuid) {
        if (organisationUuid == null) {
            return false;
        }
        try {
            if (domainSecurityService.isPlatformAdmin()) {
                return true;
            }

            UUID callerUuid = domainSecurityService.getCurrentUserUuid();
            if (callerUuid == null) {
                log.debug("No authenticated user; refusing management of organisation {}", organisationUuid);
                return false;
            }

            // Memoised per organisation and domain: every group-, branch-, fund- and
            // competition-scoped guard funnels through here, so a single request asks this
            // repeatedly about the same organisation.
            return domainSecurityService.belongsToOrganisationWithDomain(
                            organisationUuid, UserDomain.organisation_user)
                    || domainSecurityService.belongsToOrganisationWithDomain(
                            organisationUuid, UserDomain.admin);
        } catch (Exception e) {
            log.error("Error checking management rights for organisation {}", organisationUuid, e);
            return false;
        }
    }

    /**
     * True when the caller may read this organisation's data: any active member of it, plus
     * platform admins.
     */
    public boolean canReadOrganisation(UUID organisationUuid) {
        if (organisationUuid == null) {
            return false;
        }
        try {
            if (domainSecurityService.isPlatformAdmin()) {
                return true;
            }

            UUID callerUuid = domainSecurityService.getCurrentUserUuid();
            if (callerUuid == null) {
                log.debug("No authenticated user; refusing read of organisation {}", organisationUuid);
                return false;
            }

            return domainSecurityService.belongsToOrganisation(organisationUuid);
        } catch (Exception e) {
            log.error("Error checking read rights for organisation {}", organisationUuid, e);
            return false;
        }
    }

    /**
     * Management rights over the organisation that owns this student group.
     */
    public boolean canManageGroup(UUID groupUuid) {
        return canManageOrganisation(organisationOfGroup(groupUuid));
    }

    /**
     * Read rights over the organisation that owns this student group.
     */
    public boolean canReadGroup(UUID groupUuid) {
        return canReadOrganisation(organisationOfGroup(groupUuid));
    }

    /**
     * Management rights over the organisation that owns this training branch.
     */
    public boolean canManageBranch(UUID branchUuid) {
        return canManageOrganisation(organisationOfBranch(branchUuid));
    }

    /**
     * Read rights over the organisation that owns this training branch.
     */
    public boolean canReadBranch(UUID branchUuid) {
        return canReadOrganisation(organisationOfBranch(branchUuid));
    }

    /**
     * Management rights over the organisation whose skills fund this source belongs to.
     * <p>
     * The delete route is keyed only by the source, so the owning organisation has to be resolved
     * before the caller's authority over it means anything. Reading a source is always done through
     * the organisation-scoped listing, so only the manage predicate exists.
     */
    public boolean canManageFundSource(UUID sourceUuid) {
        return canManageOrganisation(organisationOfFundSource(sourceUuid));
    }

    /**
     * Management rights over the organisation whose skills fund this transaction belongs to.
     * Money moves here, so an unresolvable transaction denies rather than falls back to a role check.
     */
    public boolean canManageFundTransaction(UUID transactionUuid) {
        return canManageOrganisation(organisationOfFundTransaction(transactionUuid));
    }

    /**
     * Management rights over the organisation that runs this competition.
     */
    public boolean canManageCompetition(UUID competitionUuid) {
        return canManageOrganisation(organisationOfCompetition(competitionUuid));
    }

    /**
     * Read rights over the organisation that runs this competition.
     */
    public boolean canReadCompetition(UUID competitionUuid) {
        return canReadOrganisation(organisationOfCompetition(competitionUuid));
    }

    /**
     * The organisation owning a student group, memoised for the request.
     * <p>
     * The group-scoped routes carry no organisation in the path, so the guard has to look it up.
     * A group never changes owner mid-request, and a single request can ask more than once, so the
     * answer is cached rather than re-queried. Outside a request context this simply computes.
     */
    private UUID organisationOfGroup(UUID groupUuid) {
        if (groupUuid == null) {
            return null;
        }
        return requestScopedCache.get(CACHE_GROUP_ORG_PREFIX + groupUuid, () -> {
            try {
                StudentGroup group = studentGroupRepository.findByUuid(groupUuid).orElse(null);
                if (group == null) {
                    log.debug("Student group not found: {}", groupUuid);
                    return null;
                }
                return group.getOrganisationUuid();
            } catch (Exception e) {
                log.error("Error resolving the organisation owning student group {}", groupUuid, e);
                return null;
            }
        });
    }

    /**
     * The organisation owning a training branch, memoised for the request. See
     * {@link #organisationOfGroup(UUID)} for why this is cached.
     */
    private UUID organisationOfBranch(UUID branchUuid) {
        if (branchUuid == null) {
            return null;
        }
        return requestScopedCache.get(CACHE_BRANCH_ORG_PREFIX + branchUuid, () -> {
            try {
                TrainingBranch branch = trainingBranchRepository.findByUuidAndDeletedFalse(branchUuid).orElse(null);
                if (branch == null) {
                    log.debug("Training branch not found: {}", branchUuid);
                    return null;
                }
                return branch.getOrganisationUuid();
            } catch (Exception e) {
                log.error("Error resolving the organisation owning training branch {}", branchUuid, e);
                return null;
            }
        });
    }

    /**
     * The organisation whose skills fund owns this source, memoised for the request. See
     * {@link #organisationOfGroup(UUID)} for why this is cached.
     */
    private UUID organisationOfFundSource(UUID sourceUuid) {
        if (sourceUuid == null) {
            return null;
        }
        return requestScopedCache.get(CACHE_FUND_SOURCE_ORG_PREFIX + sourceUuid, () -> {
            try {
                SkillsFundSource source = skillsFundSourceRepository.findByUuid(sourceUuid).orElse(null);
                if (source == null) {
                    log.debug("Skills fund source not found: {}", sourceUuid);
                    return null;
                }
                return source.getOrganisationUuid();
            } catch (Exception e) {
                log.error("Error resolving the organisation owning skills fund source {}", sourceUuid, e);
                return null;
            }
        });
    }

    /**
     * The organisation whose skills fund owns this transaction, memoised for the request. See
     * {@link #organisationOfGroup(UUID)} for why this is cached.
     */
    private UUID organisationOfFundTransaction(UUID transactionUuid) {
        if (transactionUuid == null) {
            return null;
        }
        return requestScopedCache.get(CACHE_FUND_TRANSACTION_ORG_PREFIX + transactionUuid, () -> {
            try {
                SkillsFundTransaction transaction =
                        skillsFundTransactionRepository.findByUuid(transactionUuid).orElse(null);
                if (transaction == null) {
                    log.debug("Skills fund transaction not found: {}", transactionUuid);
                    return null;
                }
                return transaction.getOrganisationUuid();
            } catch (Exception e) {
                log.error("Error resolving the organisation owning skills fund transaction {}", transactionUuid, e);
                return null;
            }
        });
    }

    /**
     * The organisation running this competition, memoised for the request. See
     * {@link #organisationOfGroup(UUID)} for why this is cached.
     */
    private UUID organisationOfCompetition(UUID competitionUuid) {
        if (competitionUuid == null) {
            return null;
        }
        return requestScopedCache.get(CACHE_COMPETITION_ORG_PREFIX + competitionUuid, () -> {
            try {
                Competition competition = competitionRepository.findByUuid(competitionUuid).orElse(null);
                if (competition == null) {
                    log.debug("Competition not found: {}", competitionUuid);
                    return null;
                }
                return competition.getOrganisationUuid();
            } catch (Exception e) {
                log.error("Error resolving the organisation running competition {}", competitionUuid, e);
                return null;
            }
        });
    }
}
