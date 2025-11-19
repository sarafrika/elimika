package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorProfessionalMembershipDTO;
import apps.sarafrika.elimika.coursecreator.factory.CourseCreatorProfessionalMembershipFactory;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorProfessionalMembership;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorProfessionalMembershipRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorProfessionalMembershipService;
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
public class CourseCreatorProfessionalMembershipServiceImpl implements CourseCreatorProfessionalMembershipService {

    private final CourseCreatorProfessionalMembershipRepository membershipRepository;
    private final GenericSpecificationBuilder<CourseCreatorProfessionalMembership> specificationBuilder;

    private static final String MEMBERSHIP_NOT_FOUND_TEMPLATE = "Course creator membership with ID %s not found";

    @Override
    public CourseCreatorProfessionalMembershipDTO createCourseCreatorProfessionalMembership(CourseCreatorProfessionalMembershipDTO dto) {
        CourseCreatorProfessionalMembership membership = CourseCreatorProfessionalMembershipFactory.toEntity(dto);
        return CourseCreatorProfessionalMembershipFactory.toDTO(membershipRepository.save(membership));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorProfessionalMembershipDTO getCourseCreatorProfessionalMembershipByUuid(UUID uuid) {
        return membershipRepository.findByUuid(uuid)
                .map(CourseCreatorProfessionalMembershipFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(MEMBERSHIP_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorProfessionalMembershipDTO> getAllCourseCreatorProfessionalMemberships(Pageable pageable) {
        return membershipRepository.findAll(pageable).map(CourseCreatorProfessionalMembershipFactory::toDTO);
    }

    @Override
    public CourseCreatorProfessionalMembershipDTO updateCourseCreatorProfessionalMembership(UUID uuid, CourseCreatorProfessionalMembershipDTO dto) {
        CourseCreatorProfessionalMembership existing = membershipRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(MEMBERSHIP_NOT_FOUND_TEMPLATE, uuid)));

        if (dto.courseCreatorUuid() != null) {
            existing.setCourseCreatorUuid(dto.courseCreatorUuid());
        }
        if (dto.organizationName() != null) {
            existing.setOrganizationName(dto.organizationName());
        }
        if (dto.membershipNumber() != null) {
            existing.setMembershipNumber(dto.membershipNumber());
        }
        if (dto.startDate() != null) {
            existing.setStartDate(dto.startDate());
        }
        if (dto.endDate() != null) {
            existing.setEndDate(dto.endDate());
        }
        if (dto.isActive() != null) {
            existing.setIsActive(dto.isActive());
        }

        return CourseCreatorProfessionalMembershipFactory.toDTO(membershipRepository.save(existing));
    }

    @Override
    public void deleteCourseCreatorProfessionalMembership(UUID uuid) {
        if (!membershipRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(MEMBERSHIP_NOT_FOUND_TEMPLATE, uuid));
        }
        membershipRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorProfessionalMembershipDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseCreatorProfessionalMembership> spec = specificationBuilder.buildSpecification(CourseCreatorProfessionalMembership.class, searchParams);
        return membershipRepository.findAll(spec, pageable).map(CourseCreatorProfessionalMembershipFactory::toDTO);
    }
}
