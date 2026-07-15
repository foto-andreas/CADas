# Hilfe und Programminformation

`HelpContentService` liest eingebettete Dokumente aus den Ressourcen. `MarkdownNavigationService`
extrahiert Überschriften und erzeugt dieselben stabilen Anker wie der HTML-Renderer.
`AboutInformation` kombiniert Anwendungsversion, Build-Zeitpunkt und Laufzeitdaten für den
Info-Dialog.

Ressourcen müssen UTF-8 verwenden. Fehlende Dokumente führen zu einer verständlichen Diagnose.
Anker müssen eindeutig, deterministisch und als JavaScript-String sicher kodierbar sein. Bei
Änderungen sind Inhaltsverzeichnis, Suche, Druckansicht und die Kopie der Benutzerdokumentation in
beiden Ressourcenpfaden zu prüfen.
