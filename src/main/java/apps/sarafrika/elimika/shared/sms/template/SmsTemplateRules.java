package apps.sarafrika.elimika.shared.sms.template;

import apps.sarafrika.elimika.shared.sms.template.SmsTemplateKey.Placeholder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks and fills SMS templates. Pure functions; the admin screen runs the same checks. */
public final class SmsTemplateRules {

    public static final int ONE_SEGMENT = 160;
    public static final int MAX_BODY = 480;
    private static final Pattern TOKEN = Pattern.compile("\\{([a-z_]+)}");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");
    private static final String GSM_7 = "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
            + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà^{}\\[~]|€";

    private SmsTemplateRules() {
    }

    /** Problems that stop a template being saved; empty when it can be sent as one plain SMS. */
    public static List<String> problems(SmsTemplateKey key, String body) {
        List<String> problems = new ArrayList<>();
        if (body == null || body.isBlank()) {
            problems.add("The message cannot be empty.");
            return problems;
        }
        if (body.length() > MAX_BODY) {
            problems.add("The message is longer than " + MAX_BODY + " characters.");
        }
        Set<String> used = tokens(body);
        Set<String> allowed = new LinkedHashSet<>(key.placeholders().stream().map(Placeholder::name).toList());
        used.stream().filter(name -> !allowed.contains(name))
                .forEach(name -> problems.add("{" + name + "} is not available for " + key.title() + "."));
        key.placeholders().stream().filter(Placeholder::isRequired).filter(p -> !used.contains(p.name()))
                .forEach(p -> problems.add("{" + p.name() + "} must stay in " + key.title() + "."));
        if (TOKEN.matcher(body).replaceAll("").chars().anyMatch(ch -> ch == '{' || ch == '}')) {
            problems.add("A { or } is not part of a placeholder.");
        }
        int worst = worstCaseLength(key, body);
        if (nonGsmCharacters(body).isEmpty() && worst > ONE_SEGMENT) {
            problems.add("Worst case is " + worst + " characters, more than one SMS (" + ONE_SEGMENT + ").");
        }
        return problems;
    }

    /** Characters outside plain SMS text; any of them drops the message to 70-character segments. */
    public static Set<Character> nonGsmCharacters(String body) {
        Set<Character> bad = new LinkedHashSet<>();
        TOKEN.matcher(body == null ? "" : body).replaceAll("").chars()
                .filter(ch -> GSM_7.indexOf(ch) < 0).forEach(ch -> bad.add((char) ch));
        return bad;
    }

    /** Length with every placeholder at its longest. */
    public static int worstCaseLength(SmsTemplateKey key, String body) {
        Map<String, Integer> max = new java.util.HashMap<>();
        key.placeholders().forEach(p -> max.put(p.name(), p.maxLength()));
        return replace(body, name -> max.containsKey(name) ? "X".repeat(max.get(name)) : "{" + name + "}").length();
    }

    /** The message with each placeholder's sample value, for previews and test sends. */
    public static String sample(SmsTemplateKey key, String body) {
        Map<String, String> samples = new java.util.HashMap<>();
        key.placeholders().forEach(p -> samples.put(p.name(), p.sample()));
        return fill(key, body, samples);
    }

    /**
     * Fills the template. A sentence whose optional placeholder has no value is left out; inside a
     * sentence that also carries a required one, only that comma clause goes (else it shows as "-").
     */
    public static String fill(SmsTemplateKey key, String body, Map<String, String> values) {
        Set<String> required = new LinkedHashSet<>(key.placeholders().stream()
                .filter(Placeholder::isRequired).map(Placeholder::name).toList());
        List<String> kept = new ArrayList<>();
        for (String sentence : SENTENCE_END.split(body.trim())) {
            Set<String> names = tokens(sentence);
            boolean missing = names.stream().anyMatch(name -> !hasValue(values, name));
            boolean carriesRequired = names.stream().anyMatch(required::contains);
            if (missing && !carriesRequired) {
                continue;
            }
            String trimmed = sentence;
            for (String name : names) {
                if (!hasValue(values, name) && !required.contains(name)) {
                    trimmed = trimmed.replaceAll(",\\s*[^,.!?{}]*\\{" + name + "}[^,.!?{}]*", "");
                }
            }
            kept.add(replace(trimmed, name -> hasValue(values, name) ? values.get(name) : "-"));
        }
        return String.join(" ", kept).replace('—', '-').replace('–', '-');
    }

    static Set<String> tokens(String text) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static boolean hasValue(Map<String, String> values, String name) {
        String value = values.get(name);
        return value != null && !value.isBlank();
    }

    private static String replace(String text, Function<String, String> value) {
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(value.apply(matcher.group(1))));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
