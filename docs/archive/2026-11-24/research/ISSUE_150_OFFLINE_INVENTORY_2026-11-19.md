# #150 – Offline-Analyse: implementierter Teilstand und sichere Abnahme

Stand: 2026-11-19. **Kein Zugriff auf die privaten S25-Dateien in diesem Durchgang.** Die früher dokumentierten 31 FIT + 2 Raw und 294 SQLite-Zeilen sind historische Testbefunde, keine in diesem Durchgang neu gemessenen Werte.

## Bereits vorhandene produktive Bausteine
- `CirqaVerifiedFitStore`: app-private, content-adressierte und FIT-CRC-validierte Originaldateien.
- `FitHistoricalObservations`: verifizierter Offline-Decoder mit Messwert- und whitelisted Device-Info-Feldern.
- `CirqaFitBackfill`: vorhandener idempotenter Importpfad über `FitObservationStore.commitFitFile`, ohne BLE.
- `CirqaRawDeviceMetadataParser`: gesonderter Raw-Metadatenpfad. Raw-Dateien **nicht** als FIT interpretieren.

## Neu: `CirqaFitOfflineInventory`
Eine **read-only** Inventur für das bereits vorhandene app-private `verified_fit`-Verzeichnis:
- Nur reguläre `.fit`-Dateien, keine Symlinks, max. 64 MiB pro Datei.
- SHA-256-Dateiname und verifizierter FIT-Envelope erforderlich.
- Anzahl gültiger/abgewiesener Dateien, Records, dekodierter und importierbarer Messwerte, fehlender Zeitstempel, außerhalb der Anzeigegrenzen liegender Werte, Device-Info-Records und Messwertzählung nach Metrik.
- `Summary.redactedText()` enthält **keine** Dateinamen, Hashes, Gerätekennungen, Zeitreihen, einzelnen Werte oder Geräte-IDs.
- **Keine** SQLite-Schreiboperation, **kein** Bluetooth, **kein** Reimport und **keine** Änderung an gespeicherten Originalen.

## Grenzen und notwendige nächste Schritte
1. Auf dem autorisierten S25 prüfen, ob `verified_fit` tatsächlich erreichbar ist; `adb devices -l` und Zugriffsmethode dokumentieren.
2. Vorher den Zustand der Originale und DB in geschützter Umgebung sichern, niemals in Git hochladen.
3. Inventur im App-Kontext auf einem Worker-Thread ausführen und nur redigierte Summen exportieren.
4. Erwartete importierbare Observations **gegen reale SQLite-Daten** abgleichen; die Inventur selbst ist **noch kein** Reconciliation-Bericht.
5. Bestehenden `CirqaFitBackfill` ggf. kontrolliert erneut ausführen, idempotenten Zweitlauf testen.
6. Batterie, Firmware, Bandzeit und Geräteidentität nur mit **belegten** Quellen aus gespeicherten FIT-/Raw-/BLE-Daten auswerten; Parserdaten sind nicht automatisch aktuelle Live-Gerätewerte.
7. Hardware- und Build-Tests sowie Freigaben bleiben beim zuständigen OpenCode-/Geräte-Agenten.

**Kein Ticketabschluss ohne belegten S25-Abgleich und Testresultate.**
