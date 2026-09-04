package apps.sarafrika.elimika.student.service;

import apps.sarafrika.elimika.student.dto.StudentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

/**
 * The read model behind {@code /api/v1/students}: student records as the current caller is entitled
 * to see them.
 * <p>
 * {@link StudentService} answers what the database holds and is deliberately unaware of who is
 * asking. Something still has to decide whether this caller gets a learner's guardian contacts or
 * only their display identity, and that decision is neither a controller's job nor a CRUD service's
 * — so it lives here, in one entry point per route. The controller calls exactly one method and
 * chooses nothing.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
public interface StudentDirectoryService {

    /**
     * A single student record, whole or reduced to display identity depending on the caller's
     * relationship to the learner.
     *
     * @param uuid the student profile identifier
     * @return the record as this caller may see it
     */
    StudentDTO getStudent(UUID uuid);

    /**
     * A page of the student directory, each record served at the level the caller has earned.
     *
     * @param pageable pagination details
     * @return the page as this caller may see it
     */
    Page<StudentDTO> listStudents(Pageable pageable);

    /**
     * A page of search results, each record served at the level the caller has earned.
     *
     * @param searchParams search parameters as key-value pairs
     * @param pageable     pagination details
     * @return the matching page as this caller may see it
     */
    Page<StudentDTO> searchStudents(Map<String, String> searchParams, Pageable pageable);
}
