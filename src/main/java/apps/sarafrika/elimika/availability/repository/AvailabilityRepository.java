package apps.sarafrika.elimika.availability.repository;

import apps.sarafrika.elimika.availability.model.InstructorAvailability;
import apps.sarafrika.elimika.availability.util.enums.AvailabilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvailabilityRepository extends JpaRepository<InstructorAvailability, Long>, JpaSpecificationExecutor<InstructorAvailability> {

    Optional<InstructorAvailability> findByUuid(UUID uuid);

    List<InstructorAvailability> findByInstructorUuid(UUID instructorUuid);

    List<InstructorAvailability> findByInstructorUuidAndAvailabilityType(UUID instructorUuid, AvailabilityType availabilityType);

    List<InstructorAvailability> findByInstructorUuidAndIsAvailable(UUID instructorUuid, Boolean isAvailable);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid AND ia.isAvailable = true")
    List<InstructorAvailability> findAvailableSlotsByInstructor(@Param("instructorUuid") UUID instructorUuid);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid AND ia.isAvailable = false")
    List<InstructorAvailability> findBlockedSlotsByInstructor(@Param("instructorUuid") UUID instructorUuid);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid " +
           "AND ia.availabilityType = :availabilityType AND ia.dayOfWeek = :dayOfWeek")
    List<InstructorAvailability> findWeeklyAvailabilityByDay(@Param("instructorUuid") UUID instructorUuid,
                                                           @Param("availabilityType") AvailabilityType availabilityType,
                                                           @Param("dayOfWeek") Integer dayOfWeek);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid " +
           "AND ia.availabilityType = :availabilityType AND ia.dayOfMonth = :dayOfMonth")
    List<InstructorAvailability> findMonthlyAvailabilityByDay(@Param("instructorUuid") UUID instructorUuid,
                                                            @Param("availabilityType") AvailabilityType availabilityType,
                                                            @Param("dayOfMonth") Integer dayOfMonth);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid " +
           "AND ia.specificDate = :specificDate")
    List<InstructorAvailability> findByInstructorAndSpecificDate(@Param("instructorUuid") UUID instructorUuid,
                                                               @Param("specificDate") LocalDate specificDate);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid " +
           "AND (ia.effectiveStartDate IS NULL OR ia.effectiveStartDate <= :date) " +
           "AND (ia.effectiveEndDate IS NULL OR ia.effectiveEndDate >= :date)")
    List<InstructorAvailability> findEffectiveAvailabilityForDate(@Param("instructorUuid") UUID instructorUuid,
                                                                @Param("date") LocalDate date);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid " +
           "AND ia.startTime <= :endTime AND ia.endTime >= :startTime " +
           "AND (ia.effectiveStartDate IS NULL OR ia.effectiveStartDate <= :date) " +
           "AND (ia.effectiveEndDate IS NULL OR ia.effectiveEndDate >= :date)")
    List<InstructorAvailability> findOverlappingAvailability(@Param("instructorUuid") UUID instructorUuid,
                                                           @Param("startTime") LocalTime startTime,
                                                           @Param("endTime") LocalTime endTime,
                                                           @Param("date") LocalDate date);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid " +
           "AND ia.availabilityType = 'weekly' AND ia.dayOfWeek = :dayOfWeek " +
           "AND ia.isAvailable = true " +
           "AND (ia.effectiveStartDate IS NULL OR ia.effectiveStartDate <= :date) " +
           "AND (ia.effectiveEndDate IS NULL OR ia.effectiveEndDate >= :date)")
    List<InstructorAvailability> findWeeklyAvailabilityForDate(@Param("instructorUuid") UUID instructorUuid,
                                                             @Param("dayOfWeek") Integer dayOfWeek,
                                                             @Param("date") LocalDate date);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid " +
           "AND ia.availabilityType = 'monthly' AND ia.dayOfMonth = :dayOfMonth " +
           "AND ia.isAvailable = true " +
           "AND (ia.effectiveStartDate IS NULL OR ia.effectiveStartDate <= :date) " +
           "AND (ia.effectiveEndDate IS NULL OR ia.effectiveEndDate >= :date)")
    List<InstructorAvailability> findMonthlyAvailabilityForDate(@Param("instructorUuid") UUID instructorUuid,
                                                              @Param("dayOfMonth") Integer dayOfMonth,
                                                              @Param("date") LocalDate date);

    @Query("SELECT ia FROM InstructorAvailability ia WHERE ia.instructorUuid = :instructorUuid " +
           "AND ia.availabilityType = 'daily' " +
           "AND ia.isAvailable = true " +
           "AND (ia.effectiveStartDate IS NULL OR ia.effectiveStartDate <= :date) " +
           "AND (ia.effectiveEndDate IS NULL OR ia.effectiveEndDate >= :date)")
    List<InstructorAvailability> findDailyAvailabilityForDate(@Param("instructorUuid") UUID instructorUuid,
                                                            @Param("date") LocalDate date);
}