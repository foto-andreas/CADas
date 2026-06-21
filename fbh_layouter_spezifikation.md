# Spezifikation: Java-Layouter für Fußbodenheizungs-Heizkreise im Verlegeraster

**Ziel:** Dieses Dokument beschreibt die Aufgabe, Randbedingungen, Datenmodelle, Algorithmen, Validierungen, Tests und Beispielcode so detailliert, dass ein Coding-Agent daraus ein belastbares Java-Programm bzw. eine Java-Klasse für die automatische und interaktive Planung von Fußbodenheizungsrohren erstellen kann.

Der Schwerpunkt liegt auf **bifilaren Spiral-/Schneckenmustern**. Eine **Meander-Variante** soll ebenfalls unterstützt werden, aber als sekundärer Modus. Das spätere Tool soll interaktiv sein: Benutzer können Raumumriss, HKV-Position, Heizkreisgrenzen und Feldgrenzen ändern; das Layout wird danach neu berechnet und neu gerendert.

---

## 1. Kurzbeschreibung der Aufgabe

Es soll ein Java-Layouter entstehen, der für einen **geschlossenen Raumgrundriss als Polygon** Heizkreise einer Fußbodenheizung plant.

Eingaben:

- geschlossener Raumumriss als Polygon
- optional: Hindernisse / Ausschnitte als Polygone
- Rastermaß, typischerweise 100 mm
- Rohrdurchmesser, z. B. 11,6 mm
- Noppendurchmesser bzw. Abstandsrinnen
- gewünschte maximale Heizkreislänge, z. B. 80 m
- HKV-/Verteilerposition
- optional manuell vorgegebene Heizkreisumrisse als Rechtecke oder Polygone
- optional manuell gesetzte Anschlusskorridore
- optional manuell fixierte Start-/Endpunkte
- optional Vorgabe: Spiral-, Meander- oder Auto-Modus

Ausgaben:

- geometrisch korrektes Rohrlayout
- getrennte Darstellung von Vorlauf und Rücklauf
- Längen je Heizkreis
- Validierungsbericht
- SVG/Canvas-kompatible Rendering-Daten
- maschinenlesbares Layoutmodell zur weiteren Bearbeitung

Wichtig: Das Programm darf **niemals** ein Layout als gültig ausgeben, wenn Leitungen außerhalb des Raums liegen, sich kreuzen, dieselbe Rinne doppelt belegen oder Zu-/Rückführungen „im Nichts“ enden.

---

## 2. Grundprinzip

Das Layoutproblem darf nicht als reines Zeichnen verstanden werden. Es ist ein **Constraint- und Graphproblem**.

Die korrekte Reihenfolge lautet:

1. Raumgeometrie normalisieren.
2. Verlegeraster und zulässige Rinnen erzeugen.
3. Raum in sinnvolle Verlegefelder zerlegen oder manuelle Felder übernehmen.
4. Pro Feld Musterkandidaten erzeugen.
5. Kandidaten auf Kantenebene prüfen.
6. Heizkreise so auswählen/teilen, dass Länge, Fläche und Anschlussfähigkeit passen.
7. HKV-Anschlüsse als eigene Pfade im Raster planen.
8. Vorlauf/Rücklauf-Orientierungen benachbarter Kreise optimieren.
9. Globale Validierung aller Rohrsegmente.
10. Erst danach rendern.

Falsch wäre:

```text
erst zeichnen → danach irgendwie Rückführung suchen → nachträglich kaputtschneiden
```

Richtig ist:

```text
Kandidaten erzeugen → Anschlussfähigkeit prüfen → global kombinieren → validieren → rendern
```

---

## 3. Harte Randbedingungen

Diese Bedingungen sind **MUSS-Bedingungen**. Ein Layout ist ungültig, sobald eine davon verletzt wird.

### 3.1 Raumgrenzen

- Jedes Rohrsegment muss vollständig im Raum liegen.
- Kein Rohrsegment darf durch Ausschnitte, Leerbereiche, Schächte, Wände oder außerhalb des Polygons laufen.
- Bögen müssen ebenfalls vollständig im Raum liegen.
- Bei Polygonen ist nicht nur der Mittelpunkt einer Kante zu prüfen, sondern die gesamte Kante bzw. bei Bögen eine ausreichend feine Abtastung.

### 3.2 Raster und Rinnen

- Rohre verlaufen auf einem definierten Verlegeraster.
- Typischer Rasterabstand: 100 mm.
- Noppenzentren liegen z. B. bei `x = 50, 150, 250, ...` und `y = 50, 150, 250, ...`.
- Rohrachsen liegen in den Rinnen, z. B. bei `x = 100, 200, 300, ...` bzw. `y = 100, 200, 300, ...`.
- Der tatsächliche Rohrdurchmesser muss beim Rendering berücksichtigt werden.

### 3.3 Eindeutige Rinnenbelegung

- Jede gerade Rasterkante darf maximal einmal belegt sein.
- Eine Anschlussleitung darf nicht in derselben Rinne wie ein Heizfeldrohr laufen.
- Eine Anschlussleitung darf nicht quer durch eine Wendegasse eines anderen Heizkreises laufen.
- Eine grafische Kreuzung ist unzulässig, auch wenn die Kantenprüfung sie nicht erkennt.
- Bögen müssen ebenfalls auf Überschneidung geprüft werden.

### 3.4 Anschlussleitungen

- Jede Zuleitung beginnt am HKV-Port und endet am Vorlaufstart des Heizkreises.
- Jede Rückleitung beginnt am Rücklaufende des Heizkreises und endet am zugehörigen HKV-Port.
- Leitungen dürfen nicht „im Nichts“ enden.
- VL/RL eines Heizkreises müssen am HKV als Paar enden.
- Pro Paar ist eine Breite von ca. 50 mm vorzusehen.
- Die paarweise Zuordnung muss im Layoutmodell eindeutig gespeichert werden.
- Anschlussleitungen dürfen nur in erlaubten Anschlusskorridoren oder freien Rinnen verlaufen.

