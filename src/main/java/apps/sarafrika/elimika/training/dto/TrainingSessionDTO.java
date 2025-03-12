package apps.sarafrika.elimika.training.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.ZonedDateTime;
import java.util.UUID;

public record TrainingSessionDTO (
    @JsonProperty("uuid")
    UUID uuid,

    @NotNull(message = "course_uuid cannot be null")
    @JsonProperty("course_uuid")
    UUID courseUuid,

    @NotNull(message = "trainer_uuid cannot be null")
    @JsonProperty("trainer_uuid")
    UUID trainerUuid,

    @NotNull(message = "start_date cannot be null")
    @FutureOrPresent(message = "start_date must be in the present or future")
    @JsonProperty("start_date")
    ZonedDateTime startDate,

    @NotNull(message = "end_date cannot be null")
    @Future(message = "end_date must be in the future")
    @JsonProperty("end_date")
    ZonedDateTime endDate,

    @NotNull(message = "class_mode cannot be null")
    @Pattern(regexp = "ONLINE|IN_PERSON", message = "class_mode must be either ONLINE or IN_PERSON")
    @JsonProperty("class_mode")
    String classMode,

    @JsonProperty("location")
    String location,

    @JsonProperty("meeting_link")
    String meetingLink,

    @JsonProperty("schedule")
    String schedule,

    @Min(value = 1, message = "capacity_limit must be at least 1")
    @JsonProperty("capacity_limit")
    int capacityLimit,

    @Min(value = 0, message = "current_enrollment_count cannot be negative")
    @JsonProperty("current_enrollment_count")
    int currentEnrollmentCount,

    @Min(value = 0, message = "waiting_list_count cannot be negative")
    @JsonProperty("waiting_list_count")
    int waitingListCount,

    @NotNull(message = "group_or_individual cannot be null")
    @Pattern(regexp = "GROUP|INDIVIDUAL", message = "group_or_individual must be either GROUP or INDIVIDUAL")
    @JsonProperty("group_or_individual")
    String groupOrIndividual
){}
