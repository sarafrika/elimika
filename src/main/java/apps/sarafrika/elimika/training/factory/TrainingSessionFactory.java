package apps.sarafrika.elimika.training.factory;

import apps.sarafrika.elimika.training.dto.TrainingSessionDTO;
import apps.sarafrika.elimika.training.model.TrainingSession;

public abstract class TrainingSessionFactory {
    public static TrainingSession toEntity(TrainingSessionDTO dto) {
        if(dto == null){
            return null;
        }

        TrainingSession entity = new TrainingSession();
        entity.setUuid(dto.uuid());
        entity.setCourseUuid(dto.courseUuid());
        entity.setTraineruuid(dto.trainerUuid());
        entity.setStartDate(dto.startDate());
        entity.setEndDate(dto.endDate());
        entity.setClassMode(TrainingSession.ClassMode.valueOf(dto.classMode()));
        entity.setLocation(dto.location());
        entity.setMeetingLink(dto.meetingLink());
        entity.setSchedule(dto.schedule());
        entity.setCapacityLimit(dto.capacityLimit());
        entity.setCurrentEnrollmentCount(dto.currentEnrollmentCount());
        entity.setWaitingListCount(dto.waitingListCount());
        entity.setGroupOrIndividual(TrainingSession.GroupType.valueOf(dto.groupOrIndividual()));
        return entity;
    }

    public static TrainingSessionDTO toDTO(TrainingSession entity) {
        if(entity == null){
            return null;
        }

        return new TrainingSessionDTO(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getTraineruuid(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getClassMode().name(),
                entity.getLocation(),
                entity.getMeetingLink(),
                entity.getSchedule(),
                entity.getCapacityLimit(),
                entity.getCurrentEnrollmentCount(),
                entity.getWaitingListCount(),
                entity.getGroupOrIndividual().name()
        );
    }
}
