package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;

import java.time.LocalDateTime;

/** A profile item a platform admin verifies; the verdict holds for every domain of its owner. */
public interface VerifiableItem {

    WalletVerificationStatus getVerificationStatus();

    void setVerificationStatus(WalletVerificationStatus status);

    void setVerifiedAt(LocalDateTime verifiedAt);

    void setVerificationNotes(String notes);

    /** A changed claim has not been checked, so it goes back to PENDING. */
    default void resetVerification() {
        setVerificationStatus(WalletVerificationStatus.PENDING);
        setVerifiedAt(null);
        setVerificationNotes(null);
    }

    default void recordVerdict(WalletVerificationStatus status, String notes, LocalDateTime at) {
        setVerificationStatus(status);
        setVerifiedAt(at);
        setVerificationNotes(notes);
    }
}
