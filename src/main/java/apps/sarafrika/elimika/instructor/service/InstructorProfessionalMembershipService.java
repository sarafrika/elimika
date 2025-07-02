package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.InstructorProfessionalMembershipDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface InstructorProfessionalMembershipService {
    InstructorProfessionalMembershipDTO createInstructorProfessionalMembership(InstructorProfessionalMembershipDTO membershipDTO);
    InstructorProfessionalMembershipDTO getInstructorProfessionalMembershipByUuid(UUID uuid);
    Page<InstructorProfessionalMembershipDTO> getAllInstructorProfessionalMemberships(Pageable pageable);
    InstructorProfessionalMembershipDTO updateInstructorProfessionalMembership(UUID uuid, InstructorProfessionalMembershipDTO membershipDTO);
    void deleteInstructorProfessionalMembership(UUID uuid);
    Page<InstructorProfessionalMembershipDTO> search(Map<String, String> searchParams, Pageable pageable);
}