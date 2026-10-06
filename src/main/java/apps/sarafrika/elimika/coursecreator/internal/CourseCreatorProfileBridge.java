package apps.sarafrika.elimika.coursecreator.internal;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Maps course creator profiles onto the user-owned professional profile: the course creator
 * qualification and wallet endpoints keep their shapes while using the shared user_* tables.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCreatorProfileBridge {

    private final CourseCreatorRepository courseCreatorRepository;

    public record ScopedSearch(Map<String, String> params, Collection<UUID> userUuids) {
    }

    public UUID requireUserUuid(UUID courseCreatorUuid) {
        return findUserUuid(courseCreatorUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Course creator not found for UUID: " + courseCreatorUuid));
    }

    public Optional<UUID> findUserUuid(UUID courseCreatorUuid) {
        if (courseCreatorUuid == null) {
            return Optional.empty();
        }
        return courseCreatorRepository.findByUuid(courseCreatorUuid).map(CourseCreator::getUserUuid);
    }

    public Optional<UUID> findCourseCreatorUuid(UUID userUuid) {
        if (userUuid == null) {
            return Optional.empty();
        }
        return courseCreatorRepository.findByUserUuid(userUuid).map(CourseCreator::getUuid);
    }

    public Map<UUID, UUID> courseCreatorUuidsByUser(Collection<UUID> userUuids) {
        Map<UUID, UUID> result = new HashMap<>();
        if (userUuids == null || userUuids.isEmpty()) {
            return result;
        }
        for (CourseCreator creator : courseCreatorRepository.findByUserUuidIn(userUuids)) {
            result.putIfAbsent(creator.getUserUuid(), creator.getUuid());
        }
        return result;
    }

    /** Rewrites a {@code courseCreatorUuid} filter into the owning user, else bounds to course creators. */
    public ScopedSearch scope(Map<String, String> searchParams) {
        Map<String, String> params = new HashMap<>();
        UUID courseCreatorUuid = null;
        if (searchParams != null) {
            for (Map.Entry<String, String> entry : searchParams.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT).replace("_", "");
                if (key.equals("coursecreatoruuid")) {
                    courseCreatorUuid = parse(entry.getValue());
                } else if (key.startsWith("coursecreatoruuid")) {
                    throw new IllegalArgumentException("Only an exact courseCreatorUuid filter is supported");
                } else {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (courseCreatorUuid != null) {
            return new ScopedSearch(params, findUserUuid(courseCreatorUuid).map(List::of).orElse(List.of()));
        }
        return new ScopedSearch(params, courseCreatorRepository.findAllUserUuids());
    }

    private static UUID parse(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("courseCreatorUuid must be a UUID");
        }
    }
}
