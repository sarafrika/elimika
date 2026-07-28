package apps.sarafrika.elimika.tenancy.internal;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitationTokenServiceTest {

    private final InvitationTokenService tokenService = new InvitationTokenService();

    @Test
    void generatesDistinctUrlSafeTokens() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            tokens.add(tokenService.generateRawToken());
        }

        assertThat(tokens).hasSize(500);
        assertThat(tokens).allSatisfy(token ->
                assertThat(token).matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void hashingIsStableForTheSameToken() {
        String token = tokenService.generateRawToken();

        assertThat(tokenService.hash(token)).isEqualTo(tokenService.hash(token));
    }

    @Test
    void hashingIsInsensitiveToSurroundingWhitespace() {
        String token = tokenService.generateRawToken();

        assertThat(tokenService.hash("  " + token + "  ")).isEqualTo(tokenService.hash(token));
    }

    @Test
    void differentTokensHashDifferently() {
        String first = tokenService.generateRawToken();
        String second = tokenService.generateRawToken();

        assertThat(tokenService.hash(first)).isNotEqualTo(tokenService.hash(second));
    }

    @Test
    void hashNeverRevealsTheRawToken() {
        String token = tokenService.generateRawToken();

        String hash = tokenService.hash(token);

        assertThat(hash).doesNotContain(token).hasSize(64);
    }

    @Test
    void rejectsMissingToken() {
        assertThatThrownBy(() -> tokenService.hash(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokenService.hash("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
