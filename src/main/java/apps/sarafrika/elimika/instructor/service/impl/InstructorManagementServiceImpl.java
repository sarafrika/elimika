package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.service.InstructorService;
import apps.sarafrika.elimika.instructor.spi.InstructorDTO;
import apps.sarafrika.elimika.instructor.spi.InstructorManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation of Instructor Management Service SPI
 * <p>
 * Provides instructor management operations for other modules
 * by delegating to internal instructor services.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
@Service
@RequiredArgsConstructor
public class InstructorManagementServiceImpl implements InstructorManagementService {

    private final InstructorService instructorService;

    @Override
    public InstructorDTO verifyInstructor(UUID instructorUuid, String reason) {
        return instructorService.verifyInstructor(instructorUuid, reason);
    }

    @Override
    public InstructorDTO unverifyInstructor(UUID instructorUuid, String reason) {
        return instructorService.unverifyInstructor(instructorUuid, reason);
    }

    @Override
    public boolean isInstructorVerified(UUID instructorUuid) {
        return instructorService.isInstructorVerified(instructorUuid);
    }

    @Override
    public long countInstructorsByVerificationStatus(boolean verified) {
        return instructorService.countInstructorsByVerificationStatus(verified);
    }
}
