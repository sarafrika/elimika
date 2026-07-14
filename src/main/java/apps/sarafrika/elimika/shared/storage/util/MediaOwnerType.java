package apps.sarafrika.elimika.shared.storage.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Owner-type discriminators for {@code media_files} registry rows. Kept as string
 * constants (not an enum) because the backfill migration writes the same literals.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MediaOwnerType {

    public static final String USER_PROFILE_IMAGE = "USER_PROFILE_IMAGE";
    public static final String COURSE_THUMBNAIL = "COURSE_THUMBNAIL";
    public static final String COURSE_BANNER = "COURSE_BANNER";
    public static final String COURSE_INTRO_VIDEO = "COURSE_INTRO_VIDEO";
    public static final String LESSON_CONTENT = "LESSON_CONTENT";
    public static final String ASSIGNMENT_ATTACHMENT = "ASSIGNMENT_ATTACHMENT";
    public static final String ASSIGNMENT_SUBMISSION_ATTACHMENT = "ASSIGNMENT_SUBMISSION_ATTACHMENT";
    public static final String CERTIFICATE = "CERTIFICATE";
    public static final String CERTIFICATE_TEMPLATE_BACKGROUND = "CERTIFICATE_TEMPLATE_BACKGROUND";
    public static final String CLASS_THUMBNAIL = "CLASS_THUMBNAIL";
    public static final String CLASS_PROMO_VIDEO = "CLASS_PROMO_VIDEO";
    public static final String CLASS_RESOURCE = "CLASS_RESOURCE";
    public static final String INSTRUCTOR_DOCUMENT = "INSTRUCTOR_DOCUMENT";
    public static final String COURSE_CREATOR_DOCUMENT = "COURSE_CREATOR_DOCUMENT";
}
