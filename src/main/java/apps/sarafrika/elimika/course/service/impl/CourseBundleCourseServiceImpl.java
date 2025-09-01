package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseBundleCourseDTO;
import apps.sarafrika.elimika.course.factory.CourseBundleCourseFactory;
import apps.sarafrika.elimika.course.model.CourseBundleCourse;
import apps.sarafrika.elimika.course.repository.CourseBundleCourseRepository;
import apps.sarafrika.elimika.course.service.CourseBundleCourseService;
import apps.sarafrika.elimika.course.service.CourseBundleSecurityService;
import apps.sarafrika.elimika.common.exception.EntityNotFoundException;
import apps.sarafrika.elimika.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseBundleCourseServiceImpl implements CourseBundleCourseService {

    private static final String ASSOCIATION_NOT_FOUND_TEMPLATE = "Bundle-course association not found with UUID: %s";
    
    private final CourseBundleCourseRepository courseBundleCourseRepository;
    private final CourseBundleSecurityService securityService;

    @Override
    @Transactional
    public CourseBundleCourseDTO createBundleCourseAssociation(CourseBundleCourseDTO courseBundleCourseDTO) {
        log.info("Creating bundle-course association for bundle: {} and course: {}", 
                courseBundleCourseDTO.bundleUuid(), courseBundleCourseDTO.courseUuid());
        
        // Validate permissions
        if (!securityService.canAddCourseToBundle(courseBundleCourseDTO.bundleUuid(), 
                courseBundleCourseDTO.courseUuid())) {
            throw new ValidationException("Access denied to create bundle-course association");
        }
        
        // Check for existing association
        if (courseBundleCourseRepository.existsByBundleUuidAndCourseUuid(
                courseBundleCourseDTO.bundleUuid(), courseBundleCourseDTO.courseUuid())) {
            throw new ValidationException("Course is already associated with this bundle");
        }
        
        CourseBundleCourse association = CourseBundleCourseFactory.toEntity(courseBundleCourseDTO);
        
        // Set default values
        if (association.getSequenceOrder() == null) {
            association.setSequenceOrder(getNextSequenceOrder(association.getBundleUuid()));
        }
        if (association.getIsRequired() == null) {
            association.setIsRequired(true);
        }
        
        CourseBundleCourse savedAssociation = courseBundleCourseRepository.save(association);
        log.info("Created bundle-course association with UUID: {}", savedAssociation.getUuid());
        
        return CourseBundleCourseFactory.toDTO(savedAssociation);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseBundleCourseDTO getBundleCourseAssociationByUuid(UUID uuid) {
        log.debug("Fetching bundle-course association with UUID: {}", uuid);
        
        CourseBundleCourse association = courseBundleCourseRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ASSOCIATION_NOT_FOUND_TEMPLATE, uuid)));
        
        // Check view permissions
        if (!securityService.canViewBundle(association.getBundleUuid())) {
            throw new ValidationException("Access denied to view bundle-course association: " + uuid);
        }
        
        return CourseBundleCourseFactory.toDTO(association);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseBundleCourseDTO> getAllBundleCourseAssociations(Pageable pageable) {
        log.debug("Fetching all bundle-course associations with pagination");
        return courseBundleCourseRepository.findAll(pageable)
                .map(CourseBundleCourseFactory::toDTO);
    }

    @Override
    @Transactional
    public CourseBundleCourseDTO updateBundleCourseAssociation(UUID uuid, CourseBundleCourseDTO courseBundleCourseDTO) {
        log.info("Updating bundle-course association with UUID: {}", uuid);
        
        CourseBundleCourse existingAssociation = courseBundleCourseRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ASSOCIATION_NOT_FOUND_TEMPLATE, uuid)));
        
        // Check modification permissions
        if (!securityService.canModifyBundle(existingAssociation.getBundleUuid())) {
            throw new ValidationException("Access denied to modify bundle-course association: " + uuid);
        }
        
        // Validate update
        validateAssociationUpdate(existingAssociation, courseBundleCourseDTO);
        
        CourseBundleCourseFactory.updateEntityFromDTO(existingAssociation, courseBundleCourseDTO);
        CourseBundleCourse savedAssociation = courseBundleCourseRepository.save(existingAssociation);
        
        log.info("Updated bundle-course association with UUID: {}", uuid);
        return CourseBundleCourseFactory.toDTO(savedAssociation);
    }

    @Override
    @Transactional
    public void deleteBundleCourseAssociation(UUID uuid) {
        log.info("Deleting bundle-course association with UUID: {}", uuid);
        
        CourseBundleCourse association = courseBundleCourseRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ASSOCIATION_NOT_FOUND_TEMPLATE, uuid)));
        
        // Check deletion permissions
        if (!securityService.canModifyBundle(association.getBundleUuid())) {
            throw new ValidationException("Access denied to delete bundle-course association: " + uuid);
        }
        
        courseBundleCourseRepository.deleteByUuid(uuid);
        log.info("Deleted bundle-course association with UUID: {}", uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseBundleCourseDTO> search(Map<String, String> searchParams, Pageable pageable) {
        log.debug("Searching bundle-course associations with params: {}", searchParams);
        
        Specification<CourseBundleCourse> spec = buildSearchSpecification(searchParams);
        return courseBundleCourseRepository.findAll(spec, pageable)
                .map(CourseBundleCourseFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseBundleCourseDTO> getCoursesByBundle(UUID bundleUuid) {
        log.debug("Fetching courses for bundle: {}", bundleUuid);
        
        // Check view permissions
        if (!securityService.canViewBundle(bundleUuid)) {
            throw new ValidationException("Access denied to view bundle courses: " + bundleUuid);
        }
        
        return courseBundleCourseRepository.findByBundleUuidOrderBySequenceOrderAsc(bundleUuid)
                .stream()
                .map(CourseBundleCourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseBundleCourseDTO> getRequiredCoursesByBundle(UUID bundleUuid) {
        log.debug("Fetching required courses for bundle: {}", bundleUuid);
        
        // Check view permissions
        if (!securityService.canViewBundle(bundleUuid)) {
            throw new ValidationException("Access denied to view bundle courses: " + bundleUuid);
        }
        
        return courseBundleCourseRepository.findByBundleUuidAndIsRequiredTrueOrderBySequenceOrderAsc(bundleUuid)
                .stream()
                .map(CourseBundleCourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseBundleCourseDTO> getOptionalCoursesByBundle(UUID bundleUuid) {
        log.debug("Fetching optional courses for bundle: {}", bundleUuid);
        
        // Check view permissions
        if (!securityService.canViewBundle(bundleUuid)) {
            throw new ValidationException("Access denied to view bundle courses: " + bundleUuid);
        }
        
        return courseBundleCourseRepository.findByBundleUuidAndIsRequiredFalseOrderBySequenceOrderAsc(bundleUuid)
                .stream()
                .map(CourseBundleCourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseBundleCourseDTO> getBundlesByCourse(UUID courseUuid) {
        log.debug("Fetching bundles containing course: {}", courseUuid);
        
        return courseBundleCourseRepository.findByCourseUuid(courseUuid)
                .stream()
                .filter(association -> securityService.canViewBundle(association.getBundleUuid()))
                .map(CourseBundleCourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourseInBundle(UUID bundleUuid, UUID courseUuid) {
        log.debug("Checking if course {} is in bundle {}", courseUuid, bundleUuid);
        return courseBundleCourseRepository.existsByBundleUuidAndCourseUuid(bundleUuid, courseUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCourseCountInBundle(UUID bundleUuid) {
        log.debug("Counting courses in bundle: {}", bundleUuid);
        return courseBundleCourseRepository.countByBundleUuid(bundleUuid);
    }

    @Override
    @Transactional
    public void reorderCoursesInBundle(UUID bundleUuid, Map<UUID, Integer> courseOrder) {
        log.info("Reordering courses in bundle: {}", bundleUuid);
        
        // Check modification permissions
        if (!securityService.canModifyBundle(bundleUuid)) {
            throw new ValidationException("Access denied to reorder courses in bundle: " + bundleUuid);
        }
        
        // Validate all courses belong to the bundle
        List<CourseBundleCourse> associations = courseBundleCourseRepository.findByBundleUuid(bundleUuid);
        for (UUID courseUuid : courseOrder.keySet()) {
            boolean found = associations.stream()
                    .anyMatch(assoc -> assoc.getCourseUuid().equals(courseUuid));
            if (!found) {
                throw new ValidationException("Course " + courseUuid + " is not in bundle " + bundleUuid);
            }
        }
        
        // Update sequence orders
        for (CourseBundleCourse association : associations) {
            Integer newOrder = courseOrder.get(association.getCourseUuid());
            if (newOrder != null) {
                association.setSequenceOrder(newOrder);
                courseBundleCourseRepository.save(association);
            }
        }
        
        log.info("Reordered {} courses in bundle: {}", courseOrder.size(), bundleUuid);
    }

    @Override
    @Transactional
    public void removeAllCoursesFromBundle(UUID bundleUuid) {
        log.info("Removing all courses from bundle: {}", bundleUuid);
        
        // Check modification permissions
        if (!securityService.canModifyBundle(bundleUuid)) {
            throw new ValidationException("Access denied to modify bundle: " + bundleUuid);
        }
        
        long courseCount = courseBundleCourseRepository.countByBundleUuid(bundleUuid);
        courseBundleCourseRepository.deleteByBundleUuid(bundleUuid);
        
        log.info("Removed {} courses from bundle: {}", courseCount, bundleUuid);
    }

    // Private helper methods
    
    private Integer getNextSequenceOrder(UUID bundleUuid) {
        Integer maxOrder = courseBundleCourseRepository.findMaxSequenceOrderByBundleUuid(bundleUuid);
        return maxOrder != null ? maxOrder + 1 : 1;
    }
    
    private void validateAssociationUpdate(CourseBundleCourse existingAssociation, CourseBundleCourseDTO updateDTO) {
        // Prevent changing bundle or course UUID
        if (updateDTO.bundleUuid() != null && 
            !updateDTO.bundleUuid().equals(existingAssociation.getBundleUuid())) {
            throw new ValidationException("Cannot change bundle UUID in association");
        }
        
        if (updateDTO.courseUuid() != null && 
            !updateDTO.courseUuid().equals(existingAssociation.getCourseUuid())) {
            throw new ValidationException("Cannot change course UUID in association");
        }
        
        // Validate sequence order if provided
        if (updateDTO.sequenceOrder() != null) {
            if (updateDTO.sequenceOrder() < 1) {
                throw new ValidationException("Sequence order must be positive");
            }
            
            // Check for duplicate sequence orders
            List<CourseBundleCourse> duplicates = courseBundleCourseRepository
                    .findDuplicateSequenceOrders(existingAssociation.getBundleUuid());
            
            boolean wouldCreateDuplicate = duplicates.stream()
                    .anyMatch(assoc -> !assoc.getUuid().equals(existingAssociation.getUuid()) &&
                            assoc.getSequenceOrder().equals(updateDTO.sequenceOrder()));
            
            if (wouldCreateDuplicate) {
                log.warn("Sequence order {} would create duplicate in bundle {}", 
                        updateDTO.sequenceOrder(), existingAssociation.getBundleUuid());
                // Allow it but log warning - business decision
            }
        }
    }
    
    private Specification<CourseBundleCourse> buildSearchSpecification(Map<String, String> searchParams) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();
            
            if (searchParams.containsKey("bundleUuid")) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("bundleUuid"),
                                UUID.fromString(searchParams.get("bundleUuid"))));
            }
            
            if (searchParams.containsKey("courseUuid")) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("courseUuid"),
                                UUID.fromString(searchParams.get("courseUuid"))));
            }
            
            if (searchParams.containsKey("isRequired")) {
                boolean required = Boolean.parseBoolean(searchParams.get("isRequired"));
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("isRequired"), required));
            }
            
            return predicates;
        };
    }
}