package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of Instructor Lookup Service
 * <p>
 * Provides read-only access to instructor information.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorLookupServiceImpl implements InstructorLookupService {

    private final InstructorRepository instructorRepository;

    @Override
    public Optional<UUID> findInstructorUuidByUserUuid(UUID userUuid) {
        return instructorRepository.findByUserUuid(userUuid)
                .map(Instructor::getUuid);
    }

    @Override
    public boolean instructorExists(UUID instructorUuid) {
        return instructorRepository.existsByUuid(instructorUuid);
    }

    @Override
    public Optional<UUID> getInstructorUserUuid(UUID instructorUuid) {
        return instructorRepository.findByUuid(instructorUuid)
                .map(Instructor::getUserUuid);
    }

    @Override
    public Optional<Boolean> getInstructorProfileCompleteByUserUuid(UUID userUuid) {
        return instructorRepository.findByUserUuid(userUuid)
                .map(instructor -> hasText(instructor.getBio()) && hasText(instructor.getProfessionalHeadline()));
    }

    @Override
    public Optional<Boolean> isInstructorAdminVerified(UUID instructorUuid) {
        return instructorRepository.findByUuid(instructorUuid)
                .map(instructor -> Boolean.TRUE.equals(instructor.getAdminVerified()));
    }

    @Override
    public Map<UUID, InstructorDirectoryEntry> findInstructorDirectoryEntries(Collection<UUID> instructorUuids) {
        if (instructorUuids == null || instructorUuids.isEmpty()) {
            return Map.of();
        }

        Collection<UUID> requested = instructorUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return Map.of();
        }

        Map<UUID, InstructorDirectoryEntry> entries = new LinkedHashMap<>();
        for (InstructorDirectoryEntry entry : instructorRepository.findDirectoryEntriesByUuidIn(requested)) {
            if (entry.instructorUuid() != null) {
                entries.put(entry.instructorUuid(), entry);
            }
        }
        return entries;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
