package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
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

/** Serves the instructor qualification endpoints, unchanged in shape, from the shared user_* tables. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorProfileBridge {

    private final InstructorRepository instructorRepository;

    /** The search filters and the owners a legacy instructor search is bounded to. */
    public record ScopedSearch(Map<String, String> params, Collection<UUID> userUuids) {
    }

    public UUID requireUserUuid(UUID instructorUuid) {
        return findUserUuid(instructorUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor with ID " + instructorUuid + " not found"));
    }

    public Optional<UUID> findUserUuid(UUID instructorUuid) {
        if (instructorUuid == null) {
            return Optional.empty();
        }
        return instructorRepository.findByUuid(instructorUuid).map(Instructor::getUserUuid);
    }

    public Optional<UUID> findInstructorUuid(UUID userUuid) {
        if (userUuid == null) {
            return Optional.empty();
        }
        return instructorRepository.findByUserUuid(userUuid).map(Instructor::getUuid);
    }

    /** user UUID to instructor UUID, for the owners given. */
    public Map<UUID, UUID> instructorUuidsByUser(Collection<UUID> userUuids) {
        Map<UUID, UUID> result = new HashMap<>();
        if (userUuids == null || userUuids.isEmpty()) {
            return result;
        }
        for (Instructor instructor : instructorRepository.findByUserUuidIn(userUuids)) {
            result.putIfAbsent(instructor.getUserUuid(), instructor.getUuid());
        }
        return result;
    }

    /** Turns instructorUuid / instructorUuid_in filters into their users; without one, bounds to instructors. */
    public ScopedSearch scope(Map<String, String> searchParams) {
        Map<String, String> params = new HashMap<>();
        Set<UUID> instructorUuids = null;
        if (searchParams != null) {
            for (Map.Entry<String, String> entry : searchParams.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT).replace("_", "");
                if (key.equals("instructoruuid")) {
                    instructorUuids = narrow(instructorUuids, Set.of(parse(entry.getValue())));
                } else if (key.equals("instructoruuidin")) {
                    instructorUuids = narrow(instructorUuids, parseAll(entry.getValue()));
                } else if (key.startsWith("instructoruuid")) {
                    throw new IllegalArgumentException("Only instructorUuid and instructorUuid_in filters are supported");
                } else {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (instructorUuids != null) {
            return new ScopedSearch(params, userUuidsOf(instructorUuids));
        }
        return new ScopedSearch(params, instructorRepository.findAllUserUuids());
    }

    private List<UUID> userUuidsOf(Set<UUID> instructorUuids) {
        if (instructorUuids.isEmpty()) {
            return List.of();
        }
        return instructorRepository.findByUuidIn(instructorUuids).stream()
                .map(Instructor::getUserUuid)
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
            throw new IllegalArgumentException("instructorUuid must be a UUID");
        }
    }
}
