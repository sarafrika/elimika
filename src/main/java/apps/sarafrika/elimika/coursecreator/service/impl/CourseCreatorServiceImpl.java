package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDTO;
import apps.sarafrika.elimika.coursecreator.factory.CourseCreatorFactory;
import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorService;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
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

    private static final String COURSE_CREATOR_NOT_FOUND_TEMPLATE = "Course creator with ID %s not found";

    @Override
    public CourseCreatorDTO createCourseCreator(CourseCreatorDTO courseCreatorDTO) {
        CourseCreator courseCreator = CourseCreatorFactory.toEntity(courseCreatorDTO);

        courseCreator.setAdminVerified(false);

        CourseCreator savedCourseCreator = courseCreatorRepository.save(courseCreator);

        applicationEventPublisher.publishEvent(
                new UserDomainMappingEvent(courseCreator.getUserUuid(), UserDomain.course_creator.name())
        );

        return CourseCreatorFactory.toDTO(savedCourseCreator);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorDTO getCourseCreatorByUuid(UUID uuid) {
        return courseCreatorRepository.findByUuid(uuid)
                .map(CourseCreatorFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_CREATOR_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorDTO> getAllCourseCreators(Pageable pageable) {
        return courseCreatorRepository.findAll(pageable).map(CourseCreatorFactory::toDTO);
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
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseCreator> spec = specificationBuilder.buildSpecification(CourseCreator.class, searchParams);
        return courseCreatorRepository.findAll(spec, pageable).map(CourseCreatorFactory::toDTO);
    }

    // ================================
    // COURSE CREATOR VERIFICATION
    // ================================

    @Override
    public CourseCreatorDTO verifyCourseCreator(UUID courseCreatorUuid, String reason) {
        log.info("Verifying course creator {} for reason: {}", courseCreatorUuid, reason);

        CourseCreator courseCreator = courseCreatorRepository.findByUuid(courseCreatorUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_CREATOR_NOT_FOUND_TEMPLATE, courseCreatorUuid)));

        domainSecurityService.enforceNotSelfApprovingProfile(
                courseCreator.getUserUuid(),
                "course creator"
        );

        courseCreator.setAdminVerified(true);
        CourseCreator verifiedCourseCreator = courseCreatorRepository.save(courseCreator);

        log.info("Successfully verified course creator {}", courseCreatorUuid);
        return CourseCreatorFactory.toDTO(verifiedCourseCreator);
    }

    @Override
    public CourseCreatorDTO unverifyCourseCreator(UUID courseCreatorUuid, String reason) {
        log.info("Removing verification from course creator {} for reason: {}", courseCreatorUuid, reason);

        CourseCreator courseCreator = courseCreatorRepository.findByUuid(courseCreatorUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_CREATOR_NOT_FOUND_TEMPLATE, courseCreatorUuid)));

        courseCreator.setAdminVerified(false);
        CourseCreator unverifiedCourseCreator = courseCreatorRepository.save(courseCreator);

        log.info("Successfully removed verification from course creator {}", courseCreatorUuid);
        return CourseCreatorFactory.toDTO(unverifiedCourseCreator);
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
                .map(CourseCreatorFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorDTO> getUnverifiedCourseCreators(Pageable pageable) {
        log.debug("Getting unverified course creators with pagination: {}", pageable);
        Specification<CourseCreator> spec = (root, query, cb) ->
                cb.equal(root.get("adminVerified"), false);
        return courseCreatorRepository.findAll(spec, pageable)
                .map(CourseCreatorFactory::toDTO);
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
        if (courseCreatorDTO.fullName() != null) {
            existingCourseCreator.setFullName(courseCreatorDTO.fullName());
        }
        if (courseCreatorDTO.website() != null) {
            existingCourseCreator.setWebsite(courseCreatorDTO.website());
        }
        if (courseCreatorDTO.bio() != null) {
            existingCourseCreator.setBio(courseCreatorDTO.bio());
        }
        if (courseCreatorDTO.professionalHeadline() != null) {
            existingCourseCreator.setProfessionalHeadline(courseCreatorDTO.professionalHeadline());
        }
    }
}
