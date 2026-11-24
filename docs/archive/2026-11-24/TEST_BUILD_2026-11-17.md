# PulsLibre – interner Android-Testbuild (17.11.2026)

**Status:** Testbuild, **kein** öffentliches Produktionsrelease. Quellstand: `df53e1840f48f15c5a8d0fa7391e2464f390c9d3`.

## APK

- [Erfolgreicher Android-CI-Lauf mit signierter APK](https://github.com/jochenkunkel/pulselibre/actions/runs/44066866520)
- Unter **Artifacts**: `pulselibre-signed-update-apk` (ZIP); darin `app-debug.apk`.
- Build-Job: **success**. Der API-35-Emulatorjob wurde bei diesem normalen main-Build **nicht ausgeführt**.
- Die APK wurde mit dem stabilen CI-Schlüssel signiert. Ein In-Place-Update ist nur möglich, wenn die bereits installierte App dieselbe Signatur verwendet und ihr `versionCode` kleiner ist. Bei Signaturkonflikten **nicht unüberlegt deinstallieren**: Eine Deinstallation löscht lokale App-Daten.

## Was geprüft werden kann

1. App starten und grundlegende Navigation/Anzeige prüfen.
2. Vorhandene passive CIRQA-Diagnose und vorhandene Messwertanzeige prüfen, soweit bereits genutzt.
3. Diagnosebericht auf neue 5050-/5037-Evidenzfelder prüfen; für Bluetooth-Gerätetests gelten die bestehenden Sicherheitsgrenzen.
4. Fehlerbericht mit Android-Version, App-Build und konkreten Schritten melden; keine privaten Rohdaten ungefragt veröffentlichen.

## Nicht als funktionsfähig behaupten

- Historischer CIRQA-Bewegungs-/Schrittdatenabruf **nicht nachgewiesen**.
- SYNC-Bit-3-Experiment **nicht aktiviert**; kein automatischer 5037-Dateidownload.
- Kein erfolgreiches Upgrade auf einem echten Gerät nachgewiesen.
- Kein vollständiger Lint-/Emulator-/Hardware-Release-Gate für diesen Build.

## Freigabekriterien für ein späteres offizielles Release

- Vollständige CI-Prüfung einschließlich Lint und Emulator.
- Installations- und Upgrade-Test auf dem Zielgerät.
- Dokumentierte, explizite Einwilligung vor aktiven CIRQA-SYNC-Experimenten.
- Verifizierte historische Messdaten inklusive Zeitstempel und Duplikatbehandlung.

Dieser Build ist für **sofortige manuelle App-Tests** verfügbar, nicht als Nachweis einer abgeschlossenen CIRQA-Historiensynchronisation.
