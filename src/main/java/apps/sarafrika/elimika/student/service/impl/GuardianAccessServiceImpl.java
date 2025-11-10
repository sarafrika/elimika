package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgramProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.dto.*;
import apps.sarafrika.elimika.student.factory.StudentGuardianLinkFactory;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.model.StudentGuardianLink;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.service.GuardianAccessService;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardianAccessServiceImpl implements GuardianAccessService {

    private static final String GUARDIAN_LINK_NOT_FOUND = "Guardian link with UUID %s not found";
    private static final EnumSet<GuardianLinkStatus> ACTIVE_OR_PENDING =
            EnumSet.of(GuardianLinkStatus.ACTIVE, GuardianLinkStatus.PENDING);
    private static final EnumSet<GuardianLinkStatus> ACTIVE_ONLY =
            EnumSet.of(GuardianLinkStatus.ACTIVE);

    private final StudentGuardianLinkRepository guardianLinkRepository;
    private final StudentRepository studentRepository;
    private final UserLookupService userLookupService;
    private final LearnerProgressLookupService learnerProgressLookupService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public GuardianStudentLinkDTO createOrUpdateLink(GuardianStudentLinkRequest request, UUID actorUuid) {
        Student student = studentRepository.findByUuid(request.studentUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Student with UUID %s not found"
                        .formatted(request.studentUuid())));

        StudentGuardianLink link = guardianLinkRepository
                .findByStudentUuidAndGuardianUserUuidAndStatusIn(
                        student.getUuid(),
                        request.guardianUserUuid(),
                        EnumSet.of(GuardianLinkStatus.PENDING, GuardianLinkStatus.ACTIVE, GuardianLinkStatus.REVOKED)
                )
                .orElseGet(() -> StudentGuardianLink.builder()
                        .studentUuid(student.getUuid())
                        .guardianUserUuid(request.guardianUserUuid())
                        .build());

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        link.setRelationshipType(request.relationshipType());
        link.setShareScope(request.shareScope());
        link.setPrimaryGuardian(request.isPrimary());
        link.setNotes(request.notes());
        link.setInvitedBy(actorUuid);
        link.setStatus(GuardianLinkStatus.ACTIVE);
        link.setLinkedDate(now);
        link.setRevokedBy(null);
        link.setRevokedDate(null);

        StudentGuardianLink savedLink = guardianLinkRepository.save(link);

        // Ensure guardian accounts gain the parent domain once linked
        applicationEventPublisher.publishEvent(
                new UserDomainMappingEvent(request.guardianUserUuid(), UserDomain.parent.name())
        );

        String guardianDisplayName = userLookupService.getUserFullName(request.guardianUserUuid())
                .orElseGet(() -> userLookupService.getUserEmail(request.guardianUserUuid()).orElse(""));

        return StudentGuardianLinkFactory.toDTO(
                savedLink,
                student.getFullName(),
                guardianDisplayName
        );
    }

    @Override
    @Transactional
    public void revokeLink(UUID linkUuid, UUID actorUuid, String reason) {
        StudentGuardianLink link = guardianLinkRepository.findByUuid(linkUuid)
                .orElseThrow(() -> new ResourceNotFoundException(GUARDIAN_LINK_NOT_FOUND.formatted(linkUuid)));

        if (GuardianLinkStatus.REVOKED.equals(link.getStatus())) {
            log.debug("Guardian link {} already revoked", linkUuid);
            return;
        }

        link.setStatus(GuardianLinkStatus.REVOKED);
        link.setRevokedBy(actorUuid);
        link.setRevokedDate(LocalDateTime.now(ZoneOffset.UTC));
        if (reason != null && !reason.isBlank()) {
            link.setNotes(reason);
        }

        guardianLinkRepository.save(link);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuardianStudentSummaryDTO> getGuardianStudentSummaries(UUID guardianUserUuid) {
        List<StudentGuardianLink> links = guardianLinkRepository
                .findByGuardianUserUuidAndStatusIn(guardianUserUuid, ACTIVE_OR_PENDING);

        if (links.isEmpty()) {
            return List.of();
        }

        Map<UUID, Student> studentMap = studentRepository
                .findByUuidIn(links.stream().map(StudentGuardianLink::getStudentUuid).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Student::getUuid, s -> s));

        return links.stream()
                .map(link -> {
                    Student student = studentMap.get(link.getStudentUuid());
                    if (student == null) {
                        return null;
                    }
                    return StudentGuardianLinkFactory.toSummary(link, student.getFullName());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianStudentDashboardDTO getGuardianDashboard(UUID guardianUserUuid, UUID studentUuid) {
        StudentGuardianLink link = guardianLinkRepository
                .findByStudentUuidAndGuardianUserUuidAndStatusIn(studentUuid, guardianUserUuid, ACTIVE_ONLY)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian does not have access to this learner."));

        Student student = studentRepository.findByUuid(studentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Student with UUID %s not found"
                        .formatted(studentUuid)));

        List<LearnerCourseProgressView> courseProgress = List.of();
        List<LearnerProgramProgressView> programProgress = List.of();

        if (!GuardianShareScope.ATTENDANCE.equals(link.getShareScope())) {
            courseProgress = learnerProgressLookupService.findRecentCourseProgress(studentUuid, 10);
            programProgress = learnerProgressLookupService.findRecentProgramProgress(studentUuid, 5);
        }

        return new GuardianStudentDashboardDTO(
                student.getUuid(),
                student.getFullName(),
                link.getShareScope(),
                link.getStatus(),
                courseProgress,
                programProgress
        );
    }
}
