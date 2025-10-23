package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of Course Creator Lookup Service
 * <p>
 * Provides read-only access to course creator information.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCreatorLookupServiceImpl implements CourseCreatorLookupService {

    private final CourseCreatorRepository courseCreatorRepository;

    @Override
    public Optional<UUID> findCourseCreatorUuidByUserUuid(UUID userUuid) {
        return courseCreatorRepository.findByUserUuid(userUuid)
                .map(CourseCreator::getUuid);
    }

    @Override
    public boolean courseCreatorExists(UUID courseCreatorUuid) {
        return courseCreatorRepository.existsByUuid(courseCreatorUuid);
    }

    @Override
    public Optional<UUID> getCourseCreatorUserUuid(UUID courseCreatorUuid) {
        return courseCreatorRepository.findByUuid(courseCreatorUuid)
                .map(CourseCreator::getUserUuid);
    }
}