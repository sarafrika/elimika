package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.factory.StudentFactory;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final GenericSpecificationBuilder<Student> specificationBuilder;

    public static final String STUDENT_NOT_FOUND_TEMPLATE = "Student with ID %s not found";

    @Override
    public StudentDTO createStudent(StudentDTO studentDTO) {
        Student student = StudentFactory.toEntity(studentDTO);
        Student savedStudent = studentRepository.save(student);
        return StudentFactory.toDTO(savedStudent);
    }

    @Override
    public StudentDTO getStudentByUuId(UUID uuid) {
        return studentRepository.findByUuid(uuid)
                .map(StudentFactory::toDTO)
                .orElseThrow(() -> new RecordNotFoundException(String.format(STUDENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    public Page<StudentDTO> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable).map(StudentFactory::toDTO);
    }

    @Override
    public StudentDTO updateStudent(UUID uuid, StudentDTO studentDTO) {
        Student existingStudent = studentRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException(String.format(STUDENT_NOT_FOUND_TEMPLATE, uuid)));

        existingStudent.setFullName(studentDTO.fullName());
        existingStudent.setUserUuid(studentDTO.userUuid());
        existingStudent.setFirstGuardianName(studentDTO.firstGuardianName());
        existingStudent.setFirstGuardianMobile(studentDTO.firstGuardianMobile());
        existingStudent.setSecondGuardianName(studentDTO.secondGuardianName());
        existingStudent.setSecondGuardianMobile(studentDTO.secondGuardianMobile());

        Student updatedStudent = studentRepository.save(existingStudent);
        return StudentFactory.toDTO(updatedStudent);
    }

    @Override
    public void deleteStudent(UUID uuid) {
        if (!studentRepository.existsByUuid(uuid)) {
            throw new RecordNotFoundException("Student not found with ID: " + uuid);
        }
        studentRepository.deleteByUuid(uuid);
    }

    @Override
    public Page<StudentDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Student> spec = specificationBuilder.buildSpecification(Student.class, searchParams);
        Page<Student> emailContact = studentRepository.findAll(spec, pageable);
        return emailContact.map(StudentFactory::toDTO);
    }
}
