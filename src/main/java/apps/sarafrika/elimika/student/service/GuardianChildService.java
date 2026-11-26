package apps.sarafrika.elimika.student.service;

import apps.sarafrika.elimika.student.dto.GuardianChildRequestDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkDTO;

import java.util.UUID;

/** Guardians adding children they manage as learners. */
public interface GuardianChildService {

    /**
     * Creates a sign-in-less account and learner profile for the child and links the guardian to it (ACTIVE, FULL
     * scope, primary), which grants the guardian the {@code parent} domain. All or nothing.
     */
    GuardianStudentLinkDTO addChild(UUID guardianUserUuid, GuardianChildRequestDTO request);
}
