package apps.sarafrika.elimika.shared.otp;

import java.time.OffsetDateTime;

/** Where a code went and when it stops working; never the code itself. */
public record IssuedCode(String sentTo, OffsetDateTime expiresAt, OffsetDateTime resendAvailableAt) {
}
