# Anwendungseinstieg `de.schrell.cadas`

## Verantwortung

Dieses Paket startet CADas, erzeugt die JavaFX-Anwendung und verbindet den Lebenszyklus des
Hauptfensters mit der optionalen lokalen Automatisierungsschnittstelle. Fachliche Berechnungen
oder Dateiformatlogik gehören nicht hierher.

## Zentrale Klassen

* `CadLauncher` ist der für Gradle, Startskripte und Paketierung konfigurierte Einstiegspunkt.
* `CadApplication` verwaltet den JavaFX-Lebenszyklus, das Hauptfenster und das kontrollierte
  Beenden der Anwendung.

## Daten- und Kontrollfluss

1. `CadLauncher` übergibt an JavaFX.
2. `CadApplication.start` erzeugt eine `CadWorkbench` und bindet sie an die primäre Bühne.
3. Wenn die Automatisierung explizit aktiviert ist, wird zusätzlich der nur lokal erreichbare
   HTTP-Server mit einem Bearer-Token gestartet.
4. Beim Beenden werden Fenster, Automatisierungsserver und JavaFX-Laufzeit geordnet geschlossen.

## Invarianten und Review-Hinweise

* Die Automatisierung darf nie ohne starkes Token oder an einer externen Netzwerkschnittstelle
  lauschen.
* Startcode darf keine fachlichen Standardwerte duplizieren; diese gehören in Domäne oder UI.
* Neue Startparameter müssen auch in Gradle-Startskripten und Paketierungsaufgaben geprüft werden.
* Fehler beim Programmstart müssen für Anwender verständlich sichtbar werden und dürfen nicht
  lediglich auf der Konsole verschwinden.
