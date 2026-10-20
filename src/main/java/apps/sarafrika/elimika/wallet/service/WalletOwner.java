package apps.sarafrika.elimika.wallet.service;

import java.util.Objects;
import java.util.UUID;

/** The party a wallet belongs to: a user, or an organisation that earns as the delivery party. */
public record WalletOwner(WalletOwnerType type, UUID uuid) {

    public WalletOwner {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(uuid, "uuid is required");
    }

    public static WalletOwner user(UUID userUuid) {
        return new WalletOwner(WalletOwnerType.USER, userUuid);
    }

    public static WalletOwner organisation(UUID organisationUuid) {
        return new WalletOwner(WalletOwnerType.ORGANISATION, organisationUuid);
    }
}