### 3.5 Heizkreisgrenzen

- Benutzer können Heizkreisumrisse als Rechtecke oder Polygone vorgeben.
- Diese Umrisse sind editierbar.
- Nach einer Benutzeränderung muss neu gerendert werden.
- Vorgegebene Heizkreisfelder dürfen nicht blind übernommen werden, sondern müssen validiert werden:
  - vollständig im Raum
  - nicht überlappend, außer definierte Überlappung ist erlaubt
  - Anschlussfähigkeit zum HKV
  - sinnvolle Mindestgröße
  - maximale Länge

### 3.6 Heizkreislängen

- Standard-Maximum: 80 m pro Heizkreis, konfigurierbar.
- Warnschwellen: z. B. 75 m und 80 m.
- Sehr kurze Restkreise sind zu vermeiden.
- Als Mindestziel können z. B. 30 m oder 40 m angesetzt werden, wenn hydraulisch sinnvoll.
- Ausnahme: Benutzer erlaubt explizit kurzen Sonderkreis.

### 3.7 Vorlauf/Rücklauf-Nachbarschaft

- Bei angrenzenden Heizkreisen sollen nicht auf beiden Seiten gleichartige Leitungen aneinandergrenzen, also nicht VL/VL oder RL/RL, sofern durch Umkehrung vermeidbar.
- Der Solver muss Heizkreise bei Bedarf invertieren:
  - Vorlauf- und Rücklaufrolle tauschen
  - Farben beim Rendering anpassen
  - Anschlussports entsprechend tauschen
- Die Optimierung muss global über alle Nachbarschaften erfolgen, nicht nur paarweise lokal.

### 3.8 Bögen und Wendungen

- 90°-Wendungen im Raster werden als Bögen gerendert.
- Der Biegeradius muss konfigurierbar sein.
- Bei zentraler Verbindung einer bifilaren Spirale ist, falls geometrisch vorgesehen, ein **Halbkreis** zu rendern, kein versehentlicher Viertelkreis.
- Bögen dürfen keine fremden Rinnen schneiden.
- Bögen dürfen nicht außerhalb des Raums liegen.
- Bei unzureichendem Radius ist das Muster ungültig.

---

## 4. Soll-Anforderungen

Diese Bedingungen sind keine harten Muss-Kriterien, beeinflussen aber die Bewertung der Lösung.

- möglichst hohe Flächenabdeckung
- möglichst gleichmäßige Heizkreislängen
- möglichst kurze Anschlussleitungen
- möglichst wenig Sonderkorridore
- möglichst wenige Kreuzungskonflikte bereits in der Kandidatenerzeugung
- möglichst einfache Geometrie
- möglichst wenige manuelle Eingriffe
- benachbarte Kreise thermisch sinnvoll verschränkt
- Vorlaufbereiche möglichst an Außenwänden, wenn konfiguriert

---

## 5. Datenmodell

### 5.1 Maßeinheiten

Alle internen Berechnungen sollen in Millimetern erfolgen.

```java
public record PointMm(double x, double y) {}

public record GridPoint(int ix, int iy) {
    public double x(double pitchMm) { return ix * pitchMm; }
    public double y(double pitchMm) { return iy * pitchMm; }
}
```

Rasterknoten sind integerbasiert. Geometrische Renderingpunkte dürfen double sein.

### 5.2 Raum

Der Raum wird als geschlossenes Polygon vorgegeben.

```java
public final class RoomGeometry {
    private final Polygon outerBoundary;
    private final List<Polygon> holes;

    public boolean contains(PointMm p) { ... }
    public boolean containsSegment(PointMm a, PointMm b) { ... }
    public boolean containsArc(Arc arc) { ... }
}
```

Wichtig:

- `containsSegment` darf nicht nur den Mittelpunkt prüfen.
- Für einfache Rasterkanten kann geprüft werden, ob Endpunkte und Zwischenpunkte im Raum liegen.
- Für Polygone sind robuste Geometrieoperationen sinnvoll, z. B. über JTS Topology Suite.

Empfohlene Bibliothek:

```text
org.locationtech.jts:jts-core
```

### 5.3 Rastergraph

```java
public final class GridGraph {
    private final double pitchMm;
    private final Set<GridPoint> nodes;
    private final Set<GridEdge> edges;
    private final Map<GridPoint, List<GridEdge>> adjacency;

    public List<GridPoint> shortestPath(
        GridPoint start,
        GridPoint goal,
        Set<GridEdge> blockedEdges,
        Predicate<GridEdge> allowedEdge
    ) { ... }
}
```

`GridEdge` muss ungerichtet normalisiert sein:

```java
public record GridEdge(GridPoint a, GridPoint b) {
    public GridEdge {
        if (compare(b, a) < 0) {
            GridPoint tmp = a;
            a = b;
            b = tmp;
        }
    }
}
```

### 5.4 Rohrsegment

```java
public sealed interface PipeSegment permits LineSegment, ArcSegment {
    PipeRole role();
    CircuitId circuitId();
}

public enum PipeRole {
    SUPPLY,      // Vorlauf
    RETURN,      // Rücklauf
    BRIDGE,      // zentrale Verbindung VL/RL
    CONNECTOR    // Zu-/Rückführung zum HKV, wenn separat markiert
}
```

### 5.5 Heizkreis

```java
public final class HeatingCircuit {
    private final String id;
    private final Geometry boundary;
    private final PatternType patternType;
    private final List<PipeSegment> supplySegments;
    private final List<PipeSegment> returnSegments;
    private final ConnectorPath supplyConnector;
    private final ConnectorPath returnConnector;
    private final ManifoldPair manifoldPair;
    private final double lengthMm;
    private final boolean inverted;
}
```

### 5.6 HKV / Verteiler

