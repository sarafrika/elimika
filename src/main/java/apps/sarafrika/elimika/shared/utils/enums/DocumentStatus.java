package apps.sarafrika.elimika.shared.utils.enums;


import lombok.Getter;

/**
 * Enum for document status
 * Must match the database enum: document_status_enum
 */
@Getter
public enum DocumentStatus {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    EXPIRED("Expired");

    private final String displayName;

    DocumentStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Get enum from string value (case insensitive)
     */
    public static DocumentStatus fromString(String value) {
        if (value == null) {
            return null;
        }

        for (DocumentStatus status : DocumentStatus.values()) {
            if (status.name().equalsIgnoreCase(value) ||
                    status.displayName.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("No enum constant for value: " + value);
    }

    /**
     * Check if status allows modifications
     */
    public boolean isModifiable() {
        return this == PENDING;
    }

    /**
     * Check if status is final (cannot be changed)
     */
    public boolean isFinal() {
        return this == APPROVED || this == EXPIRED;
    }
}