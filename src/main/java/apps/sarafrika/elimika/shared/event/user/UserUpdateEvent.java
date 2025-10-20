package apps.sarafrika.elimika.shared.event.user;

import apps.sarafrika.elimika.shared.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;

public record UserUpdateEvent(
    String username, 
    String firstName, 
    String lastName, 
    String email,
    String middleName,
    String phoneNumber,
    LocalDate dob,
    Gender gender,
    String profileImageUrl,
    Boolean active, 
    String realm, 
    UUID sarafrikaCorrelationId, 
    String keyCloakId
) {
    
    /**
     * Factory method for backward compatibility with existing code.
     * Creates UserUpdateEvent with only basic fields, setting extended fields to null.
     */
    public static UserUpdateEvent createBasic(String username, String firstName, 
            String lastName, String email, Boolean active, String realm, 
            UUID correlationId, String keycloakId) {
        return new UserUpdateEvent(username, firstName, lastName, email, 
                null, null, null, null, null, active, realm, correlationId, keycloakId);
    }
    
    /**
     * Factory method to create UserUpdateEvent with all available user data.
     */
    public static UserUpdateEvent createFull(String username, String firstName, 
            String lastName, String email, String middleName, String phoneNumber,
            LocalDate dob, Gender gender, String profileImageUrl, Boolean active, 
            String realm, UUID correlationId, String keycloakId) {
        return new UserUpdateEvent(username, firstName, lastName, email, middleName,
                phoneNumber, dob, gender, profileImageUrl, active, realm, correlationId, keycloakId);
    }
}
