package apps.sarafrika.elimika.search.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apps.sarafrika.elimika.search.internal.global.GlobalSearchService;
import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("Global search controller when search cannot answer")
class GlobalSearchControllerTest {

    private final SearchGateway gateway = mock(SearchGateway.class);
    private final SearchAvailability availability = mock(SearchAvailability.class);
    private MockMvc mockMvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        GlobalSearchProvider courses = mock(GlobalSearchProvider.class);
        SearchIndexDefinition definition = SearchIndexDefinition.of("courses", 1, List.of("name"), List.of("status"), List.of());
        when(courses.type()).thenReturn("courses");
        when(courses.definition()).thenReturn(definition);
        when(courses.index()).thenReturn("courses");
        when(courses.scopeForCurrentCaller()).thenReturn(Optional.of(SearchScope.unrestricted("test")));
        ObjectProvider<GlobalSearchProvider> providers = mock(ObjectProvider.class);
        when(providers.orderedStream()).thenAnswer(invocation -> Stream.of(courses));
        GlobalSearchService service = new GlobalSearchService(providers, gateway, availability);
        mockMvc = MockMvcBuilders.standaloneSetup(new GlobalSearchController(service)).build();
    }

    @Test
    @DisplayName("Search disabled answers 503 with an ApiResponse error, without calling the engine")
    void disabledIs503() throws Exception {
        when(availability.isEnabled()).thenReturn(false);

        mockMvc.perform(get("/api/v1/search").param("q", "astronomy"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Search is unavailable"));
        mockMvc.perform(get("/api/v1/search/courses").param("q", "astronomy"))
                .andExpect(status().isServiceUnavailable());
        verify(gateway, never()).multiSearchPerIndex(any());
    }

    @Test
    @DisplayName("Search enabled but no requested type read-enabled answers 503")
    void noReadEnabledTypeIs503() throws Exception {
        when(availability.isEnabled()).thenReturn(true);
        when(availability.isReadEnabled("courses")).thenReturn(false);

        mockMvc.perform(get("/api/v1/search").param("q", "astronomy"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("An engine failure answers 503")
    void engineDownIs503() throws Exception {
        when(availability.isEnabled()).thenReturn(true);
        when(availability.isReadEnabled("courses")).thenReturn(true);
        when(gateway.multiSearchPerIndex(any())).thenThrow(new SearchUnavailableException("engine unreachable"));

        mockMvc.perform(get("/api/v1/search").param("q", "astronomy"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }
}
