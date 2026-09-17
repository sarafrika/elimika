package apps.sarafrika.elimika.timetabling.service;

import apps.sarafrika.elimika.timetabling.dto.InstructorClassOptionDTO;
import apps.sarafrika.elimika.timetabling.dto.InstructorStudentDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface InstructorStudentRosterService {

    /** The students an instructor teaches in an organisation's classes, readable by that organisation's managers. */
    InstructorStudentRoster listInstructorStudents(UUID organisationUuid,
                                                   UUID instructorUuid,
                                                   String search,
                                                   UUID classDefinitionUuid,
                                                   int page,
                                                   int size);

    record InstructorStudentRoster(Page<InstructorStudentDTO> students,
                                   List<InstructorClassOptionDTO> classOptions,
                                   long studentCount) {
    }
}
