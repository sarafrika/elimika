package apps.sarafrika.elimika;

import org.springframework.modulith.core.ApplicationModules;

/**
 * Standalone checker to list all Spring Modulith violations.
 */
public class ModulithViolationsChecker {

    public static void main(String[] args) {
        try {
            System.out.println("Verifying Spring Modulith structure...\n");
            ApplicationModules modules = ApplicationModules.of(ElimikaApplication.class);
            modules.verify();
            System.out.println("✓ No violations found!");
        } catch (Exception e) {
            System.err.println("\n========== SPRING MODULITH VIOLATIONS ==========");
            System.err.println(e.getMessage());
            System.err.println("===============================================\n");
            System.exit(1);
        }
    }
}
