package apps.sarafrika.elimika.coursecreator.internal;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Serves the course creator qualification and wallet endpoints, unchanged in shape, from the shared user_* tables. */
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

    /** Turns courseCreatorUuid / courseCreatorUuid_in filters into their users; without one, bounds to course creators. */
    public ScopedSearch scope(Map<String, String> searchParams) {
        Map<String, String> params = new HashMap<>();
        Set<UUID> courseCreatorUuids = null;
        if (searchParams != null) {
            for (Map.Entry<String, String> entry : searchParams.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT).replace("_", "");
                if (key.equals("coursecreatoruuid")) {
                    courseCreatorUuids = narrow(courseCreatorUuids, Set.of(parse(entry.getValue())));
                } else if (key.equals("coursecreatoruuidin")) {
                    courseCreatorUuids = narrow(courseCreatorUuids, parseAll(entry.getValue()));
                } else if (key.startsWith("coursecreatoruuid")) {
                    throw new IllegalArgumentException("Only courseCreatorUuid and courseCreatorUuid_in filters are supported");
                } else {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (courseCreatorUuids != null) {
            return new ScopedSearch(params, userUuidsOf(courseCreatorUuids));
        }
        return new ScopedSearch(params, courseCreatorRepository.findAllUserUuids());
    }

    private List<UUID> userUuidsOf(Set<UUID> courseCreatorUuids) {
        if (courseCreatorUuids.isEmpty()) {
            return List.of();
        }
        return courseCreatorRepository.findByUuidIn(courseCreatorUuids).stream()
                .map(CourseCreator::getUserUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static Set<UUID> narrow(Set<UUID> current, Set<UUID> next) {
        if (current == null) {
            return new HashSet<>(next);
        }
        current.retainAll(next);
        return current;
    }

    private static Set<UUID> parseAll(String value) {
        Set<UUID> result = new HashSet<>();
        if (value == null) {
            return result;
        }
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                result.add(parse(part));
            }
        }
        return result;
    }

    private static UUID parse(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("courseCreatorUuid must be a UUID");
        }
    }
}
