package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.instructor.dto.InstructorProfessionalMembershipDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorProfessionalMembershipFactory;
import apps.sarafrika.elimika.instructor.model.InstructorProfessionalMembership;
import apps.sarafrika.elimika.instructor.repository.InstructorProfessionalMembershipRepository;
import apps.sarafrika.elimika.instructor.service.InstructorProfessionalMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorProfessionalMembershipServiceImpl implements InstructorProfessionalMembershipService {

    private final InstructorProfessionalMembershipRepository membershipRepository;
    private final GenericSpecificationBuilder<InstructorProfessionalMembership> specificationBuilder;

    private static final String MEMBERSHIP_NOT_FOUND_TEMPLATE = "Instructor professional membership with ID %s not found";

    @Override
    public InstructorProfessionalMembershipDTO createInstructorProfessionalMembership(InstructorProfessionalMembershipDTO membershipDTO) {
        InstructorProfessionalMembership membership = InstructorProfessionalMembershipFactory.toEntity(membershipDTO);
        membership.setCreatedDate(LocalDateTime.now());

        // Set default values
        if (membership.getIsActive() == null) {
            membership.setIsActive(true);
        }

        // Business logic: if it's active and no end date, ensure end date is null
        if (Boolean.TRUE.equals(membership.getIsActive()) && membership.getEndDate() == null) {
            membership.setEndDate(null);
        }

        InstructorProfessionalMembership savedMembership = membershipRepository.save(membership);
        return InstructorProfessionalMembershipFactory.toDTO(savedMembership);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorProfessionalMembershipDTO getInstructorProfessionalMembershipByUuid(UUID uuid) {
        return membershipRepository.findByUuid(uuid)
                .map(InstructorProfessionalMembershipFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(MEMBERSHIP_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorProfessionalMembershipDTO> getAllInstructorProfessionalMemberships(Pageable pageable) {
        return membershipRepository.findAll(pageable).map(InstructorProfessionalMembershipFactory::toDTO);
    }

    @Override
    public InstructorProfessionalMembershipDTO updateInstructorProfessionalMembership(UUID uuid, InstructorProfessionalMembershipDTO membershipDTO) {
        InstructorProfessionalMembership existingMembership = membershipRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(MEMBERSHIP_NOT_FOUND_TEMPLATE, uuid)));

        // Update fields from DTO
        updateMembershipFields(existingMembership, membershipDTO);

        // Business logic: if membership is deactivated, set end date to today if not already set
        if (Boolean.FALSE.equals(existingMembership.getIsActive()) && existingMembership.getEndDate() == null) {
            existingMembership.setEndDate(LocalDate.now());
        }

        InstructorProfessionalMembership updatedMembership = membershipRepository.save(existingMembership);
        return InstructorProfessionalMembershipFactory.toDTO(updatedMembership);
    }

    @Override
    public void deleteInstructorProfessionalMembership(UUID uuid) {
        if (!membershipRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(MEMBERSHIP_NOT_FOUND_TEMPLATE, uuid));
        }
        membershipRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorProfessionalMembershipDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<InstructorProfessionalMembership> spec = specificationBuilder.buildSpecification(InstructorProfessionalMembership.class, searchParams);
        return membershipRepository.findAll(spec, pageable).map(InstructorProfessionalMembershipFactory::toDTO);
    }

    private void updateMembershipFields(InstructorProfessionalMembership existingMembership, InstructorProfessionalMembershipDTO dto) {
        if (dto.instructorUuid() != null) {
            existingMembership.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.organizationName() != null) {
            existingMembership.setOrganizationName(dto.organizationName());
        }
        if (dto.membershipNumber() != null) {
            existingMembership.setMembershipNumber(dto.membershipNumber());
        }
        if (dto.startDate() != null) {
            existingMembership.setStartDate(dto.startDate());
        }
        if (dto.endDate() != null) {
            existingMembership.setEndDate(dto.endDate());
        }
        if (dto.isActive() != null) {
            existingMembership.setIsActive(dto.isActive());
        }
    }
}