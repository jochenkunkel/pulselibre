# CIRQA: Nächster sicherer Gerätetest (vollständige historische Daten)

Stand: 2026-11-16. Ziel: **alle zugänglichen historischen Daten**, unabhängig von Dateityp oder Messgröße.

## Warum jetzt ein Gerätetest nötig ist

Der bisherige FileSync-V2-Dateilistenversuch erhielt dreimal GFDI-5000 `UNSUPPORTED` für Request `0x8A39`. Ein grüner Android-Build beweist keine CIRQA-Protokollunterstützung. Weitere aktive, vermutete GFDI-Kommandos ohne Gerätebeobachtung sind nicht vertretbar. Die aktuell vorhandenen ACK-Encoder, Paginierungsanfragen und Reassembler bleiben offline bzw. passiv.

## Freigabekriterien vor dem Test

- Neuester `main`-Build und Unit-Tests grün.
- Keine automatische Übertragung von FileSync-, FileSetFlags-, Reset- oder CLOSE_ALL-Befehlen.
- Passive Diagnostik erfasst CRC-validierte GFDI-Typen, Geräteinformationen, Statusantworten und Transport-Metadaten ohne Geräteadressen oder Gesundheitswerte.
- Eindeutige Anzeige, ob Dateiliste/Dateiübertragung **tatsächlich** beobachtet oder nur offline simuliert wurde.

## Benötigte Benutzeraktion (ca. 5–10 Minuten)

1. Die APK des neuesten **erfolgreichen** CI-Builds auf dem Android-Gerät installieren.
2. Garmin CIRQA koppeln/verbinden und andere aktive CIRQA-Verbindungen möglichst beenden.
3. In PulsLibre ausschließlich die **passive CIRQA-Historien-Diagnose** ausführen, nicht die experimentelle Dateilistenanfrage.
4. Den vollständigen Text des Diagnoseberichts kopieren und in den Entwicklungs-Chat senden. Keine Geräteadressen, Schlüssel oder privaten Messwerte posten.

## Entscheidung nach dem Bericht

- **Geräteinformationen / weitere GFDI-Typen sichtbar:** Anhand beobachteter Sequenzen Bootstrap-Hypothesen und rein lesende Testanfragen weiter eingrenzen.
- **Nur UNSUPPORTED oder keine neuen Protokollnachrichten:** Keine spekulative Übertragung; zunächst CIRQA-Firmware-/Protokollunterschiede und weitere öffentlich belegte Implementierungen untersuchen.
- **FileListResponse beobachtet:** Paginierung und vollständigen Rohdateiabruf implementieren; kein Filter auf Herzfrequenz, Aktivität oder FIT.

## Noch nicht implementiert / nicht nachgewiesen

Aktive CIRQA-kompatible Bootstrap-Sequenz, verifizierte kompakte Chunk-Reassembly, vollständige Dateiliste inklusive Cursor-Ende, Datei-Download, dauerhafte Rohdatenspeicherung, Wiederaufnahme und Vollständigkeitsnachweis. Diese Punkte dürfen nicht als fertig oder am Gerät bestätigt gemeldet werden.
