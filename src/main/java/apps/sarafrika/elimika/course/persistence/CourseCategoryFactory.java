package apps.sarafrika.elimika.course.persistence;

public class CourseCategoryFactory {

    public static CourseCategory createCourseCategory(Long courseId, Long categoryId) {
        return CourseCategory.builder()
                .courseId(courseId)
                .categoryId(categoryId)
                .build();
    }
}
