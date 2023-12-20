package ee.tenman.elektrihind.utility;

import java.util.Map;

public class TextUtility {

    private static final Map<String, String> REPLACEMENTS = Map.ofEntries(
            Map.entry("_", "\\_"),
            Map.entry("*", "\\*"),
            Map.entry("[", "\\["),
            Map.entry("]", "\\]"),
            Map.entry("(", "\\("),
            Map.entry(")", "\\)"),
            Map.entry("~", "\\~"),
            Map.entry(">", "\\>"),
            Map.entry("#", "\\#"),
            Map.entry("+", "\\+"),
            Map.entry("-", "\\-"),
            Map.entry("=", "\\="),
            Map.entry("|", "\\|"),
            Map.entry("{", "\\{"),
            Map.entry("}", "\\}"),
            Map.entry(".", "\\."),
            Map.entry("!", "\\!"),
            Map.entry("<", "\\<")
    );

    public static String escapeMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder escapedText = new StringBuilder(text);

        REPLACEMENTS.forEach((key, value) -> {
            int index = escapedText.indexOf(key);
            while (index != -1) {
                escapedText.replace(index, index + key.length(), value);
                index += value.length(); // Move to the end of the replacement
                index = escapedText.indexOf(key, index);
            }
        });

        return escapedText.toString();
    }

}
