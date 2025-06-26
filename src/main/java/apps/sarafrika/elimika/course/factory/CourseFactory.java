package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.model.Course;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Course Factory
 * <p>
 * Factory class responsible for converting between Course entities and CourseDTO objects.
 * Provides centralized conversion logic to ensure consistency across the application.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since Thursday, June 26, 2025
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseFactory {

    public static CourseDTO toDTO(Course course) {
        if (course == null) {
            return null;
        }

        return new CourseDTO(
                course.getUuid(),
                course.getCourseCode(),
                course.getCourseName(),
                course.getCourseDescription(),
                course.getCourseThumbnail(),
                course.getInitialPrice(),
                course.getCurrentPrice(),
                course.getAccessStartDate(),
                course.getClassLimit(),
                course.getAgeUpperLimit(),
                course.getAgeLowerLimit(),
                course.getDifficulty(),
                course.getCourseObjectives(),
                course.getCourseStatus(),
                course.getCreatedDate(),
                course.getLastModifiedDate(),
                course.getCreatedBy(),
                course.getLastModifiedBy()
        );
    }

    public static Course toEntity(CourseDTO courseDTO) {
        if (courseDTO == null) {
            return null;
        }

        Course course = new Course();
        course.setCourseCode(courseDTO.courseCode());
        course.setCourseName(courseDTO.courseName());
        course.setCourseDescription(courseDTO.courseDescription());
        course.setInitialPrice(courseDTO.initialPrice());
        course.setCurrentPrice(courseDTO.currentPrice());
        course.setAccessStartDate(courseDTO.accessStartDate());
        course.setClassLimit(courseDTO.classLimit());
        course.setAgeUpperLimit(courseDTO.ageUpperLimit());
        course.setAgeLowerLimit(courseDTO.ageLowerLimit());
        course.setDifficulty(courseDTO.difficulty());
        course.setCourseObjectives(courseDTO.courseObjectives());
        course.setCourseStatus(courseDTO.courseStatus());

        return course;
    }
}