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
            Map.entry("`", "\\`"),
            Map.entry(">", "\\>"),
            Map.entry("#", "\\#"),
            Map.entry("+", "\\+"),
            Map.entry("-", "\\-"),
            Map.entry("=", "\\="),
            Map.entry("|", "\\|"),
            Map.entry("{", "\\{"),
            Map.entry("}", "\\}"),
            Map.entry(".", "\\."),
            Map.entry("!", "\\!")
    );

    public static String escapeMarkdown(String text) {
        StringBuilder escapedText = new StringBuilder(text);

        for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            int index = escapedText.indexOf(key);
            while (index != -1) {
                escapedText.replace(index, index + key.length(), value);
                index += value.length(); // Move to the end of the replacement
                index = escapedText.indexOf(key, index);
            }
        }

        return escapedText.toString();
    }


}
