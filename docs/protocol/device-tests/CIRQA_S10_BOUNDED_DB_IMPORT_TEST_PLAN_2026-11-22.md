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

## Status

**Geplant – nicht ausgeführt.** Ausführung erst nach Abschluss von PR #425
(Offline-Test + volle CI) und nach bestätigter Abwesenheit paralleler Arbeiten
anderer Agenten (agent-sync 08:00).
