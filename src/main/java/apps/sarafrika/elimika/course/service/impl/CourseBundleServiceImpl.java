package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseBundleDTO;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.factory.CourseBundleFactory;
import apps.sarafrika.elimika.course.factory.CourseFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseBundle;
import apps.sarafrika.elimika.course.model.CourseBundleCourse;
import apps.sarafrika.elimika.course.repository.CourseBundleCourseRepository;
import apps.sarafrika.elimika.course.repository.CourseBundleRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseBundleSecurityService;
import apps.sarafrika.elimika.course.service.CourseBundleService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseBundleServiceImpl implements CourseBundleService {

    private static final String BUNDLE_NOT_FOUND_TEMPLATE = "Course bundle not found with UUID: %s";
    private static final String COURSE_NOT_FOUND_TEMPLATE = "Course not found with UUID: %s";
    
    private final CourseBundleRepository courseBundleRepository;
    private final CourseBundleCourseRepository courseBundleCourseRepository;
    private final CourseRepository courseRepository;
    private final CourseBundleSecurityService securityService;
    private final UserContextService userContextService;

    @Override
    @Transactional
    public CourseBundleDTO createCourseBundle(CourseBundleDTO courseBundleDTO) {
        log.info("Creating new course bundle: {}", courseBundleDTO.name());
        
        // Validate instructor ownership if courses are specified
        validateBundleCreation(courseBundleDTO);
        
        CourseBundle courseBundle = CourseBundleFactory.toEntity(courseBundleDTO);
        
        // Set default values
        if (courseBundle.getStatus() == null) {
            courseBundle.setStatus(ContentStatus.DRAFT);
        }
        if (courseBundle.getActive() == null) {
            courseBundle.setActive(false);
        }
        
        // Set current user as instructor if not specified
        if (courseBundle.getInstructorUuid() == null) {
            courseBundle.setInstructorUuid(userContextService.getCurrentUserUuid());
        }
        
        CourseBundle savedBundle = courseBundleRepository.save(courseBundle);
        log.info("Created course bundle with UUID: {}", savedBundle.getUuid());
        
        return CourseBundleFactory.toDTO(savedBundle);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseBundleDTO getCourseBundleByUuid(UUID uuid) {
        log.debug("Fetching course bundle with UUID: {}", uuid);
        
        CourseBundle courseBundle = courseBundleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BUNDLE_NOT_FOUND_TEMPLATE, uuid)));
        
        // Check view permissions
        if (!securityService.canViewBundle(uuid)) {
            throw new IllegalArgumentException("Access denied to course bundle: " + uuid);
        }
        
        return CourseBundleFactory.toDTO(courseBundle);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseBundleDTO> getAllCourseBundles(Pageable pageable) {
        log.debug("Fetching all course bundles with pagination");
        return courseBundleRepository.findAll(pageable)
                .map(CourseBundleFactory::toDTO);
    }

    @Override
    @Transactional
    public CourseBundleDTO updateCourseBundle(UUID uuid, CourseBundleDTO courseBundleDTO) {
        log.info("Updating course bundle with UUID: {}", uuid);
        
        CourseBundle existingBundle = courseBundleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BUNDLE_NOT_FOUND_TEMPLATE, uuid)));
        
        // Check modification permissions
        if (!securityService.canModifyBundle(uuid)) {
            throw new IllegalArgumentException("Access denied to modify course bundle: " + uuid);
        }
        
        // Validate business rules for updates
        validateBundleUpdate(existingBundle, courseBundleDTO);
        
        CourseBundleFactory.updateEntityFromDTO(existingBundle, courseBundleDTO);
        CourseBundle savedBundle = courseBundleRepository.save(existingBundle);
        
        log.info("Updated course bundle with UUID: {}", uuid);
        return CourseBundleFactory.toDTO(savedBundle);
    }

    @Override
    @Transactional
    public void deleteCourseBundle(UUID uuid) {
        log.info("Deleting course bundle with UUID: {}", uuid);
        
        if (!courseBundleRepository.findByUuid(uuid).isPresent()) {
            throw new ResourceNotFoundException(String.format(BUNDLE_NOT_FOUND_TEMPLATE, uuid));
        }
        
        // Check deletion permissions
        if (!securityService.canDeleteBundle(uuid)) {
            throw new IllegalArgumentException("Cannot delete course bundle: " + uuid);
        }
        
        // Remove all course associations first
        courseBundleCourseRepository.deleteByBundleUuid(uuid);
        
        // Delete the bundle
        courseBundleRepository.deleteByUuid(uuid);
        log.info("Deleted course bundle with UUID: {}", uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseBundleDTO> search(Map<String, String> searchParams, Pageable pageable) {
        log.debug("Searching course bundles with params: {}", searchParams);
        
        Specification<CourseBundle> spec = buildSearchSpecification(searchParams);
        return courseBundleRepository.findAll(spec, pageable)
                .map(CourseBundleFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseBundleDTO> getCourseBundlesByInstructor(UUID instructorUuid, Pageable pageable) {
        log.debug("Fetching course bundles for instructor: {}", instructorUuid);
        return courseBundleRepository.findByInstructorUuid(instructorUuid, pageable)
                .map(CourseBundleFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseBundleDTO> getPublishedCourseBundles(Pageable pageable) {
        log.debug("Fetching published course bundles");
        return courseBundleRepository.findByStatusAndActiveTrue(ContentStatus.PUBLISHED, pageable)
                .map(CourseBundleFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO> getBundleCourses(UUID bundleUuid) {
        log.debug("Fetching courses for bundle: {}", bundleUuid);
        
        // Check view permissions
        if (!securityService.canViewBundle(bundleUuid)) {
            throw new IllegalArgumentException("Access denied to view bundle courses: " + bundleUuid);
        }
        
        return courseBundleCourseRepository.findByBundleUuidOrderBySequenceOrderAsc(bundleUuid)
                .stream()
                .map(bc -> courseRepository.findByUuid(bc.getCourseUuid()))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .map(CourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseBundleDTO addCourseToBundle(UUID bundleUuid, UUID courseUuid, Integer sequenceOrder, Boolean isRequired) {
        log.info("Adding course {} to bundle {}", courseUuid, bundleUuid);
        
        // Check permissions
        if (!securityService.canAddCourseToBundle(bundleUuid, courseUuid)) {
            throw new IllegalArgumentException("Access denied to add course to bundle");
        }
        
        // Check if course already in bundle
        if (courseBundleCourseRepository.existsByBundleUuidAndCourseUuid(bundleUuid, courseUuid)) {
            throw new IllegalArgumentException("Course is already in the bundle");
        }
        
        // Validate course exists and is eligible
        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));
        
        // Create association
        CourseBundleCourse association = new CourseBundleCourse();
        association.setBundleUuid(bundleUuid);
        association.setCourseUuid(courseUuid);
        association.setSequenceOrder(sequenceOrder != null ? sequenceOrder : getNextSequenceOrder(bundleUuid));
        association.setIsRequired(isRequired != null ? isRequired : true);
        
        courseBundleCourseRepository.save(association);
        
        // Return updated bundle
        return getCourseBundleByUuid(bundleUuid);
    }

    @Override
    @Transactional
    public CourseBundleDTO removeCourseFromBundle(UUID bundleUuid, UUID courseUuid) {
        log.info("Removing course {} from bundle {}", courseUuid, bundleUuid);
        
        // Check permissions
        if (!securityService.canModifyBundle(bundleUuid)) {
            throw new IllegalArgumentException("Access denied to modify bundle");
        }
        
        courseBundleCourseRepository.deleteByBundleUuidAndCourseUuid(bundleUuid, courseUuid);
        
        // Return updated bundle
        return getCourseBundleByUuid(bundleUuid);
    }

    // Lifecycle management methods
    
    @Override
    @Transactional(readOnly = true)
    public boolean isBundleReadyForPublishing(UUID uuid) {
        log.debug("Checking if bundle is ready for publishing: {}", uuid);
        
        CourseBundle bundle = courseBundleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BUNDLE_NOT_FOUND_TEMPLATE, uuid)));
        
        // Must be in draft status
        if (!bundle.getStatus().equals(ContentStatus.DRAFT)) {
            return false;
        }
        
        // Must have at least one course
        long courseCount = courseBundleCourseRepository.countByBundleUuid(uuid);
        if (courseCount == 0) {
            return false;
        }
        
        // All courses must be published
        return courseBundleCourseRepository.areAllCoursesPublished(uuid);
    }

    @Override
    @Transactional
    public CourseBundleDTO publishBundle(UUID uuid) {
        log.info("Publishing course bundle: {}", uuid);
        
        if (!securityService.canModifyBundle(uuid)) {
            throw new IllegalArgumentException("Access denied to publish bundle");
        }
        
        if (!isBundleReadyForPublishing(uuid)) {
            throw new IllegalArgumentException("Bundle is not ready for publishing");
        }
        
        CourseBundle bundle = courseBundleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BUNDLE_NOT_FOUND_TEMPLATE, uuid)));
        
        bundle.setStatus(ContentStatus.PUBLISHED);
        bundle.setActive(true);
        
        CourseBundle savedBundle = courseBundleRepository.save(bundle);
        log.info("Published course bundle: {}", uuid);
        
        return CourseBundleFactory.toDTO(savedBundle);
    }

    @Override
    @Transactional
    public CourseBundleDTO unpublishBundle(UUID uuid) {
        log.info("Unpublishing course bundle: {}", uuid);
        
        if (!canUnpublishBundle(uuid)) {
            throw new IllegalArgumentException("Bundle cannot be unpublished");
        }
        
        CourseBundle bundle = courseBundleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BUNDLE_NOT_FOUND_TEMPLATE, uuid)));
        
        bundle.setStatus(ContentStatus.DRAFT);
        bundle.setActive(false);
        
        CourseBundle savedBundle = courseBundleRepository.save(bundle);
        log.info("Unpublished course bundle: {}", uuid);
        
        return CourseBundleFactory.toDTO(savedBundle);
    }

    @Override
    @Transactional
    public CourseBundleDTO archiveBundle(UUID uuid) {
        log.info("Archiving course bundle: {}", uuid);
        
        if (!securityService.canModifyBundle(uuid)) {
            throw new IllegalArgumentException("Access denied to archive bundle");
        }
        
        CourseBundle bundle = courseBundleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BUNDLE_NOT_FOUND_TEMPLATE, uuid)));
        
        bundle.setStatus(ContentStatus.ARCHIVED);
        bundle.setActive(false);
        
        CourseBundle savedBundle = courseBundleRepository.save(bundle);
        log.info("Archived course bundle: {}", uuid);
        
        return CourseBundleFactory.toDTO(savedBundle);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUnpublishBundle(UUID uuid) {
        if (!securityService.canModifyBundle(uuid)) {
            return false;
        }
        
        CourseBundle bundle = courseBundleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BUNDLE_NOT_FOUND_TEMPLATE, uuid)));
        
        // Can only unpublish if currently published
        if (!bundle.getStatus().equals(ContentStatus.PUBLISHED)) {
            return false;
        }
        
        // Add business logic for checking active purchases, enrollments, etc.
        // For now, allowing unpublishing - extend based on business requirements
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentStatus> getAvailableStatusTransitions(UUID uuid) {
        CourseBundle bundle = courseBundleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BUNDLE_NOT_FOUND_TEMPLATE, uuid)));
        
        return switch (bundle.getStatus()) {
            case DRAFT -> isBundleReadyForPublishing(uuid) 
                ? Arrays.asList(ContentStatus.IN_REVIEW, ContentStatus.PUBLISHED)
                : Arrays.asList(ContentStatus.IN_REVIEW);
            case IN_REVIEW -> Arrays.asList(ContentStatus.DRAFT, ContentStatus.PUBLISHED);
            case PUBLISHED -> Arrays.asList(ContentStatus.DRAFT, ContentStatus.ARCHIVED);
            case ARCHIVED -> Arrays.asList(ContentStatus.DRAFT);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateBundleContent(UUID uuid) {
        long courseCount = courseBundleCourseRepository.countByBundleUuid(uuid);
        return courseCount > 0 && courseBundleCourseRepository.areAllCoursesPublished(uuid);
    }

    @Override
    @Transactional
    public CourseBundleDTO uploadThumbnail(UUID bundleUuid, MultipartFile thumbnail) {
        // Implementation depends on your file upload service
        // For now, returning the bundle as-is
        log.info("Thumbnail upload requested for bundle: {}", bundleUuid);
        throw new IllegalArgumentException("Thumbnail upload not implemented yet");
    }

    @Override
    @Transactional
    public CourseBundleDTO uploadBanner(UUID bundleUuid, MultipartFile banner) {
        // Implementation depends on your file upload service
        // For now, returning the bundle as-is
        log.info("Banner upload requested for bundle: {}", bundleUuid);
        throw new IllegalArgumentException("Banner upload not implemented yet");
    }

    // Private helper methods
    
    private void validateBundleCreation(CourseBundleDTO courseBundleDTO) {
        // Validate bundle name uniqueness for instructor
        if (courseBundleDTO.instructorUuid() != null && courseBundleDTO.name() != null) {
            boolean exists = courseBundleRepository.existsByInstructorUuidAndNameIgnoreCase(
                    courseBundleDTO.instructorUuid(), courseBundleDTO.name());
            if (exists) {
                throw new IllegalArgumentException("Bundle name already exists for this instructor");
            }
        }
        
        // Validate price
        if (courseBundleDTO.price() != null && courseBundleDTO.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Bundle price cannot be negative");
        }
    }
    
    private void validateBundleUpdate(CourseBundle existingBundle, CourseBundleDTO updateDTO) {
        // Validate name uniqueness if changed
        if (updateDTO.name() != null && !updateDTO.name().equals(existingBundle.getName())) {
            boolean exists = courseBundleRepository.existsByInstructorUuidAndNameIgnoreCaseAndUuidNot(
                    existingBundle.getInstructorUuid(), updateDTO.name(), existingBundle.getUuid());
            if (exists) {
                throw new IllegalArgumentException("Bundle name already exists for this instructor");
            }
        }
        
        // Prevent certain changes on published bundles
        if (existingBundle.getStatus().equals(ContentStatus.PUBLISHED)) {
            if (updateDTO.price() != null && 
                updateDTO.price().compareTo(existingBundle.getPrice()) < 0) {
                throw new IllegalArgumentException("Cannot decrease price of published bundle");
            }
        }
    }
    
    private Integer getNextSequenceOrder(UUID bundleUuid) {
        Integer maxOrder = courseBundleCourseRepository.findMaxSequenceOrderByBundleUuid(bundleUuid);
        return maxOrder != null ? maxOrder + 1 : 1;
    }
    
    private Specification<CourseBundle> buildSearchSpecification(Map<String, String> searchParams) {
        // Basic implementation - extend based on search requirements
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();
            
            if (searchParams.containsKey("name")) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                                "%" + searchParams.get("name").toLowerCase() + "%"));
            }
            
            if (searchParams.containsKey("status")) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("status"),
                                ContentStatus.fromValue(searchParams.get("status"))));
            }
            
            if (searchParams.containsKey("instructorUuid")) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("instructorUuid"),
                                UUID.fromString(searchParams.get("instructorUuid"))));
            }
            
            return predicates;
        };
    }
}