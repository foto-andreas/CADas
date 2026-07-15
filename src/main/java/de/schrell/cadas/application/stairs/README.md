# Treppenunterbau

`StairUnderbuildService` erzeugt aus einer Treppe optionale linke und rechte Unterbauwände. Deren
UUIDs werden deterministisch aus Treppe und Seite abgeleitet, damit Neuberechnungen vorhandene
Türen und Fenster erhalten können.

Die Wandoberkante folgt der Treppensteigung als Polygonprofil. Unterbaubreiten müssen eine freie
Treppenbreite lassen; die Untersichtdicke bleibt kleiner als die Gesamthöhe. Wendeltreppen besitzen
keinen geraden Unterbau. Beim Deaktivieren einer Seite werden nur deren Wand und abhängige
Öffnungen entfernt.

Tests prüfen beide Seiten, Rotation, unzulässige Breiten, variable Wandhöhe und Öffnungen im hohen
sowie zu niedrigen Profilabschnitt.
