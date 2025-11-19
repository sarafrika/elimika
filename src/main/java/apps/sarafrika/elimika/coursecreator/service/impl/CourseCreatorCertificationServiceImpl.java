package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCertificationDTO;
import apps.sarafrika.elimika.coursecreator.factory.CourseCreatorCertificationFactory;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorCertification;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorCertificationRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorCertificationService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorCertificationServiceImpl implements CourseCreatorCertificationService {

    private final CourseCreatorCertificationRepository certificationRepository;
    private final GenericSpecificationBuilder<CourseCreatorCertification> specificationBuilder;

    private static final String CERTIFICATION_NOT_FOUND_TEMPLATE = "Course creator certification with ID %s not found";

    @Override
    public CourseCreatorCertificationDTO createCourseCreatorCertification(CourseCreatorCertificationDTO dto) {
        CourseCreatorCertification certification = CourseCreatorCertificationFactory.toEntity(dto);
        return CourseCreatorCertificationFactory.toDTO(certificationRepository.save(certification));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorCertificationDTO getCourseCreatorCertificationByUuid(UUID uuid) {
        return certificationRepository.findByUuid(uuid)
                .map(CourseCreatorCertificationFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CERTIFICATION_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorCertificationDTO> getAllCourseCreatorCertifications(Pageable pageable) {
        return certificationRepository.findAll(pageable).map(CourseCreatorCertificationFactory::toDTO);
    }

    @Override
    public CourseCreatorCertificationDTO updateCourseCreatorCertification(UUID uuid, CourseCreatorCertificationDTO dto) {
        CourseCreatorCertification existing = certificationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CERTIFICATION_NOT_FOUND_TEMPLATE, uuid)));

        if (dto.courseCreatorUuid() != null) {
            existing.setCourseCreatorUuid(dto.courseCreatorUuid());
        }
        if (dto.certificationName() != null) {
            existing.setCertificationName(dto.certificationName());
        }
        if (dto.issuingOrganization() != null) {
            existing.setIssuingOrganization(dto.issuingOrganization());
        }
        if (dto.issuedDate() != null) {
            existing.setIssuedDate(dto.issuedDate());
        }
        if (dto.expiryDate() != null) {
            existing.setExpiryDate(dto.expiryDate());
        }
        if (dto.credentialId() != null) {
            existing.setCredentialId(dto.credentialId());
        }
        if (dto.credentialUrl() != null) {
            existing.setCredentialUrl(dto.credentialUrl());
        }
        if (dto.description() != null) {
            existing.setDescription(dto.description());
        }
        if (dto.isVerified() != null) {
            existing.setIsVerified(dto.isVerified());
        }

        return CourseCreatorCertificationFactory.toDTO(certificationRepository.save(existing));
    }

    @Override
    public void deleteCourseCreatorCertification(UUID uuid) {
        if (!certificationRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(CERTIFICATION_NOT_FOUND_TEMPLATE, uuid));
        }
        certificationRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorCertificationDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseCreatorCertification> spec = specificationBuilder.buildSpecification(CourseCreatorCertification.class, searchParams);
        return certificationRepository.findAll(spec, pageable).map(CourseCreatorCertificationFactory::toDTO);
    }
}