```java
public final class Manifold {
    private PointMm anchor;
    private double pairPitchMm;     // z. B. 50 mm
    private double pairWidthMm;     // z. B. 50 mm
    private Orientation orientation;
    private List<ManifoldPair> pairs;
}

public final class ManifoldPair {
    private int index;
    private PointMm supplyPort;
    private PointMm returnPort;
}
```

Der Benutzer muss den HKV verschieben können. Danach müssen Anschlusswege neu geroutet werden.

### 5.7 Manuell editierbare Felder

```java
public final class EditableCircuitBoundary {
    private String id;
    private Geometry geometry;      // Rechteck oder Polygon
    private boolean locked;
    private PatternType preferredPattern;
}
```

Benutzeraktionen:

- neues Rechteckfeld zeichnen
- Rechteck verschieben
- Rechteckgröße ändern
- Polygonpunkte verschieben
- Feld löschen
- Feld sperren
- Feldmuster ändern
- HKV verschieben
- Heizkreisrichtung invertieren
- Anschlussport manuell setzen

---

## 6. Algorithmusübersicht

### 6.1 Hauptpipeline

```java
LayoutResult solve(LayoutInput input) {
    NormalizedGeometry geometry = normalize(input.roomPolygon, input.holes);

    GridGraph graph = buildGridGraph(geometry, input.pitchMm, input.clearances);

    List<CircuitField> fields = input.manualFields().isEmpty()
        ? autoPartition(geometry, graph, input)
        : validateAndNormalizeManualFields(input.manualFields(), geometry, graph);

    List<PatternCandidate> candidates = generatePatternCandidates(fields, graph, input);

    List<PatternCandidate> selected = optimizeCircuitSelection(candidates, input);

    selected = optimizeFlowOrientation(selected, graph, input);

    ManifoldPlan manifold = allocateManifoldPairs(input.manifold, selected, input);

    ConnectorPlan connectors = routeConnectors(manifold, selected, graph, input);

    ValidationReport report = validateAll(geometry, graph, selected, connectors, input);

    if (!report.isValid()) {
        return LayoutResult.invalid(report);
    }

    RenderModel renderModel = buildRenderModel(selected, connectors, report);

    return LayoutResult.valid(renderModel, report);
}
```

---

## 7. Automatische Raumzerlegung

### 7.1 Ziel

Aus einem beliebigen geschlossenen Raum-Polygon sollen sinnvolle rechteckige oder polygonale Verlegefelder abgeleitet werden.

Für den ersten stabilen Solver wird empfohlen:

1. Raum in Rasterzellen zerlegen.
2. Belegte Zellen bestimmen.
3. Maximal zusammenhängende rechteckige Blöcke suchen.
4. Blöcke nach Länge und Anschlussfähigkeit splitten.
5. Kleine Restflächen mit Nachbarfeldern zusammenführen.

### 7.2 Rechteckzerlegung per Raster

Rasterzellen:

```text
Zelle (i,j) repräsentiert Fläche:
x = i*pitch .. (i+1)*pitch
y = j*pitch .. (j+1)*pitch
```

Eine Zelle ist nutzbar, wenn sie vollständig im Raum liegt oder nach Toleranzregel ausreichend im Raum liegt.

Algorithmus:

```text
1. Erzeuge binäre Matrix usable[i][j].
2. Suche größte Rechtecke aus zusammenhängenden true-Zellen.
3. Entferne Rechteck aus Matrix.
4. Wiederhole, bis keine sinnvollen Rechtecke übrig sind.
5. Restzellen an angrenzende Felder anhängen oder als Sonderfeld markieren.
```

### 7.3 Alternative: horizontale Bänder

Für L- und U-Formen oft einfacher:

```text
1. Für jede y-Rasterzeile zusammenhängende x-Intervalle suchen.
2. Aufeinanderfolgende Zeilen mit gleichen oder ähnlichen Intervallen bündeln.
3. Daraus horizontale Bänder bilden.
4. Lange Bänder in Teilfelder splitten.
```

### 7.4 Manuelle Felder

Wenn der Benutzer Rechtecke vorgibt:

- Diese Felder haben Vorrang.
- Der Solver darf sie splitten, wenn sonst die Maximallänge überschritten wird.
- Der Solver darf eine Warnung ausgeben, wenn ein Feld nicht anschließbar ist.
- Der Solver darf ein Feld nicht einfach ignorieren.

---

## 8. Spiralmuster

### 8.1 Ziel

Ein Spiral-/Schneckenmuster soll innerhalb eines rechteckigen oder polygonalen Feldes Vorlauf und Rücklauf möglichst gleichmäßig nebeneinander führen.

Vorteil:

- Die Rückführung ist Bestandteil des Musters.
- Es muss keine separate Rücklaufleitung durch das Heizfeld geführt werden.
- Die thermische Verteilung ist gleichmäßiger als beim einfachen Mäander.

### 8.2 Grundmuster für Rechteckfeld

Für ein Rechteckfeld mit Rastergrenzen:

```text
left, right, top, bottom
```

wird eine äußere Spur erzeugt:

```text
(start unten links)
links hoch
oben nach rechts
rechts runter
unten nach innen
nächster Ring
...
```

Parallel dazu wird eine innere, um eine Rasterrinne versetzte Spur erzeugt.

Pseudocode:

```java
List<GridPoint> rectangularSpiral(int left, int right, int top, int bottom, int step) {
    List<GridPoint> path = new ArrayList<>();

    int l = left;
    int r = right;
    int t = top;
    int b = bottom;

    path.add(new GridPoint(l, b));

    while (l <= r && t <= b) {
        path.add(new GridPoint(l, t));
        path.add(new GridPoint(r, t));
        path.add(new GridPoint(r, b));

        int nl = l + step;
        int nr = r - step;
        int nt = t + step;
        int nb = b - step;

        if (nl <= nr && nt <= nb) {
            path.add(new GridPoint(nl, b));
            path.add(new GridPoint(nl, nb));
        }

        l = nl;
        r = nr;
        t = nt;
        b = nb;
    }

    return simplify(path);
}
```

