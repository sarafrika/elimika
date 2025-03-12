package apps.sarafrika.elimika.common.internal;

import lombok.Data;

import java.util.Map;

@Data
public class TemplateData {
    private String recipient;
    private Map<String, String> placeholders;

    public TemplateData(String recipient, Map<String, String> placeholders) {
        this.recipient = recipient;
        this.placeholders = placeholders;
    }
}

