package apps.sarafrika.elimika.course.domain;


import apps.sarafrika.elimika.course.api.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateCourseRequestDTO;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CourseFactory {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    public static Course create(CreateCourseRequestDTO createCourseRequestDTO) {

        return Course.builder()
                .name(createCourseRequestDTO.name())
                .code(generateCourseCode(createCourseRequestDTO.name()))
                .description(createCourseRequestDTO.description())
                .difficultyLevel(createCourseRequestDTO.difficultyLevel().name())
                .maxAge(createCourseRequestDTO.maxAge())
                .minAge(createCourseRequestDTO.minAge())
                .build();
    }

    public static void update(final Course course, UpdateCourseRequestDTO updateCourseRequestDTO) {

        course.setName(updateCourseRequestDTO.name());
        course.setMaxAge(updateCourseRequestDTO.maxAge());
        course.setMinAge(updateCourseRequestDTO.minAge());
        course.setDescription(updateCourseRequestDTO.description());
        course.setDifficultyLevel(updateCourseRequestDTO.difficultyLevel().name());
    }

    private static String generateCourseCode(final String courseName) {

        String courseNameCode = courseName.substring(0, Math.min(courseName.length(), 3)).toUpperCase();
        String dateCode = LocalDateTime.now().format(DATE_FORMATTER);
        int randomCode = RANDOM.nextInt(1000);

        return String.format("%s-%s-%03d", courseNameCode, dateCode, randomCode);
    }
}
