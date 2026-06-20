package de.schrell.cadas.application.help;

import java.util.ArrayList;
import java.util.List;

public final class MarkdownNavigationService {

    public List<HelpSection> sections(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        List<HelpSection> sections = new ArrayList<>();
        for (String line : markdown.lines().toList()) {
            int level = headingLevel(line);
            if (level < 2 || level > 4) {
                continue;
            }
            String title = line.substring(level).trim()
                    .replace("`", "")
                    .replace("**", "")
                    .replace("__", "");
            sections.add(new HelpSection(title, "abschnitt-" + (sections.size() + 1), level));
        }
        return List.copyOf(sections);
    }

    private int headingLevel(String line) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        return level < line.length() && Character.isWhitespace(line.charAt(level)) ? level : 0;
    }

    public record HelpSection(String title, String anchor, int level) {

        @Override
        public String toString() {
            return "  ".repeat(Math.max(0, level - 2)) + title;
        }
    }
}
