package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.model.CourseCategory;

public class CourseCategoryFactory {

    public static CourseCategory createCourseCategory(Long courseId, Long categoryId) {
        return CourseCategory.builder()
                .courseId(courseId)
                .categoryId(categoryId)
                .build();
    }
}
