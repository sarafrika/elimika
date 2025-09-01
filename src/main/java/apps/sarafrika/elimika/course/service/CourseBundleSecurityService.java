package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseBundleRepository;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseBundle;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Security service for course bundle operations.
 * Handles ownership validation and access control for bundle operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseBundleSecurityService {
    
    private final CourseRepository courseRepository;
    private final CourseBundleRepository courseBundleRepository;
    private final UserContextService userContextService;
    
    /**
     * Check if current user can create a bundle with the specified courses.
     *
     * @param courseUuids list of course UUIDs to include in bundle
     * @return true if user owns all courses
     */
    public boolean canCreateBundle(List<UUID> courseUuids) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return false;
        }
        
        UUID currentInstructorUuid = userContextService.getCurrentUserUuid();
        if (currentInstructorUuid == null) {
            log.warn("No current user found for bundle creation");
            return false;
        }
        
        return courseUuids.stream()
            .allMatch(courseUuid -> {
                Course course = courseRepository.findByUuid(courseUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseUuid));
                boolean owns = course.getInstructorUuid().equals(currentInstructorUuid);
                if (!owns) {
                    log.warn("User {} does not own course {}", currentInstructorUuid, courseUuid);
                }
                return owns;
            });
    }
    
    /**
     * Check if current user can add a course to an existing bundle.
     *
     * @param bundleUuid the bundle UUID
     * @param courseUuid the course UUID to add
     * @return true if user owns both bundle and course
     */
    public boolean canAddCourseToBundle(UUID bundleUuid, UUID courseUuid) {
        UUID currentInstructorUuid = userContextService.getCurrentUserUuid();
        if (currentInstructorUuid == null) {
            return false;
        }
        
        // Check bundle ownership
        CourseBundle bundle = courseBundleRepository.findByUuid(bundleUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Bundle not found: " + bundleUuid));
        
        if (!bundle.getInstructorUuid().equals(currentInstructorUuid)) {
            log.warn("User {} does not own bundle {}", currentInstructorUuid, bundleUuid);
            return false;
        }
        
        // Check course ownership
        Course course = courseRepository.findByUuid(courseUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseUuid));
        
        if (!course.getInstructorUuid().equals(currentInstructorUuid)) {
            log.warn("User {} does not own course {}", currentInstructorUuid, courseUuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if current user can modify a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return true if user owns the bundle
     */
    public boolean canModifyBundle(UUID bundleUuid) {
        UUID currentInstructorUuid = userContextService.getCurrentUserUuid();
        if (currentInstructorUuid == null) {
            return false;
        }
        
        CourseBundle bundle = courseBundleRepository.findByUuid(bundleUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Bundle not found: " + bundleUuid));
        
        boolean owns = bundle.getInstructorUuid().equals(currentInstructorUuid);
        if (!owns) {
            log.warn("User {} does not own bundle {}", currentInstructorUuid, bundleUuid);
        }
        
        return owns;
    }
    
    /**
     * Check if current user can view a bundle (including drafts).
     *
     * @param bundleUuid the bundle UUID
     * @return true if user owns the bundle or it's published
     */
    public boolean canViewBundle(UUID bundleUuid) {
        UUID currentInstructorUuid = userContextService.getCurrentUserUuid();
        
        CourseBundle bundle = courseBundleRepository.findByUuid(bundleUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Bundle not found: " + bundleUuid));
        
        // Owner can always view their bundles
        if (currentInstructorUuid != null && bundle.getInstructorUuid().equals(currentInstructorUuid)) {
            return true;
        }
        
        // Non-owners can only view published and active bundles
        return bundle.getStatus().getValue().equals("published") && Boolean.TRUE.equals(bundle.getActive());
    }
    
    /**
     * Check if current user can delete a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return true if user owns the bundle and it's not published with active purchases
     */
    public boolean canDeleteBundle(UUID bundleUuid) {
        UUID currentInstructorUuid = userContextService.getCurrentUserUuid();
        if (currentInstructorUuid == null) {
            return false;
        }
        
        CourseBundle bundle = courseBundleRepository.findByUuid(bundleUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Bundle not found: " + bundleUuid));
        
        // Must own the bundle
        if (!bundle.getInstructorUuid().equals(currentInstructorUuid)) {
            return false;
        }
        
        // Can delete draft bundles freely
        if (bundle.getStatus().getValue().equals("draft")) {
            return true;
        }
        
        // For published bundles, additional checks would be needed
        // (e.g., no active purchases) - implement based on business requirements
        log.warn("Attempting to delete published bundle {} - additional validation needed", bundleUuid);
        return false;
    }
}