### 8.3 Bifilare Spirale

Eine bifilare Spirale besteht aus:

- Vorlaufspur
- innerer Verbindung
- Rücklaufspur

```java
PatternCandidate createBifilarSpiral(RectField field) {
    List<GridPoint> supply = rectangularSpiral(field.left(), field.right(), field.top(), field.bottom(), 2);
    List<GridPoint> ret = rectangularSpiral(field.left()+1, field.right()-1, field.top()+1, field.bottom()-1, 2);

    GridPoint a = last(supply);
    GridPoint b = last(ret);

    ArcSegment bridge = createHalfCircleBridge(a, b);

    Collections.reverse(ret);

    return new PatternCandidate(supply, bridge, ret);
}
```

### 8.4 Zentrale Verbindung

Wenn Vorlauf und Rücklauf in der Mitte zusammentreffen, muss die Verbindung korrekt sein:

- bevorzugt Halbkreis
- kein zufälliger Viertelkreis
- kein L-Stück, wenn ein Halbkreis geometrisch nötig ist
- Radius >= Mindestbiegeradius
- im Raum liegend
- keine Kollision mit anderen Segmenten

Validierung:

```java
validateBridge(ArcSegment bridge) {
    assert bridge.radiusMm() >= minBendRadiusMm;
    assert room.containsArc(bridge);
    assert !intersectsAnyPipe(bridge);
}
```

### 8.5 Invertierung eines Heizkreises

Ein Heizkreis kann invertiert werden:

```java
HeatingCircuit invert(HeatingCircuit c) {
    // Vorlauf und Rücklauf tauschen
    // Start und Ende tauschen
    // Farben tauschen
    // Anschlussports tauschen oder neu routen
}
```

Diese Funktion ist wichtig für die Nachbarschaftsoptimierung.

---

## 9. Meander-Muster

Meander ist sekundär zu unterstützen.

### 9.1 Einfacher Mäander

```java
List<GridPoint> createMeander(RectField field, Direction direction) {
    List<GridPoint> path = new ArrayList<>();
    boolean up = true;

    for (int x = field.left(); x <= field.right(); x += 1) {
        if (up) {
            path.add(new GridPoint(x, field.bottom()));
            path.add(new GridPoint(x, field.top()));
        } else {
            path.add(new GridPoint(x, field.top()));
            path.add(new GridPoint(x, field.bottom()));
        }

        if (x < field.right()) {
            path.add(new GridPoint(x + 1, up ? field.top() : field.bottom()));
        }

        up = !up;
    }

    return simplify(path);
}
```

### 9.2 Mäander mit integrierter Rückführung

Ein einfacher Mäander endet oft auf der falschen Seite. Daher muss eine Rückführgasse eingeplant werden.

Regeln:

- Nur gerade Gassenzahlen, wenn Start und Ende auf gleicher Seite liegen sollen.
- Alternativ eine freie Rückführgasse reservieren.
- Rückführgasse muss vor Erzeugung des Heizfeldes reserviert werden.
- Rückführung darf nicht nachträglich durch das Feld gezogen werden.

### 9.3 Wann Meander verwenden?

Meander eignet sich:

- für sehr schmale Felder
- für Randzonen
- für Felder, in denen keine Spirale möglich ist
- als Fallback, wenn Spiralvalidierung fehlschlägt

---

## 10. Anschlussrouting

### 10.1 Grundidee

Anschlussleitungen sind eigene Pfade im Rastergraphen.

```java
List<GridPoint> findPath(
    GridPoint start,
    GridPoint goal,
    Set<GridEdge> blockedEdges,
    Predicate<GridEdge> allowedEdge
)
```

Parameter:

- `start`: HKV-Port
- `goal`: Heizkreisstart oder Heizkreisende
- `blockedEdges`: bereits belegte Heizfeld- und Anschlusskanten
- `allowedEdge`: Korridorregel

### 10.2 Korridore

Anschlussleitungen dürfen nur laufen in:

- reservierten Anschlusskorridoren
- explizit freien Randrinnen
- benutzerdefinierten Korridoren
- vom Solver automatisch freigehaltenen Rinnen

Sie dürfen nicht laufen:

- durch Heizfelder
- auf Spiralkanten
- in Wendebereichen anderer Kreise
- durch Leerbereiche
- außerhalb des Raumes

### 10.3 Paarweises Routing

VL und RL eines Heizkreises sollten gemeinsam geplant werden.

```java
ConnectorPair routePair(
    ManifoldPair pair,
    CircuitEndpoint supplyEndpoint,
    CircuitEndpoint returnEndpoint,
    GridGraph graph,
    Set<GridEdge> blocked
)
```

Bewertung:

- beide Pfade existieren
- Pfade kreuzen sich nicht
- Pfade sind kurz
- Pfade liegen in erlaubten Korridoren
- Pfade enden am richtigen Heizkreis
- Pfade enden am richtigen HKV-Paar

### 10.4 HKV-Paare

Jedes Heizkreispaar:

```text
Paarbreite: 50 mm
VL-Port: Paarmitte - 10..15 mm
RL-Port: Paarmitte + 10..15 mm
```

Das Rendering darf die 50-mm-Ports darstellen, auch wenn das Verlegeraster 100 mm beträgt. Wichtig ist aber, dass der Übergang vom HKV-Port zum 100-mm-Raster geometrisch sauber und eindeutig ist.

---

## 11. Globale Orientierungsoptimierung

### 11.1 Problem

Bei angrenzenden Heizkreisen kann es passieren, dass an der gemeinsamen Grenze zweimal Vorlauf oder zweimal Rücklauf liegt.

Anforderung:

