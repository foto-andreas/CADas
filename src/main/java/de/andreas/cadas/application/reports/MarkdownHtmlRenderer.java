package de.andreas.cadas.application.reports;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

public final class MarkdownHtmlRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownHtmlRenderer() {
        List<Extension> extensions = List.of(TablesExtension.create());
        parser = Parser.builder()
                .extensions(extensions)
                .build();
        renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .escapeHtml(true)
                .build();
    }

    public String renderDocument(String markdown) {
        String renderedBody = renderer.render(parser.parse(markdown == null ? "" : markdown));
        return """
                <!doctype html>
                <html lang="de">
                <head>
                <meta charset="UTF-8">
                <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; color: #26231f; margin: 28px; line-height: 1.45; }
                h1 { font-size: 28px; margin: 0 0 22px; }
                h2 { font-size: 20px; margin: 28px 0 12px; border-bottom: 1px solid #cfc7ba; padding-bottom: 4px; }
                h3 { font-size: 16px; margin: 22px 0 8px; }
                p, ul { margin: 8px 0 14px; }
                ul { padding-left: 22px; }
                table { border-collapse: collapse; width: 100%; margin: 10px 0 22px; font-size: 12px; }
                th { background: #efe9dc; text-align: left; }
                th, td { border: 1px solid #cfc7ba; padding: 6px 8px; vertical-align: top; }
                th[align="right"], td[align="right"] { text-align: right; white-space: nowrap; }
                @media print { body { margin: 12mm; } table { page-break-inside: avoid; } h2, h3 { page-break-after: avoid; } }
                </style>
                </head>
                <body>
                """
                + renderedBody
                + """
                </body>
                </html>
                """;
    }
}
