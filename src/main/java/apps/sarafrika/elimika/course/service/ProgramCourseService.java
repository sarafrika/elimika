package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ProgramCourseDTO;
import apps.sarafrika.elimika.course.dto.ProgramCourseWeightDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProgramCourseService {
    ProgramCourseDTO createProgramCourse(ProgramCourseDTO programCourseDTO);

    ProgramCourseDTO getProgramCourseByUuid(UUID uuid);

    Page<ProgramCourseDTO> getAllProgramCourses(Pageable pageable);

    ProgramCourseDTO updateProgramCourse(UUID uuid, ProgramCourseDTO programCourseDTO);

    void deleteProgramCourse(UUID uuid);

    Page<ProgramCourseDTO> search(Map<String, String> searchParams, Pageable pageable);

    /** Replaces every course's mandatory flag and weight at once; mandatory weights must total 100. */
    List<ProgramCourseDTO> updateCourseWeights(UUID programUuid, List<ProgramCourseWeightDTO> weights);

    /** Throws unless the programme's mandatory course weights total exactly 100. */
    void enforceCompleteWeights(UUID programUuid);
}