package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorEducationDTO;
import apps.sarafrika.elimika.coursecreator.factory.CourseCreatorEducationFactory;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorEducation;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorDocumentRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorEducationRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorEducationService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.model.DocumentType;
import apps.sarafrika.elimika.shared.repository.DocumentTypeRepository;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorEducationServiceImpl implements CourseCreatorEducationService {

    private final CourseCreatorEducationRepository educationRepository;
    private final GenericSpecificationBuilder<CourseCreatorEducation> specificationBuilder;
    private final CourseCreatorDocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;

    private static final String EDUCATION_NOT_FOUND_TEMPLATE = "Course creator education with ID %s not found";
    private static final String CERTIFICATE_DOCUMENT_TYPE = "CERTIFICATE";
    private static final String CERTIFICATE_REQUIRED_MESSAGE = "Supporting certificate document is required for education updates";

    @Override
    public CourseCreatorEducationDTO createCourseCreatorEducation(CourseCreatorEducationDTO dto) {
        CourseCreatorEducation education = CourseCreatorEducationFactory.toEntity(dto);
        return CourseCreatorEducationFactory.toDTO(educationRepository.save(education));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorEducationDTO getCourseCreatorEducationByUuid(UUID uuid) {
        return educationRepository.findByUuid(uuid)
                .map(CourseCreatorEducationFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(EDUCATION_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorEducationDTO> getAllCourseCreatorEducation(Pageable pageable) {
        return educationRepository.findAll(pageable).map(CourseCreatorEducationFactory::toDTO);
    }

    @Override
    public CourseCreatorEducationDTO updateCourseCreatorEducation(UUID uuid, CourseCreatorEducationDTO dto) {
        CourseCreatorEducation existing = educationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(EDUCATION_NOT_FOUND_TEMPLATE, uuid)));

        enforceCertificateRequirement(existing.getCourseCreatorUuid(), uuid);

        if (dto.courseCreatorUuid() != null) {
            existing.setCourseCreatorUuid(dto.courseCreatorUuid());
        }
        if (dto.qualification() != null) {
            existing.setQualification(dto.qualification());
        }
        if (dto.schoolName() != null) {
            existing.setSchoolName(dto.schoolName());
        }
        if (dto.yearCompleted() != null) {
            existing.setYearCompleted(dto.yearCompleted());
        }
        if (dto.certificateNumber() != null) {
            existing.setCertificateNumber(dto.certificateNumber());
        }

        return CourseCreatorEducationFactory.toDTO(educationRepository.save(existing));
    }

    @Override
    public void deleteCourseCreatorEducation(UUID uuid) {
        if (!educationRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(EDUCATION_NOT_FOUND_TEMPLATE, uuid));
        }
        educationRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorEducationDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseCreatorEducation> spec = specificationBuilder.buildSpecification(CourseCreatorEducation.class, searchParams);
        return educationRepository.findAll(spec, pageable).map(CourseCreatorEducationFactory::toDTO);
    }

    private void enforceCertificateRequirement(UUID courseCreatorUuid, UUID educationUuid) {
        UUID certificateTypeUuid = documentTypeRepository.findByNameIgnoreCase(CERTIFICATE_DOCUMENT_TYPE)
                .map(DocumentType::getUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate document type is not configured"));

        boolean hasCertificate = documentRepository.existsByEducationUuidAndCourseCreatorUuidAndDocumentTypeUuid(
                educationUuid,
                courseCreatorUuid,
                certificateTypeUuid
        );

        if (!hasCertificate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CERTIFICATE_REQUIRED_MESSAGE);
        }
    }
}
