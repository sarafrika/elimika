package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ProgramEnrollmentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ProgramEnrollmentService {
    ProgramEnrollmentDTO createProgramEnrollment(ProgramEnrollmentDTO programEnrollmentDTO);

    ProgramEnrollmentDTO getProgramEnrollmentByUuid(UUID uuid);

    Page<ProgramEnrollmentDTO> getAllProgramEnrollments(Pageable pageable);

    ProgramEnrollmentDTO updateProgramEnrollment(UUID uuid, ProgramEnrollmentDTO programEnrollmentDTO);

    void deleteProgramEnrollment(UUID uuid);

    Page<ProgramEnrollmentDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * A program's enrolments as the current caller may see them: the named roster for platform
     * admins and the program's staff (author or approved trainer), the caller's own row for an
     * enrolled learner, and an anonymised tally for everyone else.
     */
    Page<ProgramEnrollmentDTO> getProgramEnrollmentsForCaller(UUID programUuid, Pageable pageable);

    /**
     * {@link #search(Map, Pageable)} narrowed to the rows the current caller may see: everything for
     * a platform admin, otherwise rows of programs whose roster they may read plus their own rows.
     */
    Page<ProgramEnrollmentDTO> searchForCaller(Map<String, String> searchParams, Pageable pageable);
}