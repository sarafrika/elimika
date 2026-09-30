package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.shared.spi.MinorLearnerLookupService;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves minors in two batched hops, each inside its owner: student → user here, and the date-of-birth
 * test in tenancy. No date of birth crosses a module boundary; only the answer does.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MinorLearnerLookupServiceImpl implements MinorLearnerLookupService {

    /** Keeps each IN list well inside PostgreSQL's bind-parameter limit. */
    private static final int CHUNK = 1000;

    private final StudentRepository studentRepository;
    private final UserLookupService userLookupService;

    @Override
    public Set<UUID> findMinorStudentUuids(Collection<UUID> studentUuids, LocalDate asOf) {
        if (studentUuids == null || studentUuids.isEmpty() || asOf == null) {
            return Set.of();
        }
        LocalDate cutoff = asOf.minusYears(AGE_OF_MAJORITY);
        List<UUID> ids = List.copyOf(new HashSet<>(studentUuids));
        Set<UUID> minors = new HashSet<>();
        for (int from = 0; from < ids.size(); from += CHUNK) {
            List<UUID> chunk = ids.subList(from, Math.min(from + CHUNK, ids.size()));
            Map<UUID, List<UUID>> studentsByUser = new HashMap<>();
            for (Student student : studentRepository.findByUuidIn(chunk)) {
                if (student.getUserUuid() != null) {
                    studentsByUser.computeIfAbsent(student.getUserUuid(), key -> new ArrayList<>()).add(student.getUuid());
                }
            }
            if (studentsByUser.isEmpty()) {
                continue;
            }
            for (UUID userUuid : userLookupService.findUserUuidsBornAfter(studentsByUser.keySet(), cutoff)) {
                minors.addAll(studentsByUser.getOrDefault(userUuid, List.of()));
            }
        }
        return minors;
    }
}
