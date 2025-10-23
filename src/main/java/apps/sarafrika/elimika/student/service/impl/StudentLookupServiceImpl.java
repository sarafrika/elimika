package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of Student Lookup Service
 * <p>
 * Provides read-only access to student information.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentLookupServiceImpl implements StudentLookupService {

    private final StudentRepository studentRepository;

    @Override
    public Optional<UUID> findStudentUuidByUserUuid(UUID userUuid) {
        return studentRepository.findByUserUuid(userUuid)
                .map(Student::getUuid);
    }

    @Override
    public boolean studentExists(UUID studentUuid) {
        return studentRepository.existsByUuid(studentUuid);
    }

    @Override
    public Optional<UUID> getStudentUserUuid(UUID studentUuid) {
        return studentRepository.findByUuid(studentUuid)
                .map(Student::getUserUuid);
    }

}