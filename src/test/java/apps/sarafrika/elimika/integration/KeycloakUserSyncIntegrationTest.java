package apps.sarafrika.elimika.integration;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to investigate Keycloak user retrieval and database user creation.
 * This test helps understand:
 * 1. Why getUserById logs are not appearing
 * 2. Why users are not being created in the database
 * 3. The actual behavior of KeycloakUserService
 */
@SpringBootTest
@ActiveProfiles("dev")
@Slf4j
@DisplayName("Keycloak User Synchronization Integration Test")
class KeycloakUserSyncIntegrationTest {

    @Autowired
    private KeycloakUserService keycloakUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.keycloak.realm}")
    private String realm;

    // You'll need to replace this with an actual Keycloak user ID from your dev environment
    private static final String TEST_KEYCLOAK_USER_ID = "your-test-user-id-here";

    @BeforeEach
    void setUp() {
        log.info("=".repeat(80));
        log.info("Starting Keycloak User Sync Integration Test");
        log.info("Realm: {}", realm);
        log.info("=".repeat(80));
    }

    @Test
    @DisplayName("Step 1: Test Keycloak getUserById - Check if logs appear")
    void testGetUserByIdFromKeycloak() {
        log.info("\n>>> STEP 1: Testing KeycloakUserService.getUserById()");
        log.info("Attempting to fetch user from Keycloak with ID: {}", TEST_KEYCLOAK_USER_ID);

        try {
            // This should trigger the log in KeycloakUserServiceImpl line 73
            Optional<UserRepresentation> userRepOptional = keycloakUserService.getUserById(TEST_KEYCLOAK_USER_ID, realm);

            if (userRepOptional.isPresent()) {
                UserRepresentation userRep = userRepOptional.get();
                log.info("✅ SUCCESS: User retrieved from Keycloak");
                log.info("User ID: {}", userRep.getId());
                log.info("Username: {}", userRep.getUsername());
                log.info("Email: {}", userRep.getEmail());
                log.info("First Name: {}", userRep.getFirstName());
                log.info("Last Name: {}", userRep.getLastName());
                log.info("Enabled: {}", userRep.isEnabled());
                log.info("Email Verified: {}", userRep.isEmailVerified());
                log.info("Attributes: {}", userRep.getAttributes());

                assertNotNull(userRep.getId());
                assertNotNull(userRep.getUsername());
            } else {
                log.warn("⚠️  WARNING: User not found in Keycloak with ID: {}", TEST_KEYCLOAK_USER_ID);
                log.warn("This could mean:");
                log.warn("1. The user ID is incorrect");
                log.warn("2. Keycloak is not running or not accessible");
                log.warn("3. The realm configuration is wrong");
                fail("User not found in Keycloak - check logs above for details");
            }
        } catch (Exception e) {
            log.error("❌ ERROR: Exception while fetching user from Keycloak", e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("This suggests a Keycloak connectivity or configuration issue");
            throw e;
        }
    }

    @Test
    @DisplayName("Step 2: Test database user creation from Keycloak UserRepresentation")
    @Transactional
    void testCreateUserInDatabaseFromKeycloak() {
        log.info("\n>>> STEP 2: Testing UserService.createUser() with Keycloak data");

        // First, get the user from Keycloak
        log.info("Fetching user from Keycloak...");
        Optional<UserRepresentation> userRepOptional = keycloakUserService.getUserById(TEST_KEYCLOAK_USER_ID, realm);

        if (userRepOptional.isEmpty()) {
            log.warn("⚠️  Skipping test - user not found in Keycloak");
            return;
        }

        UserRepresentation userRep = userRepOptional.get();
        log.info("User retrieved: {}", userRep.getUsername());

        // Check if user already exists in database
        boolean userExists = userRepository.existsByKeycloakId(TEST_KEYCLOAK_USER_ID);
        log.info("User exists in database before creation: {}", userExists);

        if (userExists) {
            log.info("User already exists, deleting for clean test...");
            Optional<User> existingUser = userRepository.findByKeycloakId(TEST_KEYCLOAK_USER_ID);
            existingUser.ifPresent(user -> {
                log.info("Deleting existing user with UUID: {}", user.getUuid());
                userRepository.delete(user);
                userRepository.flush();
            });
        }

        // Now create the user
        log.info("Creating user in database...");
        try {
            userService.createUser(userRep);
            log.info("✅ UserService.createUser() executed without throwing exceptions");

            // Verify the user was created
            boolean userExistsAfterCreation = userRepository.existsByKeycloakId(TEST_KEYCLOAK_USER_ID);
            log.info("User exists in database after creation: {}", userExistsAfterCreation);

            if (userExistsAfterCreation) {
                Optional<User> createdUser = userRepository.findByKeycloakId(TEST_KEYCLOAK_USER_ID);
                if (createdUser.isPresent()) {
                    User user = createdUser.get();
                    log.info("✅ SUCCESS: User created successfully in database");
                    log.info("Database User UUID: {}", user.getUuid());
                    log.info("Database User Keycloak ID: {}", user.getKeycloakId());
                    log.info("Database User Username: {}", user.getUsername());
                    log.info("Database User Email: {}", user.getEmail());
                    log.info("Database User Active: {}", user.getActive());

                    // Assert the user was properly created
                    assertEquals(TEST_KEYCLOAK_USER_ID, user.getKeycloakId());
                    assertEquals(userRep.getUsername(), user.getUsername());
                    assertEquals(userRep.getEmail(), user.getEmail());
                } else {
                    log.error("❌ CRITICAL: existsByKeycloakId returned true, but findByKeycloakId returned empty!");
                    fail("Inconsistent database state - user exists check passed but retrieval failed");
                }
            } else {
                log.error("❌ CRITICAL: User was NOT created in the database");
                log.error("This indicates a transaction issue or the createUser method is not persisting data");
                fail("User creation failed - user does not exist in database after UserService.createUser()");
            }
        } catch (Exception e) {
            log.error("❌ ERROR: Exception during user creation", e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            throw e;
        }
    }

    @Test
    @DisplayName("Step 3: Full synchronization workflow simulation")
    @Transactional
    void testFullUserSyncWorkflow() {
        log.info("\n>>> STEP 3: Simulating full UserSyncFilter workflow");

        String keycloakUserId = TEST_KEYCLOAK_USER_ID;

        try {
            // Step 1: Check if user exists
            log.info("Step 3.1: Checking if user exists in database");
            boolean userExists = userRepository.existsByKeycloakId(keycloakUserId);
            log.info("User exists: {}", userExists);

            if (userExists) {
                log.info("User already exists - workflow would skip creation");
                log.info("✅ Test successful - existing user detected correctly");
                return;
            }

            // Step 2: Fetch from Keycloak
            log.info("Step 3.2: User not found, fetching from Keycloak");
            Optional<UserRepresentation> userRepOptional = keycloakUserService.getUserById(keycloakUserId, realm);

            if (userRepOptional.isEmpty()) {
                log.error("❌ User not found in Keycloak - this would cause a ResourceNotFoundException");
                fail("User not found in Keycloak");
            }

            UserRepresentation userRep = userRepOptional.get();
            log.info("✅ User retrieved from Keycloak: {}", userRep.getUsername());
            log.info("Retrieved user representation: {}", userRep.toString());

            // Step 3: Create user in database
            log.info("Step 3.3: Creating user in database");
            userService.createUser(userRep);
            log.info("✅ UserService.createUser() completed");

            // Step 4: Verify creation
            log.info("Step 3.4: Verifying user was created");
            boolean verifyExists = userRepository.existsByKeycloakId(keycloakUserId);
            log.info("Verification check - user exists: {}", verifyExists);

            if (!verifyExists) {
                log.error("❌ CRITICAL FAILURE: User creation verification failed");
                log.error("This is the exact error that would occur in UserSyncFilter line 107-110");
                fail("User was not properly created in database");
            }

            log.info("✅ SUCCESS: Full workflow completed successfully");
            log.info("User has been synchronized from Keycloak to local database");

        } catch (Exception e) {
            log.error("❌ ERROR in workflow simulation", e);
            log.error("This exception would be caught in UserSyncFilter and wrapped");
            throw e;
        }
    }

    @Test
    @DisplayName("Step 4: Check transaction behavior")
    @Transactional
    void testTransactionBehavior() {
        log.info("\n>>> STEP 4: Testing transaction behavior");

        log.info("Current transaction active: {}",
                org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
        log.info("Current transaction name: {}",
                org.springframework.transaction.support.TransactionSynchronizationManager.getCurrentTransactionName());
        log.info("Transaction isolation level: {}",
                org.springframework.transaction.support.TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

        // This shows us if transactions are working properly
        assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),
                "Transaction should be active in @Transactional test method");
    }

    @Test
    @DisplayName("Step 5: Direct Keycloak connection test")
    void testDirectKeycloakConnection() {
        log.info("\n>>> STEP 5: Testing direct Keycloak connection");
        log.info("Realm: {}", realm);

        try {
            // Try to get any user to test connectivity
            log.info("Attempting to fetch user with ID: {}", TEST_KEYCLOAK_USER_ID);

            Optional<UserRepresentation> result = keycloakUserService.getUserById(TEST_KEYCLOAK_USER_ID, realm);

            if (result.isPresent()) {
                log.info("✅ Keycloak connection is working");
                log.info("Successfully retrieved user from Keycloak");
            } else {
                log.warn("⚠️  Keycloak connection works, but user not found");
                log.warn("Make sure TEST_KEYCLOAK_USER_ID is set to a valid user ID in your Keycloak realm");
            }
        } catch (Exception e) {
            log.error("❌ Keycloak connection failed", e);
            log.error("Check:");
            log.error("1. Is Keycloak running?");
            log.error("2. Is the realm name correct? Current: {}", realm);
            log.error("3. Are Keycloak connection properties configured correctly?");
            fail("Keycloak connection test failed: " + e.getMessage());
        }
    }
}