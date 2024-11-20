package apps.sarafrika.elimika;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    static ApplicationModules modules = ApplicationModules.of(ElimikaApplication.class);


    @Test
    void verifiesModularStructure() {

        modules.verify();
    }
}
