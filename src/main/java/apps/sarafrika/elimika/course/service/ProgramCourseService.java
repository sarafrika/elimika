package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ProgramCourseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ProgramCourseService {
    ProgramCourseDTO createProgramCourse(ProgramCourseDTO programCourseDTO);

    ProgramCourseDTO getProgramCourseByUuid(UUID uuid);

    Page<ProgramCourseDTO> getAllProgramCourses(Pageable pageable);

    ProgramCourseDTO updateProgramCourse(UUID uuid, ProgramCourseDTO programCourseDTO);

    void deleteProgramCourse(UUID uuid);

    Page<ProgramCourseDTO> search(Map<String, String> searchParams, Pageable pageable);
}