- Wenn zwei Heizkreise aneinandergrenzen und VL/VL oder RL/RL nebeneinander liegen, soll einer der Kreise invertiert werden, sofern dadurch global weniger Konflikte entstehen.

### 11.2 Graphmodell

Erzeuge einen Nachbarschaftsgraphen:

```java
public final class CircuitAdjacencyGraph {
    Map<CircuitId, List<NeighborRelation>> neighbors;
}
```

Eine Kante enthält:

- gemeinsamer Grenzabschnitt
- Seite des Feldes
- dort anliegende Rohrrolle bei nicht invertierter Orientierung
- Konfliktkosten

### 11.3 Optimierung

Jeder Heizkreis hat zwei Zustände:

```text
0 = normal
1 = invertiert
```

Für jede Nachbarschaft wird eine Kostenfunktion berechnet.

```java
int cost(Circuit a, boolean invA, Circuit b, boolean invB) {
    Role roleA = roleAtBoundary(a, invA, boundary);
    Role roleB = roleAtBoundary(b, invB, boundary);

    if (roleA == roleB) return HIGH_COST;
    return 0;
}
```

Gesucht ist eine Belegung aller Inversionsvariablen mit minimalen Kosten.

Für wenige Heizkreise kann brute force genutzt werden:

```java
for (int mask = 0; mask < (1 << n); mask++) {
    evaluate(mask);
}
```

Für viele Heizkreise:

- simulated annealing
- local search
- graph cut, falls Kostenfunktion geeignet
- ILP/CP-SAT optional

### 11.4 Ergebnis

Nach der Optimierung:

- invertierte Kreise neu erzeugen
- Farben anpassen
- Start/Endpunkte anpassen
- Anschlussrouting neu berechnen

---

## 12. Validierung

Die Validierung ist zentral. Sie muss vor jedem Rendering laufen.

### 12.1 Validierungsbericht

```java
public final class ValidationReport {
    boolean valid;
    List<ValidationError> errors;
    List<ValidationWarning> warnings;
}
```

Fehlerarten:

```java
enum ValidationErrorType {
    PIPE_OUTSIDE_ROOM,
    PIPE_IN_HOLE,
    DUPLICATE_GRID_EDGE,
    GEOMETRIC_INTERSECTION,
    INVALID_BEND_RADIUS,
    CONNECTOR_NOT_CONNECTED,
    CONNECTOR_ENDS_IN_NOTHING,
    MANIFOLD_PAIR_OVERLAP,
    CIRCUIT_TOO_LONG,
    CIRCUIT_TOO_SHORT,
    INVALID_FIELD_BOUNDARY,
    FIELD_OUTSIDE_ROOM,
    UNROUTABLE_CONNECTOR,
    ADJACENT_SAME_FLOW_UNRESOLVED
}
```

### 12.2 Kantenvalidierung

```java
void validateUniqueEdges(Collection<PipeSegment> segments) {
    Set<GridEdge> used = new HashSet<>();

    for (PipeSegment segment : segments) {
        for (GridEdge edge : segment.occupiedGridEdges()) {
            if (!used.add(edge)) {
                error(DUPLICATE_GRID_EDGE, edge);
            }
        }
    }
}
```

### 12.3 Geometrische Kreuzungsprüfung

Nicht nur GridEdges prüfen. Auch gerenderte Linien und Bögen prüfen:

```java
void validateGeometryIntersections(List<PipeSegment> segments) {
    for (int i = 0; i < segments.size(); i++) {
        for (int j = i + 1; j < segments.size(); j++) {
            if (sameCircuitAllowedTouch(segments.get(i), segments.get(j))) continue;
            if (intersects(segments.get(i), segments.get(j))) {
                error(GEOMETRIC_INTERSECTION, i, j);
            }
        }
    }
}
```

Wichtig:

- Berührung an direkt verbundenen Segmenten desselben Rohres ist erlaubt.
- Kreuzung fremder Kreise ist verboten.
- Kreuzung von Anschlussleitung und Heizfeld ist verboten.
- Bogen/Linie und Bogen/Bogen müssen geprüft werden.

### 12.4 Anschlussvalidierung

```java
void validateConnectors(HeatingCircuit c) {
    assert c.supplyConnector.start().equals(c.manifoldPair.supplyPort());
    assert c.supplyConnector.end().equals(c.supplyStart());

    assert c.returnConnector.start().equals(c.returnEnd());
    assert c.returnConnector.end().equals(c.manifoldPair.returnPort());
}
```

### 12.5 Raumvalidierung

```java
void validateInsideRoom(PipeSegment segment) {
    if (segment instanceof LineSegment line) {
        assert room.containsSegment(line.start(), line.end());
    }
    if (segment instanceof ArcSegment arc) {
        assert room.containsArc(arc);
    }
}
```

### 12.6 Biegeradius

```java
void validateBendRadius(ArcSegment arc) {
    assert arc.radiusMm() >= config.minBendRadiusMm();
}
```

### 12.7 Flächenabdeckung

Die Flächenabdeckung ist eine Warnung oder Optimierungsgröße.

```java
double coveredArea = estimateCoveredArea(layout);
double usableArea = estimateUsableArea(room);
double ratio = coveredArea / usableArea;

if (ratio < config.minCoverageRatio()) {
    warning(LOW_COVERAGE, ratio);
}
```

---

## 13. Interaktive Bedienung

### 13.1 Benutzeraktionen

Der spätere Benutzer soll:

- Raumumriss als Polygon eingeben oder importieren
- HKV verschieben
- Heizkreisgrenzen als Rechteck zeichnen
- Heizkreisgrenzen verschieben
- Heizkreisgrenzen skalieren
- Eckpunkte von Polygonfeldern verschieben
- Heizkreis invertieren
- Muster ändern: Auto / Spirale / Meander
- Anschlusskorridore setzen
- Anschlusskorridore sperren
- Ergebnis neu rendern
- Warnungen/Fehler anzeigen

