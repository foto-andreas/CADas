# KI2-Recherche

## Befund vom 28.06.2026

Im Projekt `KI2.cadas` entstand im Flur an der Treppe eine nur etwa `40 mm` kurze Mini-Kante.
Die bisherige Höhenableitung im `AutoRoomGenerationService` mittelte an beiden Endpunkten dieser
Kante die nahen Wände und senkte dadurch beide Eckhöhen auf `550 mm` ab.

Fachlich korrekt ist nur eine abgesenkte treppennahe Ecke. Die zweite Ecke muss auf der normalen
Raumhöhe von `2550 mm` bleiben.

## Ursache

Die Ursache lag im Code, nicht in den Projektdaten. Kurze Restsegmente an Treppen- oder
Wandanschlüssen wurden bei der Ableitung der Raum-Eckhöhen nicht gesondert behandelt.

## Korrektur

Die Eckhöhen werden nach der Wandabtastung für sehr kurze Segmente normalisiert. Wenn beide
Endpunkte eines Mini-Segments zunächst dieselbe abgesenkte Höhe erhalten, aber die angrenzenden
Segmente deutlich höher liegen, bleibt nur die zur Raummitte nähere Ecke abgesenkt.

## Absicherung

Der Regressionstest `senktImKi2FlurNurEineTreppennaheMiniEckeAb` prüft, dass im Flur genau eine
Ecke unter `600 mm` liegt und die Spannweite der Deckenhöhen `550 mm` bis `2550 mm` beträgt.
