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

Die abschließenden Vorlauf- und Rücklaufgeraden werden gekürzt oder verlängert, sobald der erste Austritt aus der Belegefläche erreicht ist. Die Anschlusslage entsteht dadurch aus dem Routing selbst; es gibt keine freie Translation an eine Ecke.

Optional kann der Generator mit schlangenförmiger Mittellinie starten, wenn die Breite sonst um ein Raster nicht sauber aufgeht. Die Schlange belegt maximal zwei Reihen. Ihre Länge wird aus `k = floor((l - b) / v)` plus zwei zusätzlichen Rasterelementen berechnet und auf eine gerade Anzahl Rastersegmente aufgerundet. Das Testfenster bietet dafür den Schalter `Mittellinie schlängeln`. Für `b = 7v`, `l = 16v` und `k = 9` ist der Anfang:

```text
rLRRllrrLLRRllrrLLRRllrriIRriiiiiiiiiiirIIIIIIIIIIIRiiirIIIR
```

Die Regel dahinter:

```text
kleinste Schlange: rLRR
Erweiterung: abwechselnd llrr und LLRR bis zur Schlangenlänge
Übergang: iI, danach Rr zur normalen Doppelspiralführung
erste äußere Runde: mindestens so lang wie die berechnete Schlangenmitte
alle äußeren Geraden wegen der breiteren Schlangenmitte: Basislänge + 1
```

Die Schlangenmitte ist gegenüber der normalen Mittellinie ein Rasterelement breiter. Deshalb werden alle folgenden geraden Vorlauf- und Rücklauf-Läufe jeweils um ein zusätzliches `I` beziehungsweise `i` verlängert. Die erste Umrundung darf nach dem Runden der Schlangenlänge nicht kürzer als diese Schlangenmitte sein.

Bei ungerader kurzer Rasterseite wird nur die größte gerade Rasterbreite genutzt, weil die Rinnen sonst nicht symmetrisch im Rechteck liegen. Für `b = 11v`, `l = 18v` wird daher mit zehn kurzen Rastersegmenten gerechnet und die Doppelspirale endet früher. Ist zusätzlich `k` ungerade, werden nur die zur langen Seite laufenden Geraden um ein weiteres Rastersegment verlängert, damit die Außenlage wieder auf dem Verlegeraster bleibt.

## Meander-Generator

Der Meander ist eine zweite Testfenster-Variante. Die gespeicherten Referenzen `5v x 5v`, `6v x 8v` und `20v x 30v` definieren eine einfache Reihenverlegung ohne zusätzliche Mittelschlange:

```text
5v x 5v:
Rücklauf: i rr iii ll iiii
Vorlauf:  II RR III LL IIII

6v x 8v:
Rücklauf: iii rr iiiiiii ll iiiiiii rr iiiiiii
Vorlauf:  IIII RR IIIIIII LL IIIIIII RR IIIIIII

20v x 30v:
Rücklauf: i^14 (ll i^29 rr i^29)^5
Vorlauf:  I^15 (LL I^29 RR I^29)^5
```

Die daraus abgeleitete Regel nutzt `n = floor(b / v)` und `m = floor(l / v)`. Es werden `floor(n / 2)` Reihen je Rohr gelegt. Der Vorlauf startet bei rechteckigen Feldern mit ungeradem `m` mit `ceil(m / 2)`, sonst mit `floor(m / 2)` geraden Segmenten. Der Rücklauf startet mit einem Segment weniger als `floor(m / 2)`. Jede folgende Längsreihe nutzt bei Rechtecken `m - 1` Segmente. Nur beim quadratischen Minimalfall mit ungeradem `m` ist die erste Längsreihe `m - 2` Segmente lang. Bei `n mod 4 = 0` beginnen die Halbkreise nach links, sonst nach rechts.

```text
Rücklauf: i^(floor(m/2)-1), dann floor(n/2) mal abwechselnd rr/ll plus Längsreihe
Vorlauf:  I^floor(m/2), dann floor(n/2) mal abwechselnd RR/LL plus Längsreihe
```

Der Schalter `Mittellinie schlängeln` ersetzt beim Meander die mittlere Startgerade durch ein zweireihiges Schlangenpräfix. Die Länge der Schlange folgt der Rasterdifferenz `k = m - n`. Für `20v x 31v` und `k = 11` ist die Referenz:

```text
Rücklauf: r (ll rr)^7 i r i^30, dann (ll i^30 rr i^30)^4
Vorlauf:  I L (RR LL)^7 RR I R I^30, dann (LL I^30 RR I^30)^4 LL I^30
```

Nach der Schlange startet der Meander auf der Gegenseite. Beim Rücklauf entfällt dadurch eine normale Reihe. Bei ungeradem `m` liefert die Schlange den ersten einfachen Bogen der folgenden Parallelreihe mit; die folgende Reihe startet deshalb mit einem einfachen statt einem doppelten Bogen.

Die Schlangengruppen werden so gewählt, dass die Schlange einschließlich ihrer Abschlussbögen möglichst genauso lang ist wie eine normale Parallelreihe einschließlich Bögen, also `m - 1` gerade Segmente plus zwei Bogenbefehle. Dafür werden ausschließlich zusätzliche Schlangengruppen genutzt; oberhalb und unterhalb der Schlange werden keine zusätzlichen Geraden eingefügt. Für `20v x 30v` entstehen dadurch sieben Rücklauf- und sechs Vorlaufgruppen.

Bei geradem `m` entfällt im Rücklauf vor den Abschlussbögen das erste gerade `i`, wenn die Rücklauf-Schlange durch zusätzliche Schlangengruppen verlängert wurde; der Abschluss läuft dann direkt als `... l rr ...`.

## Zuleitungs-Enden

Im Testfenster können die Feldenden segmentweise angepasst werden:

```text
VL +: hängt ein I an
VL -: entfernt das letzte I, wenn das letzte Vorlauf-Kommando gerade ist
RL +: hängt ein i an
RL -: entfernt das letzte i, wenn das letzte Rücklauf-Kommando gerade ist
```

Damit lassen sich Vorlauf und Rücklauf am Heizkreisrand zunächst manuell um je ein Rastersegment verlängern oder kürzen. Die spätere HKV-Anbindung wird darauf aufbauen.

## Vario-Malen

Im Kommandoprotokoll kann Vario zusätzlich schrittweise gemalt werden:

```text
+: malt die nächste gemeinsame VL/RL-Seite der aktuellen Vario-Doppelspirale
-: entfernt die zuletzt gemalte gemeinsame VL/RL-Seite
```

Grundlage ist der aktuelle Heizbereich mit aktuellem Verlegeabstand und Schlangen-Schalter. Die aktuelle Eingabe muss ein Prefix des daraus berechneten Vario-Routers sein. Ist das berechnete Rechteck vollständig gemalt, ergänzt `+` weiterhin eine gemeinsame gerade Seite `Ii`; `-` entfernt diese zusätzlichen Seiten wieder paarweise.
