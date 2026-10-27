package apps.sarafrika.elimika.skills.integration;

import apps.sarafrika.elimika.skills.dto.SkillLevelDTO;
import apps.sarafrika.elimika.skills.internal.SkillLevelService;
import apps.sarafrika.elimika.skills.spi.SkillLevelsChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@RecordApplicationEvents
@Import({SkillLevelService.class, SkillLevelScaleIntegrationTest.TestConfig.class})
@DisplayName("Platform skill level scale")
class SkillLevelScaleIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }
    }

    @Autowired private SkillLevelService service;
    @Autowired private ApplicationEvents events;

    @Test
    @DisplayName("the seeded scale is Not achieved 0 / Developing 50 / Achieved 70")
    void seededDefaults() {
        assertThat(service.list()).extracting(SkillLevelDTO::name).containsExactly("Not achieved", "Developing", "Achieved");
        assertThat(service.levelFor(new BigDecimal("69.99")).name()).isEqualTo("Developing");
        assertThat(service.levelFor(new BigDecimal("70")).countsAsAchieved()).isTrue();
        assertThat(service.levelFor(BigDecimal.ZERO).rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("replacing with the 4-level scale re-ranks levels and announces the change")
    void replaceWithFourLevels() {
        service.replace(List.of(
                level("Advanced", "85", true), level("Foundation", "0", false),
                level("Developing", "50", false), level("Proficient", "70", true)));

        assertThat(service.list()).extracting(SkillLevelDTO::name)
                .containsExactly("Foundation", "Developing", "Proficient", "Advanced");
        assertThat(service.levelFor(new BigDecimal("90")).rank()).isEqualTo(4);
        assertThat(events.stream(SkillLevelsChangedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("rejects a scale without a 0% level, duplicate minimums, broken achieved band or no achieved level")
    void validation() {
        assertThatThrownBy(() -> service.replace(List.of(level("Low", "10", false), level("High", "70", true))))
                .hasMessageContaining("0%");
        assertThatThrownBy(() -> service.replace(List.of(level("A", "0", false), level("B", "50.00", true), level("C", "50", true))))
                .hasMessageContaining("share the minimum");
        assertThatThrownBy(() -> service.replace(List.of(level("A", "0", false), level("B", "50", true), level("C", "70", false))))
                .hasMessageContaining("top levels");
        assertThatThrownBy(() -> service.replace(List.of(level("A", "0", false), level("B", "50", false))))
                .hasMessageContaining("at least one");
        assertThatThrownBy(() -> service.replace(List.of(level("Same", "0", false), level("same", "70", true))))
                .hasMessageContaining("unique");
    }

    private static SkillLevelDTO level(String name, String min, boolean achieved) {
        return new SkillLevelDTO(null, name, new BigDecimal(min), achieved);
    }
}
