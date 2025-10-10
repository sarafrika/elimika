package apps.sarafrika.elimika.shared.tracking.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RequestUserMetadata {

    private UUID userUuid;
    private String email;
    private String fullName;
    private String keycloakId;
    private List<String> domains;
    private String authenticationName;

    public static RequestUserMetadata anonymous(String authenticationName) {
        return new RequestUserMetadata(null, null, null, null, Collections.emptyList(), authenticationName);
    }

    public boolean isAnonymous() {
        return userUuid == null;
    }
}
