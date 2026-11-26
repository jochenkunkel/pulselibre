# Erreichbarkeits-Audit der Produktionsdateien (#661)

Stand: main, 26.11.2026. Erstellt vom GUI-Agenten nach der Projektübernahme.

## Methode

Grundlage ist ein statischer Verweisgraph über alle 306 Kotlin-Dateien in `app/src/main/java/de/pulselibre`.
- **Startpunkte:** alle im `AndroidManifest.xml` deklarierten Komponenten (Activities, Dienst, Application).
- **Kanten:** Verweise auf Klassen, Objekte, Werte und Top-Level-Funktionen anderer Dateien.

Eine Datei gilt als erreichbar, wenn ein Pfad von einem Startpunkt zu ihr führt. Die Analyse ist konservativ: Ein Verweis zählt auch dann, wenn er nur in einem Kommentar steht. Unerreichbar heißt also sicher unbenutzt; erreichbar beweist keine Nutzung zur Laufzeit.

## Ergebnis

- **Erreichbar:** 279 Dateien. Sie gehören zum Produkt und decken die App, den Aufzeichnungsdienst, das Testlabor und Export/Backup ab.
- **Nicht erreichbar:** 27 Dateien mit 1 472 von 37 510 Zeilen, also 3,9 %.

| Gruppe | Dateien | Bewertung / Maßnahme |
|---|---|---|
| Protokoll-Forschung, nur in Tests benutzt | `Cirqa5050ExperimentComparison`, `Cirqa5050OfflineCapabilities`, `Cirqa5050SyncHypothesis`, `CirqaBootstrapGapAnalysis`, `CirqaBoundedLegacyFile`, `CirqaConnect531HandshakeModel`, `CirqaConnect531Host5024Response`, `CirqaConnectDirectoryTransition`, `CirqaConnectionIndicator`, `CirqaFileListCounterGate`, `CirqaLegacy5030To5007Session`, `CirqaLegacyFitFileSelector`, `CirqaLegacyOfflineFileSession`, `CirqaMinuteAggregation`, `CirqaRawDeviceMetadataBackfill`, `CirqaSyncDiagnosticSession`, `CirqaSyncExperimentGate`, `DiagnosticReport`, `FitRecordStructure`, `GarminCompactAck`, `GarminDeviceInformationReference`, `GarminHistoryFrameStream` | Kein Nutzerrisiko, weil keine Laufzeit. Sie dokumentieren geprüfte Protokollhypothesen und sind getestet. **Behalten.** Bei der Modularisierung (#496, geparkt) in ein Forschungsmodul verschieben. |
| Fachregeln, nur in eigenen Tests benutzt | `GroundRules` (BMI/WHO), `NutritionRules` (DGE), `ExportDateRange` | Doppelte Logik: Die App rechnet BMI in `ProfileMath`. **Behalten bis zur Konsolidierung** (Folgepunkt: auf eine Implementierung reduzieren). |
| Toter Code ohne Tests | `DashboardInfoIconView` | **Entfernt** (dieser PR). |
| Fehlalarm der Analyse | `BleFormatting` (`toHex()` wird von Decoder und Backup benutzt) | keine Maßnahme |

## Konsequenz für #661

- Das Risiko liegt in den 279 erreichbaren Dateien. Für die Protokoll-, BLE-, Decoder- und Importdateien darunter gilt:
  - Der FIT-Decoder ist durch den Garmin-SDK-Vergleich auf 37 echten Dateien abgesichert (#707, 96,3 % vor und 100 % nach dem Fix in #717).
  - Download und Abbruch sind durch die gezielten Tests aus #599 und #637 abgedeckt.
  - Backup und Audit sind durch #601 belegt.
- Die kombinierte Coverage (JVM + Instrumentation) erzeugt die CI bereits als Artefakt `pulselibre-combined-jacoco` in jedem FULL-Lauf.
