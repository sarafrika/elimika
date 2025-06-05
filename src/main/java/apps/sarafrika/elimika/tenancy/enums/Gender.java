package apps.sarafrika.elimika.tenancy.enums;

public enum Gender {
    MALE("Male"),
    FEMALE("Female"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Gender fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        for (Gender gender : Gender.values()) {
            if (gender.name().equalsIgnoreCase(text.trim()) ||
                    gender.displayName.equalsIgnoreCase(text.trim())) {
                return gender;
            }
        }

        // Handle common variations
        String normalized = text.trim().toLowerCase();
        return switch (normalized) {
            case "m", "man" -> MALE;
            case "f", "woman" -> FEMALE;
            case "prefer not to say", "not specified", "other", "none" -> PREFER_NOT_TO_SAY;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
