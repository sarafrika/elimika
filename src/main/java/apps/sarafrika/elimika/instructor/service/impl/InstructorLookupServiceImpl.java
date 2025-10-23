package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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

}