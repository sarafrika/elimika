package apps.sarafrika.elimika.student.service;

import apps.sarafrika.elimika.student.dto.StudentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface StudentService {
    StudentDTO createStudent(StudentDTO studentDTO);
    StudentDTO getStudentByUuId(UUID uuid);
    Page<StudentDTO> getAllStudents(Pageable pageable);
    StudentDTO updateStudent(UUID uuid, StudentDTO studentDTO);
    void deleteStudent(UUID uuid);
    Page<StudentDTO> search(Map<String, String> searchParams, Pageable pageable);
}
