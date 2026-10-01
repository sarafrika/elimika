package apps.sarafrika.elimika.shared.web;

import apps.sarafrika.elimika.shared.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RemovedTextOperatorInterceptor")
class RemovedTextOperatorInterceptorTest {

    private MockMvc mockMvc;

    /** Binds only q and paging, like {@code GET /api/v1/courses}: any other key used to be ignored. */
    @RestController
    static class ListingController {
        @GetMapping("/api/v1/things")
        String list(@RequestParam(value = "q", required = false) String q, Pageable pageable) {
            return "ok";
        }

        @PostMapping("/api/v1/things")
        String create() {
            return "created";
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ListingController())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addMappedInterceptors(new String[]{"/api/**"}, new RemovedTextOperatorInterceptor())
                .build();
    }

    @Test
    @DisplayName("A listing that never reads the key still rejects name_like with the documented 400")
    void rejectsLikeOnAListingThatIgnoresUnknownKeys() throws Exception {
        mockMvc.perform(get("/api/v1/things").param("name_like", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Text operators were removed; use the q parameter for text search (rejected: name_like)"));
    }

    @Test
    @DisplayName("_startswith and _endswith are rejected too, case-insensitively and alongside q")
    void rejectsStartsWithAndEndsWith() throws Exception {
        mockMvc.perform(get("/api/v1/things").param("title_startswith", "x"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/things").param("q", "python").param("title_EndsWith", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ordinary keys and q still pass")
    void ordinaryKeysPass() throws Exception {
        mockMvc.perform(get("/api/v1/things").param("q", "python").param("status_eq", "published")
                        .param("liked", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Only reads are checked")
    void writesAreNotChecked() throws Exception {
        mockMvc.perform(post("/api/v1/things").param("name_like", "x"))
                .andExpect(status().isOk());
    }
}
