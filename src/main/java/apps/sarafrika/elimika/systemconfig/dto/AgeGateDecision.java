package apps.sarafrika.elimika.systemconfig.dto;

public record AgeGateDecision(boolean allowed, String reason) {

    public static AgeGateDecision allow() {
        return new AgeGateDecision(true, "Allowed");
    }

    public static AgeGateDecision rejected(String reason) {
        return new AgeGateDecision(false, reason);
    }
}
