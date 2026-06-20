package de.schrell.cadas.application.reports;

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
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; color: #26231f; margin: 28px; line-height: 1.5; max-width: 920px; }
                h1 { font-size: 28px; margin: 0 0 22px; color: #3a3026; }
                h2 { font-size: 20px; margin: 32px 0 12px; border-bottom: 2px solid #d6c9b5; padding-bottom: 4px; color: #3a3026; }
                h3 { font-size: 16px; margin: 24px 0 8px; color: #4a3f33; }
                h4 { font-size: 14px; margin: 18px 0 6px; color: #5c5146; }
                p, ul, ol { margin: 8px 0 14px; }
                ul, ol { padding-left: 24px; }
                li { margin: 4px 0; }
                code { background: #f0ebe0; padding: 1px 4px; border-radius: 3px; font-size: 13px; font-family: "SF Mono", "Menlo", monospace; }
                pre { background: #f6f1e8; padding: 12px 16px; border-radius: 6px; overflow-x: auto; border: 1px solid #e2d8c8; }
                pre code { background: none; padding: 0; }
                blockquote { border-left: 4px solid #d6c9b5; margin: 12px 0; padding: 4px 16px; color: #6b6258; background: #f6f1e8; }
                table { border-collapse: collapse; width: 100%; margin: 12px 0 22px; font-size: 13px; }
                th { background: #efe9dc; text-align: left; color: #3a3026; }
                th, td { border: 1px solid #cfc7ba; padding: 6px 8px; vertical-align: top; }
                tr:nth-child(even) td { background: #f6f1e8; }
                th[align="right"], td[align="right"] { text-align: right; white-space: nowrap; }
                img { max-width: 100%; }
                a { color: #6a5a45; }
                hr { border: none; border-top: 1px solid #cfc7ba; margin: 24px 0; }
                @media print { body { margin: 12mm; max-width: none; } table { page-break-inside: avoid; } h2, h3 { page-break-after: avoid; } }
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
