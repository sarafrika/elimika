package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.common.enums.UserDomain;
import apps.sarafrika.elimika.common.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.factory.StudentFactory;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final GenericSpecificationBuilder<Student> specificationBuilder;

    public static final String STUDENT_NOT_FOUND_TEMPLATE = "Student with ID %s not found";

    @Override
    @Transactional
    public StudentDTO createStudent(StudentDTO studentDTO) {
        Student student = StudentFactory.toEntity(studentDTO);
        Student savedStudent = studentRepository.save(student);
        applicationEventPublisher.publishEvent(
                new UserDomainMappingEvent(student.getUserUuid(), UserDomain.student.name())
        );
        return StudentFactory.toDTO(savedStudent);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDTO getStudentByUuId(UUID uuid) {
        return studentRepository.findByUuid(uuid)
                .map(StudentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(STUDENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentDTO> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable).map(StudentFactory::toDTO);
    }

    @Override
    @Transactional
    public StudentDTO updateStudent(UUID uuid, StudentDTO studentDTO) {
        Student existingStudent = studentRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException(String.format(STUDENT_NOT_FOUND_TEMPLATE, uuid)));
        existingStudent.setUserUuid(studentDTO.userUuid());
        existingStudent.setFirstGuardianName(studentDTO.firstGuardianName());
        existingStudent.setFirstGuardianMobile(studentDTO.firstGuardianMobile());
        existingStudent.setSecondGuardianName(studentDTO.secondGuardianName());
        existingStudent.setSecondGuardianMobile(studentDTO.secondGuardianMobile());

        Student updatedStudent = studentRepository.save(existingStudent);
        return StudentFactory.toDTO(updatedStudent);
    }

    @Override
    @Transactional
    public void deleteStudent(UUID uuid) {
        if (!studentRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException("Student not found with ID: " + uuid);
        }
        studentRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Student> spec = specificationBuilder.buildSpecification(Student.class, searchParams);
        Page<Student> emailContact = studentRepository.findAll(spec, pageable);
        return emailContact.map(StudentFactory::toDTO);
    }
}