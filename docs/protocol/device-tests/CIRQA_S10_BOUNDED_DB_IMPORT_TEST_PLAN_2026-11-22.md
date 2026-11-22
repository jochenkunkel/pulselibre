# S10 – begrenzter Gerätetest: DB-Importnachweis (Plan, vor Ausführung)

Stand: 2026-11-22 08:20 UTC · Freigabe: Nutzer hat Schritt 4 ausdrücklich freigegeben.
**Dies ist die schriftliche Definition von Umfang und Stop-Bedingungen. Ohne diese
Dokumentation darf keine Aktion ausgeführt werden.**

## Ziel

Nachweis, ob PulsLibre auf dem angeschlossenen S10 FIT-Historiendaten tatsächlich
in ihre lokale Datenbank importiert. Offen ist weiterhin die Vollständigkeit.

## Gerät und Voraussetzungen

- Gerät: S10 `RF8M826126Z` (SM_G970F), USB, bereits angeschlossen.
- Installierte App: `de.pulselibre`, `2.0.0_build1757` (passiv geprüft).
- `run-as de.pulselibre` funktioniert **nicht** (nicht debuggable), kein Root.
  Die App besitzt jedoch `ExportActivity` mit SAF-Export (`DatabaseBackup.write`).
- **Kein Instrumentationstest** auf diesem oder einem S25-Gerät. Keine neuen
  BLE-/GATT-/FileSync-Kommandos, kein Reset, kein Entkoppeln, kein 5008/Archive/
  Erase/SetFlags, kein `CLOSE_ALL`.

## Umfang (exakt)

1. **Baseline (read-only):** App-Export über die vorhandene Export-Funktion in
   einen vom Nutzer erreichbaren Ordner ausführen, exportierte Datei mit `adb pull`
   lokal kopieren und offline auf FIT-Importeinträge prüfen. Keine Geräteänderung.
2. **Optional nur danach:** Falls die Baseline unklar bleibt, den **vorhandenen**
   Historien-Download über die bestehende UI starten (keine neuen Kommandos),
   höchstens 5 Minuten laufen lassen, danach erneut exportieren und vergleichen.
3. **Parallel:** HCI-Mitschnitt nur über die bereits bestehende, freigegebene
   Snoop-Funktion; keine Änderung am Transportverhalten.

## Stop-Bedingungen

- Sofortiger Abbruch bei unerwarteten Geräteantworten, Pairing-/Reset-Aufforderung,
  Verbindungsabbruch mit unklarem Zustand oder jeder Aufforderung zu destruktiven
  Kommandos.
- Höchstens 5 Minuten Laufzeit pro Downloadversuch, kein Retry, keine Schleife.
- Kein Schreiben auf das Band, keine Profil-/Einstellungsänderung, keine
  Firmwareaktion, keine Löschung.
- Abbruch bei jeder neuen Unklarheit; keine Eskalation „einfach weiterprobieren".

## Datenschutz

- Export und HCI bleiben ausschließlich lokal im 0700/0600-Backup.
- Keine Roh-BLE-Payloads, Gerätekennungen, Tokens, UIDs oder Gesundheitswerte in
  GitHub oder PR-Kommentaren. Öffentlich nur redaktierte Metadaten (Anzahlen,
  Größen, CRC-Status).
- Kein unanonymisierter Export an Dritte.

## Erwartetes Ergebnis / Grenzen

- Die installierte build1757 enthält den zlib-Decoder aus #416 **nicht verdrahtet**.
  Ein negativer Befund wäre daher kein Beweis gegen die Geräteunterstützung,
  sondern nur gegen den aktuellen Produktpfad.
- Ein positiver Befund (DB-Einträge nach Export) wäre der erste konkrete
  DB-Import-Nachweis. Die Vollständigkeit aller Historien-Dateien bleibt auch
  dann separat offen.
- `coverage=device-history-unverified` bleibt bis zu beiden Nachweisen gültig.

## Build-Stand (passiv aus CI und Git ermittelt, 2026-11-22 09:40 UTC)

- Auf dem S10 wurde zwischenzeitlich `2.0.0_build1781` statt `build1757` festgestellt.
- Build 1781 stammt aus CI-Lauf `45004884778` (08:53 UTC) auf Commit `7e223ad`
  (PR #434, A6-38-Bluetooth-Fix). Der Merge von PR #433 (`6dee48e`, zlib/MLR/
  sync_response-Härtung) erfolgte erst um 09:14 UTC; PR #432 wurde um 09:50 UTC
  gemergt (`341fe20`).
- **Folge:** Build 1781 enthält weder die #433-Verdrahtung noch die #432-
  Reassemblierung. Ein Gerätelauf mit Build 1781 kann den Produktpfad aus
  #432/#433 nicht bestätigen. Vor einem erneuten begrenzten Gerätetest ist ein
  frischer Build vom Stand nach dem #432-Merge nötig.
- Diese Feststellung ist rein passiv (Git/CI-Laufmetadaten); es wurde kein
  Gerätebefehl ausgeführt und kein Installations-/Exportvorgang gestartet.

## Status

**Geplant – nicht ausgeführt.** Ausführung erst nach Abschluss von PR #425
(Offline-Test + volle CI) und nach bestätigter Abwesenheit paralleler Arbeiten
anderer Agenten (agent-sync 08:00). Zusätzlich ist der oben genannte frische
Build nach dem #432-Merge erforderlich.
