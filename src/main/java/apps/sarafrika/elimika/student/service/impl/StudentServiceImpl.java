package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.spi.StudentAgeGateException;
import apps.sarafrika.elimika.student.factory.StudentFactory;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.service.StudentService;
import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final GenericSpecificationBuilder<Student> specificationBuilder;

    private final RuleEvaluationService ruleEvaluationService;
    private final UserLookupService userLookupService;

    public static final String STUDENT_NOT_FOUND_TEMPLATE = "Student with ID %s not found";
    private static final String AGE_GATE_RULE_KEY = "student.onboarding.age_gate";

    @Override
    @Transactional
    public StudentDTO createStudent(StudentDTO studentDTO) {
        enforceAgeGate(studentDTO);
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
        enforceAgeGate(studentDTO);
        Student existingStudent = studentRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException(String.format(STUDENT_NOT_FOUND_TEMPLATE, uuid)));
        existingStudent.setUserUuid(studentDTO.userUuid());
        existingStudent.setDemographicTag(studentDTO.demographicTag());
        existingStudent.setFirstGuardianName(studentDTO.firstGuardianName());
        existingStudent.setFirstGuardianMobile(studentDTO.firstGuardianMobile());
        existingStudent.setSecondGuardianName(studentDTO.secondGuardianName());
        existingStudent.setSecondGuardianMobile(studentDTO.secondGuardianMobile());
        existingStudent.setBio(studentDTO.bio());

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

    private void enforceAgeGate(StudentDTO studentDTO) {
        AgeGateDecision decision = ruleEvaluationService.evaluateAgeGate(
                userLookupService.getUserDateOfBirth(studentDTO.userUuid()).orElse(null),
                buildAgeGateContext(studentDTO)
        );
        if (!decision.allowed()) {
            throw new StudentAgeGateException(decision.reason());
        }
    }

    private RuleContext buildAgeGateContext(StudentDTO studentDTO) {
        RuleContext.RuleContextBuilder builder = RuleContext.builder()
                .ruleKey(AGE_GATE_RULE_KEY)
                .evaluationInstant(OffsetDateTime.now(ZoneOffset.UTC));
        if (studentDTO.demographicTag() != null && !studentDTO.demographicTag().isBlank()) {
            builder.demographicTags(Set.of(studentDTO.demographicTag().trim().toLowerCase(Locale.ROOT)));
        }
        return builder.build();
    }
}
