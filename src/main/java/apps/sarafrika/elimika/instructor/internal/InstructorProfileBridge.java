package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
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
 * Maps instructor profiles onto the user-owned professional profile: the instructor qualification
 * endpoints keep their shapes while reading and writing the shared user_* tables.
 */
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

    /**
     * Rewrites an {@code instructorUuid} filter into the owning user; without one, bounds the search to
     * users who hold an instructor profile. Other instructor-key operators are rejected, not widened.
     */
    public ScopedSearch scope(Map<String, String> searchParams) {
        Map<String, String> params = new HashMap<>();
        UUID instructorUuid = null;
        if (searchParams != null) {
            for (Map.Entry<String, String> entry : searchParams.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT).replace("_", "");
                if (key.equals("instructoruuid")) {
                    instructorUuid = parse(entry.getValue());
                } else if (key.startsWith("instructoruuid")) {
                    throw new IllegalArgumentException("Only an exact instructorUuid filter is supported");
                } else {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (instructorUuid != null) {
            return new ScopedSearch(params, findUserUuid(instructorUuid).map(List::of).orElse(List.of()));
        }
        return new ScopedSearch(params, instructorRepository.findAllUserUuids());
    }

    private static UUID parse(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("instructorUuid must be a UUID");
        }
    }
}
