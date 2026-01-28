package apps.sarafrika.elimika.commerce.purchase.service.impl;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class DevCommerceAccessServiceImplTest {

    private final DevCommerceAccessServiceImpl service = new DevCommerceAccessServiceImpl();

    @Test
    void shouldAlwaysReturnTrue() {
        UUID studentUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        assertThat(service.hasCourseAccess(studentUuid, courseUuid)).isTrue();
        assertThat(service.hasClassAccess(studentUuid, classDefinitionUuid)).isTrue();
    }
}
