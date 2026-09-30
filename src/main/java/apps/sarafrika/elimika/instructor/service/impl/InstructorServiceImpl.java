package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainRemovedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.instructor.spi.InstructorDTO;
import apps.sarafrika.elimika.instructor.dto.OrgInstructorSummaryDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorFactory;
import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.search.InstructorSearchReader;
import apps.sarafrika.elimika.instructor.search.InstructorVisibility;
import apps.sarafrika.elimika.instructor.service.InstructorService;
import apps.sarafrika.elimika.shared.search.NearMe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final GenericSpecificationBuilder<Instructor> specificationBuilder;
    private final DomainSecurityService domainSecurityService;
    private final InstructorSearchReader instructorSearchReader;
    private final InstructorVisibility instructorVisibility;

    private static final String INSTRUCTOR_NOT_FOUND_TEMPLATE = "Instructor with ID %s not found";
    private static final String QUERY_PARAM = "q";

    @Override
    public InstructorDTO createInstructor(InstructorDTO instructorDTO) {
        Instructor instructor = instructorRepository.findByUserUuid(instructorDTO.userUuid())
                .orElseGet(Instructor::new);
        instructor.setUserUuid(instructorDTO.userUuid());
        applyInstructorProfile(instructor, instructorDTO);
        instructor.setAdminVerified(false);

        Instructor savedInstructor = instructorRepository.save(instructor);

        applicationEventPublisher.publishEvent(
                new UserDomainMappingEvent(savedInstructor.getUserUuid(), UserDomain.instructor.name())
        );

        return InstructorFactory.toDTO(savedInstructor);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorDTO getInstructorByUuid(UUID uuid) {
        return instructorRepository.findByUuid(uuid)
                .map(this::toDtoForCaller)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDTO> getAllInstructors(String q, Pageable pageable) {
        if (StringUtils.hasText(q)) {
            Map<String, String> searchParams = new HashMap<>();
            searchParams.put(QUERY_PARAM, q);
            return search(searchParams, pageable);
        }
        specificationBuilder.validateSortProperties(Instructor.class, pageable);
        Specification<Instructor> visible = instructorVisibility.databaseScope(instructorVisibility.currentCaller(), Map.of());
        return (visible == null ? instructorRepository.findAll(pageable) : instructorRepository.findAll(visible, pageable))
                .map(this::toDirectoryDTO);
    }

    @Override
    public InstructorDTO updateInstructor(UUID uuid, InstructorDTO instructorDTO) {
        Instructor existingInstructor = instructorRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid)));

        updateInstructorFields(existingInstructor, instructorDTO);

        Instructor updatedInstructor = instructorRepository.save(existingInstructor);
        return isOwnedByCaller(updatedInstructor)
                ? InstructorFactory.toOwnerDTO(updatedInstructor)
                : InstructorFactory.toDTO(updatedInstructor);
    }

    @Override
    public void deleteInstructor(UUID uuid) {
        Instructor instructor = instructorRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid)));

        instructorRepository.delete(instructor);
        applicationEventPublisher.publishEvent(
                new UserDomainRemovedEvent(instructor.getUserUuid(), UserDomain.instructor.name())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Map<String, String> params = searchParams == null ? new HashMap<>() : new HashMap<>(searchParams);
        // q is free text, never a column: it must not reach the specification builder.
        String q = params.remove(QUERY_PARAM);
        // One visibility rule for both paths; see InstructorVisibility.
        InstructorVisibility.Caller caller = instructorVisibility.currentCaller();
        // Free text is served only by the instructors index: 503 when search cannot answer.
        if (StringUtils.hasText(q)) {
            return instructorSearchReader.search(q, params, pageable, caller).map(this::toDirectoryDTO);
        }
        specificationBuilder.validateSortProperties(Instructor.class, pageable);
        Specification<Instructor> spec = specificationBuilder.buildSpecification(Instructor.class, params);
        Specification<Instructor> visible = instructorVisibility.databaseScope(caller, params);
        if (visible != null) {
            spec = spec == null ? visible : spec.and(visible);
        }
        return (spec == null ? instructorRepository.findAll(pageable) : instructorRepository.findAll(spec, pageable))
                .map(this::toDirectoryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDTO> searchNear(String q, NearMe near, Map<String, String> searchParams, Pageable pageable) {
        Map<String, String> params = searchParams == null ? new HashMap<>() : new HashMap<>(searchParams);
        params.remove(QUERY_PARAM);
        InstructorVisibility.Caller caller = instructorVisibility.currentCaller();
        InstructorSearchReader.NearMeResult result = instructorSearchReader.searchNear(q, near, params, pageable, caller);
        return result.page().map(instructor -> InstructorFactory.toNearMeDTO(instructor,
                result.distanceBands().get(instructor.getUuid()), isOwnedByCaller(instructor)));
    }

    @Override
    public InstructorDTO setLocationSearchOptIn(UUID uuid, boolean enabled) {
        Instructor instructor = instructorRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid)));
        instructor.setLocationSearchOptIn(enabled);
        // The entity trigger re-indexes the profile, adding or dropping its rounded _geo point.
        return InstructorFactory.toOwnerDTO(instructorRepository.save(instructor));
    }

    // ================================
    // INSTRUCTOR VERIFICATION
    // ================================

    @Override
    public InstructorDTO verifyInstructor(UUID instructorUuid, String reason) {
        log.info("Verifying instructor {} for reason: {}", instructorUuid, reason);

        Instructor instructor = instructorRepository.findByUuid(instructorUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, instructorUuid)));

        domainSecurityService.enforceNotSelfApprovingProfile(
                instructor.getUserUuid(),
                "instructor"
        );

        boolean wasVerified = Boolean.TRUE.equals(instructor.getAdminVerified());
        instructor.setAdminVerified(true);
        Instructor verifiedInstructor = instructorRepository.save(instructor);
        if (!wasVerified) {
            publishVerificationNotification(verifiedInstructor, true);
        }

        log.info("Successfully verified instructor {}", instructorUuid);
        return InstructorFactory.toDTO(verifiedInstructor);
    }

    @Override
    public InstructorDTO unverifyInstructor(UUID instructorUuid, String reason) {
        log.info("Removing verification from instructor {} for reason: {}", instructorUuid, reason);

        Instructor instructor = instructorRepository.findByUuid(instructorUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, instructorUuid)));

        boolean wasVerified = Boolean.TRUE.equals(instructor.getAdminVerified());
        instructor.setAdminVerified(false);
        Instructor unverifiedInstructor = instructorRepository.save(instructor);
        if (wasVerified) {
            publishVerificationNotification(unverifiedInstructor, false);
        }

        log.info("Successfully removed verification from instructor {}", instructorUuid);
        return InstructorFactory.toDTO(unverifiedInstructor);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInstructorVerified(UUID instructorUuid) {
        Instructor instructor = instructorRepository.findByUuid(instructorUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, instructorUuid)));

        return Boolean.TRUE.equals(instructor.getAdminVerified());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDTO> getVerifiedInstructors(Pageable pageable) {
        log.debug("Getting verified instructors with pagination: {}", pageable);
        return instructorRepository.findByAdminVerified(true, pageable)
                .map(this::toDirectoryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDTO> getUnverifiedInstructors(Pageable pageable) {
        log.debug("Getting unverified instructors with pagination: {}", pageable);
        return instructorRepository.findByAdminVerified(false, pageable)
                .map(this::toDirectoryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long countInstructorsByVerificationStatus(boolean verified) {
        return instructorRepository.countByAdminVerified(verified);
    }

    // ================================
    // PRIVATE HELPER METHODS
    // ================================

    /**
     * Updates the fields of the existing instructor entity with values from the DTO.
     * Only updates non-null values from the DTO to support partial updates.
     * Note: Read-only fields like uuid, createdDate, createdBy, fullName, verified, etc. are not updated.
     */
    private void updateInstructorFields(Instructor existingInstructor, InstructorDTO instructorDTO) {
        applyInstructorProfile(existingInstructor, instructorDTO);
    }

    /**
     * Copies the editable profile fields across. Deliberately does not touch {@code userUuid}: which
     * account a profile belongs to is settled when the profile is created and is what the route
     * guard on every instructor operation is checked against, so honouring a {@code user_uuid} in an
     * update body would let an instructor move their own profile onto somebody else's account — or
     * clear it — and take the guard with it.
     */
    private void applyInstructorProfile(Instructor instructor, InstructorDTO instructorDTO) {
        if (instructorDTO.locationName() != null) {
            instructor.setLocationName(instructorDTO.locationName());
        }
        if (instructorDTO.latitude() != null) {
            instructor.setLatitude(instructorDTO.latitude());
        }
        if (instructorDTO.longitude() != null) {
            instructor.setLongitude(instructorDTO.longitude());
        }
        if (instructorDTO.website() != null) {
            instructor.setWebsite(instructorDTO.website());
        }
        if (instructorDTO.bio() != null) {
            instructor.setBio(instructorDTO.bio());
        }
        if (instructorDTO.professionalHeadline() != null) {
            instructor.setProfessionalHeadline(instructorDTO.professionalHeadline());
        }
    }

    private void publishVerificationNotification(Instructor instructor, boolean approved) {
        if (instructor.getUserUuid() == null || instructor.getUuid() == null) {
            return;
        }

        String notificationType = approved
                ? "INSTRUCTOR_VERIFICATION_APPROVED"
                : "INSTRUCTOR_VERIFICATION_REVOKED";
        String title = approved
                ? "Instructor profile approved"
                : "Instructor verification removed";
        String body = approved
                ? "Your instructor profile has been approved."
                : "Your instructor profile verification has been removed.";

        applicationEventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                instructor.getUserUuid(),
                notificationType,
                "POPUP",
                title,
                body,
                "/dashboard/instructor/profile",
                Map.of(
                        "instructor_uuid", instructor.getUuid(),
                        "profile_type", "instructor",
                        "admin_verified", approved
                ),
                "instructor-verification:" + instructor.getUuid() + ":" + notificationType
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrgInstructorSummaryDTO> getInstructorSummariesForOrganisation(UUID organisationUuid) {
        return instructorRepository.findInstructorSummariesForOrganisation(organisationUuid).stream()
                .map(row -> new OrgInstructorSummaryDTO(
                        toUuid(row[0]),
                        toUuid(row[1]),
                        toStr(row[2]),
                        toStr(row[3]),
                        toStr(row[4]),
                        toStr(row[5]),
                        toStr(row[6]),
                        toDouble(row[7]),
                        toLong(row[8]),
                        toLong(row[9])
                ))
                .toList();
    }

    private static UUID toUuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }

    private static String toStr(Object value) {
        return value == null ? null : value.toString();
    }

    private static Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd.doubleValue();
        if (value instanceof Number n) return n.doubleValue();
        return null;
    }

    private static long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }

    /**
     * A single profile at full coordinate precision for its owner or a platform admin, and at town
     * level for everyone else.
     */
    private InstructorDTO toDtoForCaller(Instructor instructor) {
        if (isOwnedByCaller(instructor)) {
            return InstructorFactory.toOwnerDTO(instructor);
        }
        return domainSecurityService.isPlatformAdmin()
                ? InstructorFactory.toDTO(instructor)
                : InstructorFactory.toPublicDTO(instructor);
    }

    /**
     * A list or search row: coordinates at town level, except on the caller's own profile. The
     * profile screens load the owner's record through search and write it back on save, so rounding
     * the owner's own row would silently coarsen their stored location.
     */
    private InstructorDTO toDirectoryDTO(Instructor instructor) {
        return isOwnedByCaller(instructor) ? InstructorFactory.toOwnerDTO(instructor) : InstructorFactory.toPublicDTO(instructor);
    }

    private boolean isOwnedByCaller(Instructor instructor) {
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        return callerUuid != null && callerUuid.equals(instructor.getUserUuid());
    }
}
