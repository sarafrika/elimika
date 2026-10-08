package apps.sarafrika.elimika.notifications.service;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives the plain-text part of an email from its HTML, so every message is multipart/alternative.
 * Links keep their address in brackets; layout tables collapse into short lines.
 */
final class EmailPlainText {

    private static final Pattern LINK = Pattern.compile("<a\\b[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private EmailPlainText() {
    }

    static String from(String html) {
        if (html == null) {
            return "";
        }
        String text = html.replaceAll("(?is)<head\\b.*?</head>", "")
                .replaceAll("(?is)<div[^>]*display:none[^>]*>.*?</div>", "")
                .replaceAll("(?is)<img\\b[^>]*>", "");
        Matcher links = LINK.matcher(text);
        StringBuilder withLinks = new StringBuilder();
        while (links.find()) {
            String label = links.group(2).replaceAll("(?s)<[^>]+>", "").trim();
            String href = links.group(1).replaceFirst("^mailto:", "");
            String replacement = label.isEmpty() || label.equals(href) ? href : label + " (" + href + ")";
            links.appendReplacement(withLinks, Matcher.quoteReplacement(replacement));
        }
        links.appendTail(withLinks);
        text = withLinks.toString()
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|h1|h2|h3|tr|table|li)>", "\n")
                .replaceAll("(?i)</td>", " ")
                .replaceAll("(?s)<[^>]+>", "");
        text = HtmlUtils.htmlUnescape(text)
                .replace(' ', ' ')
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }
}
