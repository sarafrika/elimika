package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.spi.InstructorDTO;
import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.shared.utils.CoordinatePrecision;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorFactory {

    // Convert Instructor entity to InstructorDTO
    public static InstructorDTO toDTO(Instructor instructor) {
        if (instructor == null) {
            return null;
        }
        return toDTO(instructor, instructor.getLatitude(), instructor.getLongitude());
    }

    /**
     * The directory form of a profile: identical to {@link #toDTO(Instructor)} except that latitude and
     * longitude are rounded to town level (see {@link CoordinatePrecision}). Used for every list and
     * search response, and for single-profile reads by anyone other than the owner or a platform
     * admin.
     */
    public static InstructorDTO toPublicDTO(Instructor instructor) {
        if (instructor == null) {
            return null;
        }
        return toDTO(instructor,
                CoordinatePrecision.toPublic(instructor.getLatitude()),
                CoordinatePrecision.toPublic(instructor.getLongitude()));
    }

    private static InstructorDTO toDTO(Instructor instructor, BigDecimal latitude, BigDecimal longitude) {
        return new InstructorDTO(
                instructor.getUuid(),
                instructor.getUserUuid(),
                instructor.getFullName(),
                instructor.getLocationName(),
                latitude,
                longitude,
                instructor.getAdminVerified(),
                instructor.getWebsite(),
                instructor.getBio(),
                instructor.getProfessionalHeadline(),
                instructor.getCreatedDate(),
                instructor.getCreatedBy(),
                instructor.getLastModifiedDate(),
                instructor.getLastModifiedBy()
        );
    }

    // Convert InstructorDTO to Instructor entity
    public static Instructor toEntity(InstructorDTO dto) {
        if (dto == null) {
            return null;
        }
        Instructor instructor = new Instructor();
        instructor.setUuid(dto.uuid());
        instructor.setUserUuid(dto.userUuid());
        instructor.setFullName(dto.fullName());
        instructor.setLocationName(dto.locationName());
        instructor.setLatitude(dto.latitude());
        instructor.setLongitude(dto.longitude());
        instructor.setAdminVerified(dto.verified());
        instructor.setWebsite(dto.website());
        instructor.setBio(dto.bio());
        instructor.setProfessionalHeadline(dto.professionalHeadline());
        instructor.setCreatedDate(dto.createdDate());
        instructor.setCreatedBy(dto.createdBy());
        instructor.setLastModifiedDate(dto.updatedDate());
        instructor.setLastModifiedBy(dto.updatedBy());
        return instructor;
    }
}