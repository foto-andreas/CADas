package de.andreas.cadas.application.reports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownHtmlRendererTest {

    private final MarkdownHtmlRenderer renderer = new MarkdownHtmlRenderer();

    @Test
    void rendertMarkdownMitTabellenUndEscapedHtml() {
        String html = renderer.renderDocument("""
                # Materialliste Beläge

                | Belag | Stückzahl |
                |---|---:|
                | Fliese <Test> | 3 |
                """);

        assertTrue(html.contains("<h1>Materialliste Beläge</h1>"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<td align=\"right\">3</td>"));
        assertTrue(html.contains("Fliese &lt;Test&gt;"));
        assertFalse(html.contains("Fliese <Test>"));
    }
}
