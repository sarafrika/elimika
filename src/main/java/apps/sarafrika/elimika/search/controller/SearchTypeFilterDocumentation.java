package apps.sarafrika.elimika.search.controller;

import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Comparator;
import java.util.List;

/**
 * Documents the filter map of {@code GET /api/v1/search/{type}} in OpenAPI: for every type, the
 * attributes it filters and sorts on. The table is built from the owning modules'
 * {@link GlobalSearchProvider} definitions - the same allow-lists the endpoint enforces - so the
 * documentation cannot drift from what the endpoint accepts.
 */
@Component
class SearchTypeFilterDocumentation implements OperationCustomizer {

    static final String OPERATION_ID = "searchByType";

    private final ObjectProvider<GlobalSearchProvider> providers;

    SearchTypeFilterDocumentation(ObjectProvider<GlobalSearchProvider> providers) {
        this.providers = providers;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (!OPERATION_ID.equals(operation.getOperationId())) {
            return operation;
        }
        List<GlobalSearchProvider> types = providers.orderedStream()
                .sorted(Comparator.comparing(GlobalSearchProvider::type))
                .toList();
        if (types.isEmpty()) {
            return operation;
        }
        StringBuilder table = new StringBuilder("\n\n**Filter map** (filters use `field` or `field_op`, "
                + "op one of eq, noteq, in, notin, gt, gte, lt, lte, between):\n\n"
                + "| type | filterable (also valid in `facets`) | sortable |\n|---|---|---|\n");
        for (GlobalSearchProvider provider : types) {
            SearchIndexDefinition definition = provider.definition();
            table.append("| `").append(provider.type()).append("` | ")
                    .append(codeList(definition.filterableAttributes())).append(" | ")
                    .append(codeList(definition.sortableAttributes())).append(" |\n");
        }
        String description = operation.getDescription() == null ? "" : operation.getDescription();
        operation.setDescription(description + table);
        return operation;
    }

    private static String codeList(List<String> attributes) {
        return attributes.isEmpty() ? "-" : String.join(", ", attributes.stream().map(a -> "`" + a + "`").toList());
    }
}
