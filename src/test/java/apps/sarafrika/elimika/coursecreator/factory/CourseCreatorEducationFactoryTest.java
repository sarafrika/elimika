package apps.sarafrika.elimika.coursecreator.factory;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorEducationDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorEducation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseCreatorEducationFactoryTest {

    @Test
    void mapsStartYearBetweenDtoAndEntity() {
        UUID courseCreatorUuid = UUID.randomUUID();
        CourseCreatorEducationDTO dto = new CourseCreatorEducationDTO(
                null,
                courseCreatorUuid,
                "Master of Education",
                "Curriculum Studies",
                "Strathmore University",
                2019,
                2021,
                "CERT-001",
                null,
                null,
                null,
                null
        );

        CourseCreatorEducation entity = CourseCreatorEducationFactory.toEntity(dto);

        assertThat(entity.getStartYear()).isEqualTo(2019);

        CourseCreatorEducationDTO mapped = CourseCreatorEducationFactory.toDTO(entity);

        assertThat(mapped.startYear()).isEqualTo(2019);
    }
}
