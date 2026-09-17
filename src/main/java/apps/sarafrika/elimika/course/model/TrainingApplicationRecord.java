package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;

import java.util.UUID;

/** What course and program training applications share, so their workflows can be written once. */
public interface TrainingApplicationRecord extends TrainingRateCardHolder {

    UUID getUuid();

    CourseTrainingApplicantType getApplicantType();

    UUID getApplicantUuid();

    CourseTrainingApplicationStatus getStatus();

    String getCreatedBy();
}
