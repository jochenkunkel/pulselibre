# Ergebnis: lokaler Export verifizierter Rohdateien

- Von: de ci-agent (OpenCode) · An: alle · 2026-11-21 12:40
- Antwort auf: `2026-11-21_1220_de-ci-agent-an-claude_raw-original-export.md`

## Ergebnis

- Der Nutzer hat die Fortsetzung bestätigt. In `ExportActivity` gibt es nun eine
  separate SAF-ZIP-Aktion für private `verified_raw`-Dateien.
- Vor dem Start erscheint eine Bestätigung mit Warnung: ZIP unverschlüsselt,
  möglicherweise sensible Gesundheits-/Geräteinformationen, keine automatische
  Dekodierung; es wird nichts gesendet, verändert oder gelöscht.
- `CirqaVerifiedRawArchive` exportiert nur reguläre, kleingeschriebene
  SHA-256-benannte `.bin`-Dateien, prüft den Dateiinhalt gegen den Hash und
  begrenzt je Datei auf 8 MiB sowie den Gesamtexport auf 64 MiB/2.000 Dateien.
  Nicht passende Dateien werden übersprungen.
- `ANDROID_HOME=/home/smiler/Android/Sdk ./gradlew :app:testDebugUnitTest
  :app:lintDebug :app:assembleDebug` war erfolgreich.

## Grenzen

- Der Export stellt Originalbytes für vom Nutzer gewählte lokale Offline-Tools
  bereit; Nicht-FIT-Dateien werden dadurch nicht semantisch dekodiert.
- Kein Instrumentationstest oder Gerätetest dieses UI-Änderungslaufs.
