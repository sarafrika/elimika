package apps.sarafrika.elimika.timetabling.dto;

import apps.sarafrika.elimika.shared.utils.PageLinks;
import apps.sarafrika.elimika.shared.utils.PageMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(name = "InstructorStudentPage", description = "A page of an instructor's students plus the classes to filter them by")
public record InstructorStudentPageDTO(

        @JsonProperty(value = "content", access = JsonProperty.Access.READ_ONLY)
        List<InstructorStudentDTO> content,

        @JsonProperty(value = "metadata", access = JsonProperty.Access.READ_ONLY)
        PageMetadata metadata,

        @JsonProperty(value = "links", access = JsonProperty.Access.READ_ONLY)
        PageLinks links,

        @Schema(description = "Every class of the organisation's the instructor has students in, whatever the filters")
        @JsonProperty(value = "class_options", access = JsonProperty.Access.READ_ONLY)
        List<InstructorClassOptionDTO> classOptions,

        @Schema(description = "Distinct students across every class in class_options, whatever the filters; "
                + "metadata.totalElements counts student-per-class rows instead")
        @JsonProperty(value = "student_count", access = JsonProperty.Access.READ_ONLY)
        long studentCount
) {

    public static InstructorStudentPageDTO from(Page<InstructorStudentDTO> page,
                                                List<InstructorClassOptionDTO> classOptions,
                                                long studentCount,
                                                String baseUrl) {
        return new InstructorStudentPageDTO(page.getContent(), PageMetadata.from(page),
                PageLinks.from(page, baseUrl), classOptions, studentCount);
    }
}