### 13.2 Reaktive Architektur

Ein Frontend kann z. B. so arbeiten:

```text
state.roomPolygon
state.manifoldPosition
state.manualFields
state.lockedFields
state.connectorCorridors
state.config
```

Bei jeder Änderung:

```text
debounce 100..300 ms
→ solve()
→ validate()
→ render()
```

Bei ungültigem Layout:

- altes gültiges Layout anzeigen
- neue Fehler als Overlay markieren
- keine falsche neue Zeichnung als gültig darstellen

### 13.3 Editierbare Heizkreisgrenzen

```java
public final class UserEditableField {
    String id;
    Geometry geometry;
    PatternType preferredPattern;
    boolean locked;
}
```

Wenn `locked = true`:

- Solver darf das Feld nicht verschieben
- darf aber Fehler melden, wenn es unbrauchbar ist

Wenn `locked = false`:

- Solver darf das Feld geringfügig an Raster und Randabstände anpassen

---

## 14. Rendering

### 14.1 SVG-Ebenen

Empfohlene Renderreihenfolge:

1. Hintergrund
2. Raumumriss
3. Ausschnitte
4. Noppen
5. reservierte Korridore
6. Heizkreisfelder
7. Anschlussleitungen
8. Spiral-Vorlauf
9. Spiral-Rücklauf
10. Brücken/Halbkreise
11. HKV
12. Beschriftungen
13. Fehler-Overlay

### 14.2 Farben

- Vorlauf: Blau
- Rücklauf: Rot
- Brücke VL/RL: Violett oder neutral
- reservierte Korridore: Grau gestrichelt
- Fehler: Rot/Orange hervorgehoben

### 14.3 Rohrbreite

Rohrbreite im SVG:

```text
stroke-width = pipeDiameterMm
```

### 14.4 Noppen

Noppen:

```text
Noppendurchmesser = pitch - pipeDiameter
```

Beispiel:

```text
pitch = 100 mm
pipeDiameter = 11.6 mm
knobDiameter = 88.4 mm
knobRadius = 44.2 mm
```

---

## 15. Beispielcode: Kernklassen

### 15.1 LayoutInput

```java
public record LayoutInput(
    Polygon room,
    List<Polygon> holes,
    Manifold manifold,
    List<UserEditableField> manualFields,
    List<UserCorridor> manualCorridors,
    LayoutConfig config
) {}
```

### 15.2 LayoutConfig

```java
public record LayoutConfig(
    double pitchMm,
    double pipeDiameterMm,
    double minBendRadiusMm,
    double maxCircuitLengthMm,
    double minCircuitLengthMm,
    double manifoldPairPitchMm,
    double manifoldPairWidthMm,
    PatternPreference patternPreference
) {}
```

### 15.3 PatternCandidate

```java
public final class PatternCandidate {
    private final String id;
    private final CircuitField field;
    private final PatternType type;
    private final List<PipeSegment> supplySegments;
    private final List<PipeSegment> returnSegments;
    private final List<PipeSegment> bridgeSegments;
    private final Set<GridEdge> occupiedEdges;
    private final PointMm supplyStart;
    private final PointMm returnEnd;
    private final double lengthMm;
    private final boolean invertible;

    public PatternCandidate inverted() { ... }
}
```

### 15.4 ConnectorRouter

```java
public final class ConnectorRouter {
    public ConnectorPair routePair(
        ManifoldPair pair,
        PatternCandidate candidate,
        GridGraph graph,
        Set<GridEdge> blocked,
        List<Corridor> corridors
    ) {
        List<GridPoint> supply = graph.shortestPath(
            pair.supplyGridPoint(),
            candidate.supplyStartGridPoint(),
            blocked,
            edge -> corridorsAllow(edge, corridors)
        );

        Set<GridEdge> blocked2 = new HashSet<>(blocked);
        blocked2.addAll(edgesOf(supply));

        List<GridPoint> ret = graph.shortestPath(
            pair.returnGridPoint(),
            candidate.returnEndGridPoint(),
            blocked2,
            edge -> corridorsAllow(edge, corridors)
        );

        return new ConnectorPair(supply, ret);
    }
}
```

---

## 16. Beispielcode: Validierung

```java
public final class LayoutValidator {

    public ValidationReport validate(Layout layout, RoomGeometry room) {
        ValidationReport report = new ValidationReport();

        validateAllSegmentsInsideRoom(layout, room, report);
        validateUniqueGridEdges(layout, report);
        validateGeometricIntersections(layout, report);
        validateConnectorContinuity(layout, report);
        validateManifoldPairs(layout, report);
        validateCircuitLengths(layout, report);
        validateBendRadii(layout, report);
        validateAdjacencyFlow(layout, report);

        return report;
    }
}
```

### 16.1 Keine losen Enden

```java
private void validateConnectorContinuity(Layout layout, ValidationReport report) {
    for (HeatingCircuit c : layout.circuits()) {
        if (!c.supplyConnector().start().equals(c.manifoldPair().supplyPort())) {
            report.error(CONNECTOR_NOT_CONNECTED, c.id());
        }
        if (!c.supplyConnector().end().equals(c.supplyStart())) {
            report.error(CONNECTOR_ENDS_IN_NOTHING, c.id());
        }
        if (!c.returnConnector().start().equals(c.returnEnd())) {
            report.error(CONNECTOR_ENDS_IN_NOTHING, c.id());
        }
        if (!c.returnConnector().end().equals(c.manifoldPair().returnPort())) {
            report.error(CONNECTOR_NOT_CONNECTED, c.id());
        }
    }
}
```

---

## 17. Testvorgaben

### 17.1 Unit-Tests für Geometrie

- Punkt in Polygon
- Segment vollständig im Polygon
- Segment schneidet Polygonkante
- Bogen vollständig im Polygon
- Bogen schneidet Hindernis
- Rasterkante normalisiert gleich in beiden Richtungen
- Schnitt Linie/Linie
- Schnitt Linie/Bogen
- Schnitt Bogen/Bogen

