# Geometriekern `de.schrell.cadas.domain.geometry`

## Zweck

Das Paket stellt die kleinsten, UI- und dateiformatunabhängigen Rechenbausteine bereit. Alle
Gebäudekoordinaten werden in Millimetern interpretiert. Einheitenumrechnung findet an den
Systemgrenzen statt; Pixel, Zoom und JavaFX-Koordinaten dürfen nicht in den Geometriekern gelangen.

## Modell

* `Length` kapselt Längen mit expliziter `LengthUnit`-Umrechnung und vermeidet verstreute
  Umrechnungsfaktoren.
* `Angle` normalisiert und formatiert Winkel.
* `PlanPoint` repräsentiert einen zweidimensionalen Punkt in der Grundrissebene.
* `PlanSegment` verbindet zwei Punkte und liefert Länge, Richtung, Projektion und Abstände.
* Raster- und Rechtecktypen unterstützen Snap-, Auswahl- und Zerlegungsalgorithmen.

## Rechenregeln

* Fachliche Toleranzen müssen explizit am jeweiligen Algorithmus benannt werden. Exakte
  Objektgleichheit darf nicht unbemerkt durch eine geometrische Näherung ersetzt werden.
* Null-Längen sind nur zulässig, wenn der jeweilige Typ sie fachlich ausdrücklich unterstützt.
* Koordinatentransformationen in Bildschirm- oder 3D-Werte liegen in `application.view` oder `ui`.
* Flächen entstehen aus geschlossenen Polygonen; die Punktreihenfolge darf für Algorithmen nicht
  stillschweigend als im oder gegen den Uhrzeigersinn angenommen werden.

## Abhängigkeiten

Dieses Paket hängt ausschließlich von der Java-Standardbibliothek ab. Domänenmodelle dürfen den
Geometriekern verwenden, der Geometriekern kennt jedoch weder Gebäudeobjekte noch JavaFX.

## Review-Leitfaden

1. Einheiten an jedem öffentlichen Ein- und Ausgang prüfen.
2. Leere, einpunktige und entartete Geometrien prüfen.
3. Vertikale, horizontale und nahezu parallele Segmente testen.
4. Große, negative und nicht endliche Werte an Systemgrenzen abweisen.
5. Toleranzen auf Maßstab und fachliche Bedeutung prüfen.
