# Bodenöffnungen

`FloorOpeningGeometryService` bildet rechteckige und kreisförmige Öffnungen als prüfbare
Grundrissgeometrie ab. Diese Ableitung wird von Flächen-, Volumen-, Belags- und 3D-Berechnungen
gemeinsam verwendet.

Rechtecke können gedreht sein; Kreise werden für Polygonoperationen mit definierter Segmentzahl
angenähert. Die fachliche Kreisfläche bleibt dabei mathematisch exakt, wenn nur eine Fläche benötigt
wird. Öffnungen außerhalb ihres Host-Raums dürfen weder dessen Fläche noch Volumen reduzieren.

Reviewer prüfen insbesondere Nullmaße, Tangentialkontakt zur Raumgrenze, vollständige Überdeckung,
gedrehte Rechtecke und die Konsistenz zwischen Polygon- und Flächenableitung.