### 17.2 Unit-Tests für Spiralpattern

Testfälle:

1. Rechteck 2400 x 2600 mm
2. Rechteck 1200 x 2600 mm
3. Rechteck 3600 x 1600 mm
4. sehr schmales Rechteck
5. Feld zu klein für Spirale

Prüfungen:

- kein Segment außerhalb des Feldes
- keine doppelte GridEdge
- supplyStart am Feldrand
- returnEnd am Feldrand
- zentrale Verbindung ist gültig
- Biegeradius ausreichend
- Länge plausibel
- invertierte Variante hat dieselben Kanten, aber getauschte Rollen

### 17.3 Unit-Tests für Meander

- gerade Gassenzahl endet auf Startseite
- ungerade Gassenzahl endet gegenüberliegend
- Rückführgasse wird reserviert
- keine Rückführung durch belegte Gassen

### 17.4 Tests für Anschlussrouting

- einfacher Raum, HKV unten
- HKV links
- HKV oben
- L-Raum
- U-Raum
- mehrere Heizkreise
- blockierte Korridore
- kein Pfad möglich

Prüfungen:

- jeder Pfad beginnt am HKV-Port
- jeder Pfad endet am korrekten Heizkreis
- Pfad liegt im Raum
- Pfad benutzt nur erlaubte Kanten
- Pfad kreuzt kein Heizfeld
- VL/RL eines Paares sind getrennt
- Paare am HKV überlappen nicht

### 17.5 Integrationstests

#### Test 1: Rechteckraum

```text
Raum: 7000 x 5000 mm
HKV: unten mittig
Erwartung:
- mehrere Spiralheizkreise
- alle < 80 m
- keine Doppelbelegung
- hohe Flächenabdeckung
```

#### Test 2: L-Raum

```text
Raum:
(0,0), (7000,0), (7000,3000), (4200,3000), (4200,5000), (0,5000)

Erwartung:
- obere Felder und unterer zusammenhängender Bereich
- keine Leitung durch L-Leerbereich
- unterer Bereich nicht unnötig in Mini-HKs zerstückelt
- Anschlusskorridore bleiben frei
```

#### Test 3: U-Raum

```text
Raum mit Innenausschnitt
Erwartung:
- keine Leitung durch Innenausschnitt
- Anschlussrouting findet zulässige Wege oder meldet Fehler
```

#### Test 4: manuelle Felder

```text
Benutzer gibt 3 Rechtecke vor
Erwartung:
- Felder werden validiert
- zu lange Felder werden gesplittet oder gemeldet
- Benutzeränderung löst Neuberechnung aus
```

#### Test 5: HKV verschieben

```text
HKV von unten nach links verschieben
Erwartung:
- Anschlusswege werden neu geroutet
- Paarzuordnung bleibt korrekt
- keine alten Anschlussleitungen bleiben erhalten
```

### 17.6 Snapshot-Tests

Für definierte Eingaben soll das SVG als Snapshot verglichen werden:

- gleiche Heizkreislängen
- gleiche Anzahl Heizkreise
- keine Validierungsfehler
- definierte Layer vorhanden
- VL/RL-Farben vorhanden
- HKV-Paare vorhanden

### 17.7 Property-Based Tests

Zufällige orthogonale Polygone generieren:

- Solver darf entweder gültiges Layout liefern oder sauber „kein gültiges Layout“ melden.
- Solver darf nie ungültiges Layout als gültig ausgeben.
- Keine Exceptions ohne erklärbaren ValidationReport.

---

## 18. Fehler, die explizit verhindert werden müssen

Diese Liste stammt aus typischen Fehlverhalten und muss als Regressionstest aufgenommen werden.

1. Vier Rohre übereinander in derselben Rinne.
2. Anschlussleitung liegt quer in Wendegasse.
3. Anschlussleitung läuft durch L-Leerbereich.
4. Rückführung wird nachträglich durch Heizfeld gezogen.
5. Vorlauf und Rücklauf enden im Nichts.
6. HKV-Paare liegen übereinander.
7. HKV wird unrealistisch groß gezeichnet.
8. Mini-Heizkreise entstehen, obwohl Zusammenfassung möglich ist.
9. Zentrale Verbindung wird als falscher Viertelkreis gezeichnet.
10. Halbkreis hat falsche Richtung.
11. Leere Flächen entstehen durch zu breite Reserven.
12. Feldgrenzen werden aus Eingaberechtecken blind übernommen.
13. Manuelle Benutzeränderungen werden ignoriert.
14. Kreisrichtung wird geändert, aber Farben werden nicht angepasst.
15. SVG zeigt etwas anderes als intern validiert wurde.
16. Geometrische Kreuzung wird nicht erkannt, weil nur GridEdges geprüft wurden.
17. Nicht-gridige HKV-Portsegmente überlagern sich grafisch.
18. Zu-/Rückführung endet nicht am zugehörigen HKV-Paar.

---

## 19. Empfohlene Modulstruktur

```text
de.example.fbh
 ├── geometry
 │   ├── PointMm.java
 │   ├── PolygonGeometry.java
 │   ├── SegmentIntersection.java
 │   └── ArcGeometry.java
 ├── grid
 │   ├── GridGraph.java
 │   ├── GridPoint.java
 │   ├── GridEdge.java
 │   └── GridBuilder.java
 ├── model
 │   ├── LayoutInput.java
 │   ├── LayoutConfig.java
 │   ├── HeatingCircuit.java
 │   ├── Manifold.java
 │   ├── ManifoldPair.java
 │   └── CircuitField.java
 ├── partition
 │   ├── AutoPartitioner.java
 │   ├── ManualFieldNormalizer.java
 │   └── FieldSplitter.java
 ├── pattern
 │   ├── SpiralPatternGenerator.java
 │   ├── MeanderPatternGenerator.java
 │   └── PatternCandidate.java
 ├── routing
 │   ├── ConnectorRouter.java
 │   ├── ManifoldAllocator.java
 │   └── CorridorPlanner.java
 ├── optimize
 │   ├── CircuitSelectionOptimizer.java
 │   ├── FlowOrientationOptimizer.java
 │   └── CostFunction.java
 ├── validate
 │   ├── LayoutValidator.java
 │   ├── ValidationReport.java
 │   └── ValidationError.java
 └── render
     ├── SvgRenderer.java
     ├── RenderModel.java
     └── Layer.java
```

