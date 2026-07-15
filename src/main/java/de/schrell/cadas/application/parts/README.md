# Teilebibliothek

`StandardPartLibrary` stellt die mitgelieferten Tür-, Fenster- und Treppenabmessungen bereit.
`PartLibraryImportService` liest ergänzende `.cadasparts`-Bibliotheken und validiert jede Zeile,
bevor `StandardPartLibraryService` sie mit den internen Presets zusammenführt.

Presets sind Vorlagen; nach der Platzierung besitzt jedes Bauteil eine eigene UUID und eigene Maße.
Ungültige oder unbekannte Einträge dürfen nicht zu teilweise initialisierten Presets führen.
Bibliotheksimporte müssen Umlaute, leere Zeilen, Duplikate, unbekannte Typen und nicht positive Maße
mit klarer Diagnose behandeln.
