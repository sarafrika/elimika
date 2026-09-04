package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.security.StudentDirectorySecurityService;
import apps.sarafrika.elimika.student.service.StudentDirectoryService;
import apps.sarafrika.elimika.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Applies the directory projection to whatever {@link StudentService} returns.
 * <p>
 * Every route funnels through {@link StudentDirectorySecurityService#project(StudentDTO)}, including
 * the single-record one, so there is exactly one place where a guardian contact can leave this
 * module and exactly one rule deciding whether it may. The caller's relationships are resolved once
 * per request, so projecting a page costs no queries beyond the page itself.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
@Service
@RequiredArgsConstructor
public class StudentDirectoryServiceImpl implements StudentDirectoryService {

    private final StudentService studentService;
    private final StudentDirectorySecurityService studentDirectorySecurityService;

    @Override
    public StudentDTO getStudent(UUID uuid) {
        return studentDirectorySecurityService.project(studentService.getStudentByUuId(uuid));
    }

    @Override
    public Page<StudentDTO> listStudents(Pageable pageable) {
        return studentService.getAllStudents(pageable).map(studentDirectorySecurityService::project);
    }

    @Override
    public Page<StudentDTO> searchStudents(Map<String, String> searchParams, Pageable pageable) {
        return studentService.search(searchParams, pageable).map(studentDirectorySecurityService::project);
    }
}
