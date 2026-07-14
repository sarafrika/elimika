package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CertificateDTO;
import apps.sarafrika.elimika.course.factory.CertificateFactory;
import apps.sarafrika.elimika.course.model.Certificate;
import apps.sarafrika.elimika.course.model.CertificateTemplate;
import apps.sarafrika.elimika.course.repository.CertificateRepository;
import apps.sarafrika.elimika.course.repository.CertificateTemplateRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.service.CertificateService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.LearningCertificateIssuedNotificationRequestedEvent;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.course.util.enums.TemplateType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateTemplateRepository certificateTemplateRepository;
    private final GenericSpecificationBuilder<Certificate> specificationBuilder;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final CourseInfoService courseInfoService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String CERTIFICATE_NOT_FOUND_TEMPLATE = "Certificate with ID %s not found";
    private static final SecureRandom CERTIFICATE_NUMBER_RANDOM = new SecureRandom();
    private static final String CERTIFICATE_NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CERTIFICATE_NUMBER_ATTEMPTS = 5;

    @Override
    public CertificateDTO createCertificate(CertificateDTO certificateDTO) {
        if (certificateDTO.templateUuid() != null
                && !certificateTemplateRepository.existsByUuid(certificateDTO.templateUuid())) {
            throw new ResourceNotFoundException(
                    String.format("Certificate template with ID %s not found", certificateDTO.templateUuid()));
        }
        if (certificateDTO.courseUuid() != null && certificateRepository
                .existsByStudentUuidAndCourseUuid(certificateDTO.studentUuid(), certificateDTO.courseUuid())) {
            throw new IllegalStateException("Certificate already exists for this student and course");
        }
        if (certificateDTO.programUuid() != null && certificateRepository
                .existsByStudentUuidAndProgramUuid(certificateDTO.studentUuid(), certificateDTO.programUuid())) {
            throw new IllegalStateException("Certificate already exists for this student and program");
        }

        Certificate certificate = CertificateFactory.toEntity(certificateDTO);

        // Set defaults based on CertificateDTO business logic
        if (certificate.getIssuedDate() == null) {
            certificate.setIssuedDate(LocalDateTime.now());
        }
        if (certificate.getIsValid() == null) {
            certificate.setIsValid(true);
        }

        // Generate certificate number
        if (certificate.getCertificateNumber() == null) {
            certificate.setCertificateNumber(generateCertificateNumber());
        }

        Certificate savedCertificate = certificateRepository.save(certificate);
        publishCertificateIssuedNotification(savedCertificate);
        return CertificateFactory.toDTO(savedCertificate);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateDTO getCertificateByUuid(UUID uuid) {
        return certificateRepository.findByUuid(uuid)
                .map(CertificateFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CERTIFICATE_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificateDTO> getAllCertificates(Pageable pageable) {
        return certificateRepository.findAll(pageable).map(CertificateFactory::toDTO);
    }

    @Override
    public CertificateDTO updateCertificate(UUID uuid, CertificateDTO certificateDTO) {
        Certificate existingCertificate = certificateRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CERTIFICATE_NOT_FOUND_TEMPLATE, uuid)));

        updateCertificateFields(existingCertificate, certificateDTO);

        Certificate updatedCertificate = certificateRepository.save(existingCertificate);
        return CertificateFactory.toDTO(updatedCertificate);
    }

    @Override
    public void deleteCertificate(UUID uuid) {
        if (!certificateRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(CERTIFICATE_NOT_FOUND_TEMPLATE, uuid));
        }
        certificateRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificateDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Certificate> spec = specificationBuilder.buildSpecification(
                Certificate.class, searchParams);
        return certificateRepository.findAll(spec, pageable).map(CertificateFactory::toDTO);
    }

    // Domain-specific methods leveraging CertificateDTO computed properties
    public CertificateDTO generateCourseCertificate(UUID studentUuid, UUID courseUuid, BigDecimal finalGrade) {
        // Check if student is eligible
        if (!isEligibleForCourseCertificate(studentUuid, courseUuid)) {
            throw new IllegalStateException("Student is not eligible for course certificate");
        }

        // Check if certificate already exists
        if (certificateRepository.existsByStudentUuidAndCourseUuid(studentUuid, courseUuid)) {
            throw new IllegalStateException("Certificate already exists for this student and course");
        }

        Certificate certificate = new Certificate();
        certificate.setStudentUuid(studentUuid);
        certificate.setCourseUuid(courseUuid);
        certificate.setTemplateUuid(getDefaultCourseTemplateUuid());
        certificate.setCertificateNumber(generateCertificateNumber());
        certificate.setIssuedDate(LocalDateTime.now());
        certificate.setCompletionDate(LocalDateTime.now());
        certificate.setFinalGrade(finalGrade);
        certificate.setIsValid(true);
        certificate.setCreatedDate(LocalDateTime.now());

        Certificate savedCertificate = certificateRepository.save(certificate);
        publishCertificateIssuedNotification(savedCertificate);
        return CertificateFactory.toDTO(savedCertificate);
    }

    public CertificateDTO generateProgramCertificate(UUID studentUuid, UUID programUuid, BigDecimal finalGrade) {
        // Check if student is eligible
        if (!isEligibleForProgramCertificate(studentUuid, programUuid)) {
            throw new IllegalStateException("Student is not eligible for program certificate");
        }

        // Check if certificate already exists
        if (certificateRepository.existsByStudentUuidAndProgramUuid(studentUuid, programUuid)) {
            throw new IllegalStateException("Certificate already exists for this student and program");
        }

        Certificate certificate = new Certificate();
        certificate.setStudentUuid(studentUuid);
        certificate.setProgramUuid(programUuid);
        certificate.setTemplateUuid(getDefaultProgramTemplateUuid());
        certificate.setCertificateNumber(generateCertificateNumber());
        certificate.setIssuedDate(LocalDateTime.now());
        certificate.setCompletionDate(LocalDateTime.now());
        certificate.setFinalGrade(finalGrade);
        certificate.setIsValid(true);

        Certificate savedCertificate = certificateRepository.save(certificate);
        publishCertificateIssuedNotification(savedCertificate);
        return CertificateFactory.toDTO(savedCertificate);
    }

    public void revokeCertificate(UUID certificateUuid, String reason) {
        Certificate certificate = certificateRepository.findByUuid(certificateUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CERTIFICATE_NOT_FOUND_TEMPLATE, certificateUuid)));

        certificate.setIsValid(false);
        certificate.setRevokedAt(LocalDateTime.now());
        certificate.setRevokedReason(reason);

        certificateRepository.save(certificate);
    }

    @Transactional(readOnly = true)
    public boolean verifyCertificate(String certificateNumber) {
        return certificateRepository.existsByCertificateNumberAndIsValidTrue(certificateNumber);
    }

    @Transactional(readOnly = true)
    public List<CertificateDTO> getCertificatesByStudent(UUID studentUuid) {
        return certificateRepository.findByStudentUuid(studentUuid)
                .stream()
                .map(CertificateFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isEligibleForCourseCertificate(UUID studentUuid, UUID courseUuid) {
        // Check if student completed the course with passing grade
        return courseEnrollmentRepository.existsByStudentUuidAndCourseUuidAndStatus(
                studentUuid, courseUuid, EnrollmentStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public boolean isEligibleForProgramCertificate(UUID studentUuid, UUID programUuid) {
        // Check if student completed the program
        return programEnrollmentRepository.existsByStudentUuidAndProgramUuidAndStatus(
                studentUuid, programUuid, EnrollmentStatus.COMPLETED);
    }

    // Leveraging CertificateDTO computed properties
    @Transactional(readOnly = true)
    public List<CertificateDTO> getDownloadableCertificates(UUID studentUuid) {
        return certificateRepository.findByStudentUuid(studentUuid)
                .stream()
                .map(CertificateFactory::toDTO)
                .filter(CertificateDTO::isDownloadable) // Using computed property
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificateDTO> getValidCertificates(UUID studentUuid) {
        return certificateRepository.findByStudentUuidAndIsValidTrue(studentUuid)
                .stream()
                .map(CertificateFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificateDTO> getCourseCertificates() {
        return certificateRepository.findByCourseUuidIsNotNull()
                .stream()
                .map(CertificateFactory::toDTO)
                .filter(cert -> "Course Completion".equals(cert.getCertificateType()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificateDTO> getProgramCertificates() {
        return certificateRepository.findByProgramUuidIsNotNull()
                .stream()
                .map(CertificateFactory::toDTO)
                .filter(cert -> "Program Completion".equals(cert.getCertificateType()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificateDTO> getRevokedCertificates() {
        return certificateRepository.findByIsValidFalse()
                .stream()
                .map(CertificateFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CertificateDTO getCertificateByNumber(String certificateNumber) {
        return certificateRepository.findByCertificateNumber(certificateNumber)
                .map(CertificateFactory::toDTO)
                .orElse(null);
    }

    public CertificateDTO generateCertificateUrl(UUID certificateUuid, String certificateUrl) {
        Certificate certificate = certificateRepository.findByUuid(certificateUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CERTIFICATE_NOT_FOUND_TEMPLATE, certificateUuid)));

        certificate.setCertificateUrl(apps.sarafrika.elimika.shared.storage.util.FileUrlResolver.toStorableValue(certificateUrl));

        Certificate updatedCertificate = certificateRepository.save(certificate);
        return CertificateFactory.toDTO(updatedCertificate);
    }

    private String generateCertificateNumber() {
        String prefix = "CERT-" + java.time.LocalDate.now().getYear() + "-";
        for (int attempt = 0; attempt < CERTIFICATE_NUMBER_ATTEMPTS; attempt++) {
            StringBuilder suffix = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                suffix.append(CERTIFICATE_NUMBER_ALPHABET
                        .charAt(CERTIFICATE_NUMBER_RANDOM.nextInt(CERTIFICATE_NUMBER_ALPHABET.length())));
            }
            String candidate = prefix + suffix;
            if (!certificateRepository.existsByCertificateNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique certificate number");
    }

    private UUID getDefaultCourseTemplateUuid() {
        return getDefaultTemplateUuid(TemplateType.COURSE_COMPLETION);
    }

    private UUID getDefaultProgramTemplateUuid() {
        return getDefaultTemplateUuid(TemplateType.PROGRAM_COMPLETION);
    }

    private UUID getDefaultTemplateUuid(TemplateType templateType) {
        return certificateTemplateRepository
                .findFirstByTemplateTypeAndIsActiveTrueOrderByCreatedDateAsc(templateType)
                .map(CertificateTemplate::getUuid)
                .orElseThrow(() -> new IllegalStateException(
                        "No active " + templateType.getValue() + " certificate template configured"));
    }

    private void updateCertificateFields(Certificate existingCertificate, CertificateDTO dto) {
        if (dto.studentUuid() != null) {
            existingCertificate.setStudentUuid(dto.studentUuid());
        }
        if (dto.courseUuid() != null) {
            existingCertificate.setCourseUuid(dto.courseUuid());
        }
        if (dto.programUuid() != null) {
            existingCertificate.setProgramUuid(dto.programUuid());
        }
        if (dto.templateUuid() != null) {
            existingCertificate.setTemplateUuid(dto.templateUuid());
        }
        if (dto.completionDate() != null) {
            existingCertificate.setCompletionDate(dto.completionDate());
        }
        if (dto.finalGrade() != null) {
            existingCertificate.setFinalGrade(dto.finalGrade());
        }
        if (dto.certificateUrl() != null) {
            existingCertificate.setCertificateUrl(apps.sarafrika.elimika.shared.storage.util.FileUrlResolver.toStorableValue(dto.certificateUrl()));
        }
        if (dto.isValid() != null) {
            existingCertificate.setIsValid(dto.isValid());
        }
    }

    private void publishCertificateIssuedNotification(Certificate certificate) {
        if (certificate.getStudentUuid() == null) {
            return;
        }

        String learningTitle = certificate.getCourseUuid() != null
                ? courseInfoService.getCourseName(certificate.getCourseUuid()).orElse("your course")
                : courseInfoService.getTrainingProgramTitle(certificate.getProgramUuid()).orElse("your program");

        eventPublisher.publishEvent(new LearningCertificateIssuedNotificationRequestedEvent(
                certificate.getStudentUuid(),
                certificate.getUuid(),
                certificate.getCertificateNumber(),
                certificate.getCourseUuid(),
                certificate.getProgramUuid(),
                learningTitle
        ));
    }
}
