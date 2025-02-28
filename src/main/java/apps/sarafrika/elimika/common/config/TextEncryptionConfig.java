package apps.sarafrika.elimika.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class TextEncryptionConfig {
    @Value("${encryption.secret-key}")
    private String ENCRYPTION_PASSWORD;
    @Value("${encryption.salt}")
    private String SALT;

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(ENCRYPTION_PASSWORD, SALT);
    }
}
