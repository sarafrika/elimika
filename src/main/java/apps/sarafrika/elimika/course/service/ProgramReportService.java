package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ClassProgrammeProgressDTO;
import apps.sarafrika.elimika.course.dto.ProgramRemarkDTO;
import apps.sarafrika.elimika.course.dto.ProgramReportDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Live programme reports, instructor remarks and class-level programme progress. */
public interface ProgramReportService {

    /** The learner's report; attendance counts only sessions inside the optional date range. */
    ProgramReportDTO getReport(UUID programUuid, UUID studentUuid, LocalDate from, LocalDate to);

    List<ProgramRemarkDTO> getRemarks(UUID programUuid, UUID studentUuid);

    ProgramRemarkDTO addRemark(UUID programUuid, UUID studentUuid, ProgramRemarkDTO remark, UUID authorUserUuid);

    /** Marks and skill levels for the class's learners; programUuid picks the programme when the class doesn't name one. */
    ClassProgrammeProgressDTO getClassProgress(UUID classDefinitionUuid, UUID programUuid);
}