---

## 20. Akzeptanzkriterien

Ein Coding-Agent soll die Implementierung erst als fertig melden, wenn folgende Kriterien erfüllt sind.

### 20.1 Funktional

- Raum als geschlossenes Polygon wird akzeptiert.
- HKV ist verschiebbar.
- Heizkreisfelder können automatisch erzeugt werden.
- Heizkreisfelder können manuell als Rechtecke/Polygone vorgegeben werden.
- Manuelle Felder können verändert werden.
- Änderungen lösen Neuberechnung aus.
- Spiralvariante funktioniert.
- Meandervariante funktioniert als Fallback.
- VL/RL werden farbgetrennt dargestellt.
- HKV-Paare werden korrekt dargestellt.
- Validierungsbericht wird erzeugt.

### 20.2 Geometrisch

- keine Leitung außerhalb des Raums
- keine Leitung durch Ausschnitte
- keine doppelte Rasterkante
- keine geometrische Kreuzung
- keine losen Enden
- keine unzulässigen Bögen
- keine falschen Halbkreis-/Viertelkreis-Verbindungen

### 20.3 Hydraulisch/planerisch

- alle Heizkreise unter Max-Länge
- keine unnötigen Mini-Heizkreise
- gleichmäßige Längen, soweit möglich
- benachbarte VL/RL-Konflikte minimiert
- sinnvolle Anschlusswege

### 20.4 UI/Interaktion

- HKV verschiebbar
- Felder verschiebbar
- Feldgrenzen editierbar
- Live-Neurendering
- Fehler-Overlay
- Validierungsdetails anklickbar

---

## 21. Hinweise für den Coding-Agent

1. Nicht mit Rendering beginnen. Zuerst Datenmodell und Validierung.
2. SVG-Ausgabe erst erzeugen, wenn `ValidationReport.valid == true`.
3. Kein Sonderfall darf nur grafisch gelöst sein; jede sichtbare Leitung muss im Modell existieren.
4. Nicht nur GridEdges prüfen. Gerenderte Bögen und HKV-Portsegmente müssen ebenfalls in die geometrische Validierung.
5. Anschlussleitungen müssen als echte Pfade modelliert sein.
6. Manuelle Benutzeränderungen müssen in das Modell zurückfließen.
7. Jede Optimierung muss deterministisch testbar sein.
8. Für unlösbare Fälle muss ein guter Fehlerbericht erzeugt werden.
9. Kein Layout als „gültig“ markieren, wenn nur eine Heuristik erfolgreich war.
10. Regressionstests für alle oben genannten Fehlerfälle anlegen.

---

## 22. Minimaler erster Implementierungsplan

### Phase 1: Kernmodell

- PointMm
- GridPoint
- GridEdge
- RoomGeometry
- GridGraph
- PipeSegment
- HeatingCircuit
- Manifold

### Phase 2: Validierung

- inside-room
- duplicate-edge
- connector-continuity
- circuit-length
- bend-radius
- basic intersections

### Phase 3: Spiralpattern

- Rechteckspirale
- bifilare Spirale
- Halbkreisbrücke
- Invertierung
- farbgetrennte Rollen

### Phase 4: Partitionierung

- manuelle Rechteckfelder
- automatische horizontale Bänder
- Split nach Länge
- Zusammenfassung kleiner Restfelder

### Phase 5: Routing

- HKV-Paare
- Anschlusskorridore
- Dijkstra/A*
- paarweises VL/RL-Routing

### Phase 6: Optimierung

- Längenoptimierung
- Restflächenoptimierung
- Nachbarschaftsorientierung
- Anschlusskosten

### Phase 7: Interaktive UI-Anbindung

- RenderModel
- SVG/Canvas Layer
- Dragging von HKV
- Dragging von Feldgrenzen
- Live-Recompute
- Fehler-Overlay

---

## 23. Definition of Done

Das Projekt gilt erst als fertig, wenn:

```text
mvn test
```

oder die entsprechende Gradle-Testausführung alle Tests besteht und die folgenden Beispielräume erfolgreich layoutet:

- Rechteckraum
- L-Raum
- U-Raum
- Raum mit Ausschnitt
- Raum mit manuell vorgegebenen Feldern
- Raum nach HKV-Verschiebung
- Raum nach Feldgrenzen-Verschiebung

Außerdem muss für jedes erzeugte Layout gelten:

```text
ValidationReport.valid == true
```

und kein Rendering darf erzeugt werden, wenn diese Bedingung nicht erfüllt ist.

---

## 24. Zusammenfassung

Der Layouter ist kein Zeichenprogramm, sondern ein geometrisch validierter Solver. Die wichtigste Regel lautet:

> Erst vollständig modellieren und validieren, dann rendern.

Insbesondere müssen Zu-/Rückführungen, HKV-Paare, Bögen, Halbkreise und interaktive Änderungen im gleichen Modell existieren wie die Heizfeldrohre. Alles, was sichtbar ist, muss auch validiert werden.

Der bevorzugte Musteransatz ist die bifilare Spirale, weil der Rücklauf dort Bestandteil des Musters ist. Meander bleibt als Fallback und Sonderfall erhalten, darf aber nur mit vorher eingeplanter Rückführgasse verwendet werden.

