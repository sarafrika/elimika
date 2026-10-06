package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDTO;
import apps.sarafrika.elimika.coursecreator.factory.CourseCreatorFactory;
import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorOnboardingService;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorService;
import apps.sarafrika.elimika.coursecreator.util.enums.CourseCreatorVerificationStatus;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainRemovedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseCreatorServiceImpl implements CourseCreatorService {

    private final CourseCreatorRepository courseCreatorRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GenericSpecificationBuilder<CourseCreator> specificationBuilder;
    private final DomainSecurityService domainSecurityService;
    private final CourseCreatorOnboardingService courseCreatorOnboardingService;

    private static final String COURSE_CREATOR_NOT_FOUND_TEMPLATE = "Course creator with ID %s not found";

    @Override
    public CourseCreatorDTO createCourseCreator(CourseCreatorDTO courseCreatorDTO) {
        CourseCreator courseCreator = courseCreatorRepository.findByUserUuid(courseCreatorDTO.userUuid())
                .orElseGet(CourseCreator::new);
        applyCourseCreatorProfile(courseCreator, courseCreatorDTO);
        courseCreator.setAdminVerified(false);
        applyOnboardingDefaults(courseCreator);

        CourseCreator savedCourseCreator = courseCreatorRepository.save(courseCreator);

        applicationEventPublisher.publishEvent(
                new UserDomainMappingEvent(savedCourseCreator.getUserUuid(), UserDomain.course_creator.name())
        );

        return CourseCreatorFactory.toDTO(savedCourseCreator);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorDTO getCourseCreatorByUuid(UUID uuid) {
        return courseCreatorRepository.findByUuid(uuid)
                .map(this::toDtoForCaller)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_CREATOR_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorDTO> getAllCourseCreators(Pageable pageable) {
        specificationBuilder.validateSortProperties(CourseCreator.class, pageable);
        return courseCreatorRepository.findAll(pageable).map(this::toDirectoryDTO);
    }

    @Override
    public CourseCreatorDTO updateCourseCreator(UUID uuid, CourseCreatorDTO courseCreatorDTO) {
        CourseCreator existingCourseCreator = courseCreatorRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_CREATOR_NOT_FOUND_TEMPLATE, uuid)));

        updateCourseCreatorFields(existingCourseCreator, courseCreatorDTO);

        CourseCreator updatedCourseCreator = courseCreatorRepository.save(existingCourseCreator);
        return CourseCreatorFactory.toDTO(updatedCourseCreator);
    }

    @Override
    public void deleteCourseCreator(UUID uuid) {
        CourseCreator courseCreator = courseCreatorRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_CREATOR_NOT_FOUND_TEMPLATE, uuid)));

        courseCreatorRepository.delete(courseCreator);
        applicationEventPublisher.publishEvent(
                new UserDomainRemovedEvent(courseCreator.getUserUuid(), UserDomain.course_creator.name())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorDTO> search(Map<String, String> searchParams, Pageable pageable) {
        specificationBuilder.validateSortProperties(CourseCreator.class, pageable);
        Specification<CourseCreator> spec = specificationBuilder.buildSpecification(CourseCreator.class, searchParams);
        return courseCreatorRepository.findAll(spec, pageable).map(this::toDirectoryDTO);
    }

    // ================================
    // COURSE CREATOR VERIFICATION
    // ================================

    @Override
    public CourseCreatorDTO verifyCourseCreator(UUID courseCreatorUuid, String reason) {
        // Same path as the moderation endpoint, so the review status and domain approval stay in step.
        courseCreatorOnboardingService.moderate(courseCreatorUuid, "approve", reason);
        return getCourseCreatorByUuid(courseCreatorUuid);
    }

    @Override
    public CourseCreatorDTO unverifyCourseCreator(UUID courseCreatorUuid, String reason) {
        courseCreatorOnboardingService.moderate(courseCreatorUuid, "revoke", reason);
        return getCourseCreatorByUuid(courseCreatorUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourseCreatorVerified(UUID courseCreatorUuid) {
        CourseCreator courseCreator = courseCreatorRepository.findByUuid(courseCreatorUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_CREATOR_NOT_FOUND_TEMPLATE, courseCreatorUuid)));

        return Boolean.TRUE.equals(courseCreator.getAdminVerified());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorDTO> getVerifiedCourseCreators(Pageable pageable) {
        log.debug("Getting verified course creators with pagination: {}", pageable);
        Specification<CourseCreator> spec = (root, query, cb) ->
                cb.equal(root.get("adminVerified"), true);
        return courseCreatorRepository.findAll(spec, pageable)
                .map(this::toDirectoryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorDTO> getUnverifiedCourseCreators(Pageable pageable) {
        log.debug("Getting unverified course creators with pagination: {}", pageable);
        Specification<CourseCreator> spec = (root, query, cb) ->
                cb.equal(root.get("adminVerified"), false);
        return courseCreatorRepository.findAll(spec, pageable)
                .map(this::toDirectoryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCourseCreatorsByVerificationStatus(boolean verified) {
        Specification<CourseCreator> spec = (root, query, cb) ->
                cb.equal(root.get("adminVerified"), verified);
        return courseCreatorRepository.count(spec);
    }

    // ================================
    // PRIVATE HELPER METHODS
    // ================================

    /**
     * Updates the fields of the existing course creator entity with values from the DTO.
     * Only updates non-null values from the DTO to support partial updates.
     * Note: Read-only fields like uuid, createdDate, createdBy, fullName, verified, etc. are not updated.
     */
    private void updateCourseCreatorFields(CourseCreator existingCourseCreator, CourseCreatorDTO courseCreatorDTO) {
        if (courseCreatorDTO.userUuid() != null
                && !courseCreatorDTO.userUuid().equals(existingCourseCreator.getUserUuid())) {
            // The owning user of a course creator profile is fixed at creation. Allowing an update to
            // re-point it would hand the profile - and every document, course and qualification keyed to
            // it - to another account.
            throw new IllegalArgumentException("user_uuid cannot be reassigned on an existing course creator profile");
        }
        applyCourseCreatorProfile(existingCourseCreator, courseCreatorDTO);
    }

    private void applyCourseCreatorProfile(CourseCreator courseCreator, CourseCreatorDTO courseCreatorDTO) {
        if (courseCreatorDTO.userUuid() != null) {
            courseCreator.setUserUuid(courseCreatorDTO.userUuid());
        }
        if (courseCreatorDTO.fullName() != null) {
            courseCreator.setFullName(courseCreatorDTO.fullName());
        }
        if (courseCreatorDTO.locationName() != null) {
            courseCreator.setLocationName(courseCreatorDTO.locationName());
        }
        if (courseCreatorDTO.latitude() != null) {
            courseCreator.setLatitude(courseCreatorDTO.latitude());
        }
        if (courseCreatorDTO.longitude() != null) {
            courseCreator.setLongitude(courseCreatorDTO.longitude());
        }
        if (courseCreatorDTO.website() != null) {
            courseCreator.setWebsite(courseCreatorDTO.website());
        }
        if (courseCreatorDTO.bio() != null) {
            courseCreator.setBio(courseCreatorDTO.bio());
        }
        if (courseCreatorDTO.professionalHeadline() != null) {
            courseCreator.setProfessionalHeadline(courseCreatorDTO.professionalHeadline());
        }
        applyOnboardingDefaults(courseCreator);
    }

    private void applyOnboardingDefaults(CourseCreator courseCreator) {
        if (courseCreator.getVerificationStatus() == null) {
            courseCreator.setVerificationStatus(Boolean.TRUE.equals(courseCreator.getAdminVerified())
                    ? CourseCreatorVerificationStatus.APPROVED
                    : CourseCreatorVerificationStatus.DRAFT);
        }
    }


    /**
     * A single profile at full coordinate precision for its owner or a platform admin, and at town
     * level for everyone else.
     */
    private CourseCreatorDTO toDtoForCaller(CourseCreator courseCreator) {
        return isOwnedByCaller(courseCreator) || domainSecurityService.isPlatformAdmin()
                ? CourseCreatorFactory.toDTO(courseCreator)
                : CourseCreatorFactory.toPublicDTO(courseCreator);
    }

    /**
     * A list or search row: coordinates at town level, except on the caller's own profile. The
     * profile screens load the owner's record through search and write it back on save, so rounding
     * the owner's own row would silently coarsen their stored location.
     */
    private CourseCreatorDTO toDirectoryDTO(CourseCreator courseCreator) {
        return isOwnedByCaller(courseCreator) ? CourseCreatorFactory.toDTO(courseCreator) : CourseCreatorFactory.toPublicDTO(courseCreator);
    }

    private boolean isOwnedByCaller(CourseCreator courseCreator) {
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        return callerUuid != null && callerUuid.equals(courseCreator.getUserUuid());
    }
}
