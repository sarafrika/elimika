package apps.sarafrika.elimika.shared.sms.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/** One system SMS as the admin screen shows it. */
public record SmsTemplateResponse(
        @JsonProperty("key") String key,
        @JsonProperty("name") String name,
        @JsonProperty("group") String group,
        @JsonProperty("trigger") String trigger,
        @JsonProperty("security") boolean security,
        @JsonProperty("enabled") boolean enabled,
        @JsonProperty("body") String body,
        @JsonProperty("default_body") String defaultBody,
        @JsonProperty("edited") boolean edited,
        @JsonProperty("updated_by_name") String updatedByName,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("placeholders") List<PlaceholderResponse> placeholders) {

    public record PlaceholderResponse(
            @JsonProperty("name") String name,
            @JsonProperty("label") String label,
            @JsonProperty("sample") String sample,
            @JsonProperty("max_length") int maxLength,
            @JsonProperty("required") boolean required) {
    }

    public record UpdateRequest(@JsonProperty("body") String body, @JsonProperty("enabled") Boolean enabled) {
    }

    public record TestResponse(@JsonProperty("sent") boolean sent, @JsonProperty("to_masked") String toMasked) {
    }

    public static SmsTemplateResponse of(SmsTemplateService.SmsTemplateView view) {
        SmsTemplateKey key = view.key();
        return new SmsTemplateResponse(key.name(), key.title(), key.group(), key.trigger(), key.security(), view.enabled(),
                view.body(), key.defaultBody(), view.edited(), view.updatedByName(), view.updatedAt(),
                key.placeholders().stream().map(p -> new PlaceholderResponse(p.name(), p.label(), p.sample(),
                        p.maxLength(), p.isRequired())).toList());
    }
}
