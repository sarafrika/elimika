package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorEducationDTO;
import apps.sarafrika.elimika.instructor.model.InstructorEducation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InstructorEducationFactoryTest {

    @Test
    void mapsStartYearBetweenDtoAndEntity() {
        UUID instructorUuid = UUID.randomUUID();
        InstructorEducationDTO dto = new InstructorEducationDTO(
                null,
                instructorUuid,
                "Bachelor of Science",
                "Computer Science",
                "University of Nairobi",
                2016,
                2020,
                "CERT-001",
                null,
                null,
                null,
                null
        );

        InstructorEducation entity = InstructorEducationFactory.toEntity(dto);

        assertThat(entity.getStartYear()).isEqualTo(2016);

        InstructorEducationDTO mapped = InstructorEducationFactory.toDTO(entity);

        assertThat(mapped.startYear()).isEqualTo(2016);
    }
}
