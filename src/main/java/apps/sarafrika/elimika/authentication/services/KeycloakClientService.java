package apps.sarafrika.elimika.authentication.services;

public interface KeycloakClientService {
    String createClient(String realm, String clientName);

    void deleteClient(String realm, String clientId);

    boolean clientExists(String realm, String clientId);
}
