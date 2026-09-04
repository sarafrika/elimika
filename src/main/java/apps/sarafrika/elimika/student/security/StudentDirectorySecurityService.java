package apps.sarafrika.elimika.student.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.model.StudentGuardianLink;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authorization for the student directory.
 * <p>
 * A student record is two records wearing one shape. The display half — uuid, the linked user, the
 * derived full name, the learner's own bio — is directory data: class rosters, enrolment tables,
 * review cards, booking lists and the public profile page all draw learners the viewer has no
 * relationship with, so refusing those reads would blank out most of the product. The other half is
 * contact data for a minor's guardians — two names and two mobile numbers — plus the demographic
 * tag that marks the learner as a child and the audit trail naming whoever last touched the record.
 * <p>
 * Reads are therefore projected rather than refused. {@link #project(StudentDTO)} decides per
 * record whether the caller is served the whole thing or
 * {@link StudentDTO#toDirectoryProjection()}; only a party with a real relationship to the learner
 * sees the private half — the learner themselves, a guardian holding an active link, a manager of
 * an organisation the learner currently belongs to, or a platform admin. That same relationship
 * governs who may create or change a record. Deleting one is narrower still, see
 * {@link #canDeleteStudent(UUID)}.
 * <p>
 * The relationship is resolved once per request, not once per row: a page of search results costs
 * the same handful of queries a single record does, and the per-row decision is then a set lookup.
 * Every predicate fails closed.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
@Service("studentDirectorySecurityService")
@RequiredArgsConstructor
@Slf4j
public class StudentDirectorySecurityService {

    private static final String CACHE_REACH = "security.studentDirectory.reach";
    private static final String CACHE_OWNER_PREFIX = "security.studentDirectory.owner.";

    private final DomainSecurityService domainSecurityService;
    private final UserLookupService userLookupService;
    private final StudentRepository studentRepository;
    private final StudentGuardianLinkRepository guardianLinkRepository;
    private final RequestScopedCache requestScopedCache;

    /**
     * The relationships the caller holds over the student directory on this request.
     *
     * @param platformAdmin      the caller administers the platform, so nothing is hidden from them
     * @param ownStudentUuid     the caller's own student profile, or null when they have none
     * @param guardedStudents    the students the caller holds an active guardian link to
     * @param managedMemberUsers the users belonging to organisations the caller manages
     */
    private record DirectoryReach(boolean platformAdmin,
                                  UUID ownStudentUuid,
                                  Set<UUID> guardedStudents,
                                  Set<UUID> managedMemberUsers) {

        boolean reaches(UUID studentUuid, UUID ownerUserUuid) {
            if (platformAdmin) {
                return true;
            }
            if (studentUuid != null
                    && (studentUuid.equals(ownStudentUuid) || guardedStudents.contains(studentUuid))) {
                return true;
            }
            return ownerUserUuid != null && managedMemberUsers.contains(ownerUserUuid);
        }
    }

    /**
     * Serves a record at the level the caller has earned: whole when they have a relationship to the
     * learner, {@linkplain StudentDTO#toDirectoryProjection() display identity only} otherwise.
     * <p>
     * Costs no query per record — the record already carries both identifiers the decision needs —
     * so a page of results is projected in memory.
     */
    public StudentDTO project(StudentDTO student) {
        if (student == null) {
            return null;
        }
        try {
            return reach().reaches(student.uuid(), student.userUuid())
                    ? student
                    : student.toDirectoryProjection();
        } catch (Exception e) {
            log.error("Error projecting student {} for the current caller", student.uuid(), e);
            return student.toDirectoryProjection();
        }
    }

    /**
     * True when the caller may see the private half of this student's record. The write gates below
     * are built on it, and an unknown student answers false, so a record that does not exist is
     * never distinguishable from one the caller may not read in full.
     */
    public boolean canViewGuardianDetails(UUID studentUuid) {
        if (studentUuid == null) {
            return false;
        }
        try {
            return reach().reaches(studentUuid, ownerOf(studentUuid));
        } catch (Exception e) {
            log.error("Error checking guardian-detail access to student {}", studentUuid, e);
            return false;
        }
    }

    /**
     * True when the caller may change this student's record, and the change keeps the record
     * pointing at the account it was created for.
     * <p>
     * Writing the guardian contacts is the same privilege as reading them, so the relationship test
     * is {@link #canViewGuardianDetails(UUID)}. Re-pointing {@code user_uuid} is refused outright:
     * the age gate and the {@code student} domain mapping both key off that account, so moving a
     * profile between accounts would silently re-home both.
     */
    public boolean canUpdateStudent(UUID studentUuid, UUID userUuid) {
        if (!canViewGuardianDetails(studentUuid)) {
            return false;
        }
        UUID ownerUuid = ownerOf(studentUuid);
        return ownerUuid == null || userUuid == null || ownerUuid.equals(userUuid);
    }

    /**
     * True when the caller may create a student record for this user: their own account, or — for a
     * platform admin, or a manager of an organisation the user currently belongs to — somebody
     * else's. Creating a profile writes guardian contacts and grants the {@code student} domain, so
     * it takes the same relationship reading those contacts does.
     */
    public boolean canCreateStudentFor(UUID userUuid) {
        if (userUuid == null) {
            return false;
        }
        try {
            if (domainSecurityService.isPlatformAdmin()) {
                return true;
            }
            UUID callerUuid = domainSecurityService.getCurrentUserUuid();
            if (callerUuid == null) {
                return false;
            }
            return callerUuid.equals(userUuid) || domainSecurityService.managesOrganisationOf(userUuid);
        } catch (Exception e) {
            log.error("Error checking whether the current user may create a student for user {}", userUuid, e);
            return false;
        }
    }

    /**
     * True when the caller may destroy this student's record: the learner themselves, or a platform
     * admin.
     * <p>
     * Deliberately narrower than {@link #canViewGuardianDetails(UUID)}. Deletion is not an
     * organisation-scoped act — it removes the platform-wide profile and publishes
     * {@code UserDomainRemovedEvent}, stripping the {@code student} domain and with it the learner's
     * standing at every other organisation they belong to. A guardian, or a manager of one of the
     * several organisations a learner may be enrolled at, must not be able to end that learner's
     * account everywhere, so neither relationship is admitted here.
     */
    public boolean canDeleteStudent(UUID studentUuid) {
        if (studentUuid == null) {
            return false;
        }
        try {
            return domainSecurityService.isPlatformAdmin()
                    || studentUuid.equals(domainSecurityService.getCurrentStudentUuid());
        } catch (Exception e) {
            log.error("Error checking delete access to student {}", studentUuid, e);
            return false;
        }
    }

    /**
     * True when the caller may mint or revoke guardian links for this learner: the learner
     * themselves, a manager of an organisation they currently belong to, or a platform admin.
     * <p>
     * A guardian link is the relationship {@link #canViewGuardianDetails(UUID)} trusts, and creating
     * one grants the {@code parent} domain and standing access to the learner's record — so whoever
     * can create one can hand that access to anybody, including themselves. It therefore takes
     * custody of the learner, not a role: an instructor who happens to teach somewhere is not
     * entitled to appoint a stranger as a child's guardian. Existing guardians are excluded too,
     * deliberately: a guardian may hand back their own access (see
     * {@link #canRevokeGuardianLink(UUID)}) but not recruit further ones.
     */
    public boolean canManageGuardianLinksFor(UUID studentUuid) {
        if (studentUuid == null) {
            return false;
        }
        try {
            if (domainSecurityService.isPlatformAdmin()) {
                return true;
            }
            if (studentUuid.equals(domainSecurityService.getCurrentStudentUuid())) {
                return true;
            }
            UUID ownerUuid = ownerOf(studentUuid);
            return ownerUuid != null && domainSecurityService.managesOrganisationOf(ownerUuid);
        } catch (Exception e) {
            log.error("Error checking guardian-link custody of student {}", studentUuid, e);
            return false;
        }
    }

    /**
     * True when the caller may revoke this guardian link: anybody with custody of the learner (see
     * {@link #canManageGuardianLinksFor(UUID)}), plus the guardian named on the link, who is always
     * free to give up their own access. An unknown link answers false.
     */
    public boolean canRevokeGuardianLink(UUID linkUuid) {
        if (linkUuid == null) {
            return false;
        }
        try {
            StudentGuardianLink link = guardianLinkRepository.findByUuid(linkUuid).orElse(null);
            if (link == null) {
                return false;
            }
            UUID callerUuid = domainSecurityService.getCurrentUserUuid();
            if (callerUuid != null && callerUuid.equals(link.getGuardianUserUuid())) {
                return true;
            }
            return canManageGuardianLinksFor(link.getStudentUuid());
        } catch (Exception e) {
            log.error("Error checking revoke access to guardian link {}", linkUuid, e);
            return false;
        }
    }

    /**
     * Resolves the caller's reach over the directory once for the whole request.
     * <p>
     * The organisation arm deliberately resolves members rather than testing membership per record:
     * a manager listing their roster would otherwise pay a membership query for every row on the
     * page. Membership is read live in both directions — the organisations come from
     * {@link UserLookupService#getActiveUserOrganizations} and the members from a query that already
     * excludes inactive and soft-deleted mappings — so a learner an organisation has removed drops
     * out of its managers' reach immediately instead of lingering behind a soft-deleted row.
     */
    private DirectoryReach reach() {
        return requestScopedCache.get(CACHE_REACH, () -> {
            try {
                if (domainSecurityService.isPlatformAdmin()) {
                    return new DirectoryReach(true, null, Set.of(), Set.of());
                }
                UUID callerUuid = domainSecurityService.getCurrentUserUuid();
                if (callerUuid == null) {
                    return new DirectoryReach(false, null, Set.of(), Set.of());
                }
                return new DirectoryReach(
                        false,
                        domainSecurityService.getCurrentStudentUuid(),
                        guardedStudents(callerUuid),
                        managedMemberUsers(callerUuid));
            } catch (Exception e) {
                log.error("Error resolving the current caller's reach over the student directory", e);
                return new DirectoryReach(false, null, Set.of(), Set.of());
            }
        });
    }

    /**
     * The students the caller holds an active guardian link to. Skipped entirely unless the caller
     * carries the {@code parent} domain, which is the gate the guardian endpoints themselves apply.
     */
    private Set<UUID> guardedStudents(UUID callerUuid) {
        if (!domainSecurityService.isGuardian()) {
            return Set.of();
        }
        List<StudentGuardianLink> links =
                guardianLinkRepository.findByGuardianUserUuidAndStatus(callerUuid, GuardianLinkStatus.ACTIVE);
        return links.stream()
                .map(StudentGuardianLink::getStudentUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The users belonging to organisations the caller manages — one query per managed organisation,
     * and none at all for the overwhelming majority of callers, who manage nothing.
     */
    private Set<UUID> managedMemberUsers(UUID callerUuid) {
        Set<UUID> members = new LinkedHashSet<>();
        for (UUID organisationUuid : userLookupService.getActiveUserOrganizations(callerUuid)) {
            boolean manages = userLookupService.userBelongsToOrganizationWithDomain(
                    callerUuid, organisationUuid, UserDomain.organisation_user)
                    || userLookupService.userBelongsToOrganizationWithDomain(
                    callerUuid, organisationUuid, UserDomain.admin);
            if (manages) {
                members.addAll(userLookupService.getOrganizationMemberUserUuids(organisationUuid));
            }
        }
        return members;
    }

    /**
     * The user a student record belongs to, memoised for the request; null when the record does not
     * exist. Only the uuid-addressed paths need it — a record already in hand carries its owner.
     */
    private UUID ownerOf(UUID studentUuid) {
        return requestScopedCache.get(CACHE_OWNER_PREFIX + studentUuid, () -> {
            try {
                return studentRepository.findByUuid(studentUuid).map(Student::getUserUuid).orElse(null);
            } catch (Exception e) {
                log.error("Error resolving the owner of student {}", studentUuid, e);
                return null;
            }
        });
    }
}
