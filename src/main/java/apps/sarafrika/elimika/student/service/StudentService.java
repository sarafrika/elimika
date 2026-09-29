package apps.sarafrika.elimika.student.service;

import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;
import java.util.UUID;

public interface StudentService {
    StudentDTO createStudent(StudentDTO studentDTO);
    StudentDTO getStudentByUuId(UUID uuid);
    Page<StudentDTO> getAllStudents(Pageable pageable);
    StudentDTO updateStudent(UUID uuid, StudentDTO studentDTO);
    void deleteStudent(UUID uuid);
    Page<StudentDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Searches like {@link #search(Map, Pageable)}, but only within {@code scope}: the scope is
     * AND-ed onto the request filters inside the query, so page totals count only matching rows.
     *
     * @param scope predicate every returned record must satisfy; null means no restriction
     */
    Page<StudentDTO> search(Map<String, String> searchParams, Pageable pageable, Specification<Student> scope);
}
