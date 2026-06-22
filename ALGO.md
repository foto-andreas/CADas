# Heizkreis-Routing Vario

## Ziel

`Vario` ist die neue Heizkreisvariante für rechteckige Heizbereiche. Sie wird zunächst parallel als Test-Router aufgebaut und noch nicht mit HKV-Anbindungen verbunden.

## Fachlicher Ablauf

1. Der Benutzer legt ein Heizkreis-Rechteck fest.
2. Die schmalere Seite heißt `b`, die längere Seite `l`, der Verlegeabstand heißt `v`.
3. Das Routing startet in der Mitte mit einem stilisierten, gerouteten `S`. Die Leserichtung des `S` liegt in Richtung der längeren Seite `l`.
4. An diesem `S` treffen Vorlauf und Rücklauf im Heizkreis zusammen.
5. Die Höhe des `S` beträgt `2v`.
6. Die Breite des `S` entsteht aus der Grundform plus Verlängerung der oberen, unteren und mittleren Linie um `(l - b) / (2v)` Rastersegmente. Ein sehr breites `S` ist fachlich zulässig.
7. Der Rücklauf wird am unteren Ende des `S` um das `S` herumgeführt, bis er am oberen Ende gleich lang wie die obere Kante des `S` ist.
8. Danach laufen beide Enden abwechselnd spiralförmig umeinander herum. Immer das andere Ende wird bis zur nächsten Biegung geführt, damit die engstmögliche Windung entsteht.
9. Die Spirale wächst, bis der Rand des Heizkreis-Rechtecks erreicht wird. Jedes Rohr endet, sobald es die Kante erreicht oder kein Platz mehr für die nächste Kurve bleibt.
10. Beide Rohrenden sollen dadurch in einer Ecke des Heizkreis-Rechtecks liegen.
11. Alle Richtungswechsel werden als Bögen mit Durchmesser `v` geführt. Es gibt keine eckigen Turns.
12. Heizkreise müssen um 90° drehbar sowie in Länge und Breite änderbar bleiben.
13. Vorlauf und Rücklauf müssen tauschbar sein.

## Zeichensprache

Die Zeichensprache beschreibt das Routing zunächst direkt und testbar.

* Großbuchstaben steuern den Vorlauf.
* Kleinbuchstaben steuern den Rücklauf.
* Beide Rohre starten im selben Punkt.
* Startausrichtung: Vorlauf nach oben, Rücklauf nach unten.
* `I` oder `i`: ein gerades Liniensegment vorwärts mit Länge `v`.
* `R` oder `r`: Viertelkreis nach rechts aus aktueller Blickrichtung, Richtungsänderung um 90° im Uhrzeigersinn, Durchmesser `v`.
* `L` oder `l`: Viertelkreis nach links aus aktueller Blickrichtung, Richtungsänderung um 90° gegen den Uhrzeigersinn, Durchmesser `v`.
* Leerzeichen und Enter werden ignoriert.
* Ungültige Eingaben werden im Testfenster mit `x` protokolliert.

Beispiele:

```text
RRrr
```

erzeugt die einfache S-Grundbewegung mit je zwei Bögen.

```text
IIIRRIIIiiirriii
```

verlängert die geraden Strecken exemplarisch um drei Rastersegmente.

## Aktueller Implementierungsstand

Der erste Schritt ist ein separates Testfenster in der Anwendung:

* Heizbereichsmaß in Zentimetern, zum Beispiel `200x300`.
* Verlegeabstand in Zentimetern, zum Beispiel `10`.
* Canvas mit zentriertem Startpunkt, Heizbereichs-Rechteck, Rinnenraster und farbigem Vorlauf/Rücklauf.
* Protokollfeld unter dem Canvas, das Eingaben buchstabenweise annimmt.
* 90°-Drehung durch Vertauschen von Länge und Breite.
* V/R-Tausch als reine Darstellung, ohne HKV-Verbindung.
* Ein Button erzeugt eine Vario-Doppelspirale aus dem aktuellen Heizbereich. Rechtecke werden auf `schmale Seite x lange Seite` normalisiert, weil die lange Richtung aktuell der Startausrichtung entspricht.

## Quadratischer Vario-Generator

Für ein Quadrat mit Seitenlänge `s` und Verlegeabstand `v` wird `n = floor(s / v)` verwendet. Der Generator erzeugt zwei unabhängige, ineinander verschachtelte Spiralsequenzen:

```text
Vorlauf:  RR I R II R III R ... I^(n-2)
Rücklauf: rr i r ii r iii r ... i^(n-1) r i^(n-1)
```

Die Kommandos werden für die Eingabe im Testfenster ineinander verschachtelt, bleiben aber je Rohr in dieser Reihenfolge. Dadurch wächst das Muster programmatisch nach außen und bleibt in einem Quadrat mit Seitenlänge höchstens `n * v`.

Für ein Rechteck mit kurzer Seite `b`, langer Seite `l` und `k = floor((l - b) / v)` werden die zur langen Seite parallelen Geraden um `k` Rastersegmente verlängert. Direkt am Anfang wird `k` außerdem auf Vorlauf und Rücklauf verteilt:

```text
Start: floor(k / 2) * i, ceil(k / 2) * I
Lange Geraden: Basislänge + k
Kurze Geraden: Basislänge
```

Damit entsteht aus der quadratischen Doppelspirale ein längliches Vario-Muster, ohne eckige Richtungswechsel einzuführen.

Die abschließenden Vorlauf- und Rücklaufgeraden werden gekürzt, sobald der erste Austritt aus der Belegefläche erreicht ist. Die Anschlusslage entsteht dadurch aus dem Routing selbst; es gibt keine freie Translation an eine Ecke. Die vorhandene halbe Rasterverschiebung bleibt ausschließlich zur Rinnenausrichtung erhalten.
