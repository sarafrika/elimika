package apps.sarafrika.elimika;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Test to verify Spring Modulith architecture compliance.
 * This test ensures all modules follow Spring Modulith principles including:
 * - Proper module boundaries and encapsulation
 * - Named interface usage for cross-module dependencies
 * - No illegal direct dependencies between modules
 * - Proper use of SPI (Service Provider Interface) pattern
 *
 * <p>If this test fails, it indicates architectural violations that must be fixed
 * by either:
 * 1. Adding the dependency to allowedDependencies in package-info.java
 * 2. Exposing functionality via a named interface (e.g., "spi")
 * 3. Refactoring to avoid the cross-module dependency
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-20
 */
class ModulithArchitectureTest {

    /**
     * Main verification test that checks all Spring Modulith rules.
     * This will fail with detailed error messages if any violations are detected.
     */
    @Test
    void verifyModularStructure() {
        try {
            ApplicationModules.of(ElimikaApplication.class).verify();
        } catch (Exception e) {
            System.err.println("\n========== SPRING MODULITH VIOLATIONS ==========");
            System.err.println(e.getMessage());
            System.err.println("===============================================\n");
            throw e;
        }
    }

    /**
     * Test to generate module documentation with verification.
     * Creates visual documentation of the module structure using PlantUML.
     * Also verifies the architecture before generating documentation.
     */
    @Test
    void generateModuleDocumentation() {
        ApplicationModules modules = ApplicationModules.of(ElimikaApplication.class).verify();

        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();

        System.out.println("Module documentation generated in target/spring-modulith-docs/");
    }
}
