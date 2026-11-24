# Testinventur Produktionscode (Issue #6, Teil 1)

Stand 2026-11-18, `main`. Methode: statische Zuordnung – eine Produktionsdatei gilt als „referenziert“, wenn mindestens eine ihrer Top-Level-Klassen/-Objekte/-Funktionen in einer Testdatei (`src/test`, `src/androidTest`) namentlich vorkommt. Das belegt **keine Abdeckungsqualität** und keine Zeilenabdeckung (kein JaCoCo-Lauf); es zeigt nur Dateien ganz ohne Testbezug.

- Produktionsdateien (`app/src/main/java/de/pulselibre`): 169
- Unit-Testdateien: 132 · Instrumentationstestdateien: 17
- Dateien ohne jeden Testbezug: 11

## Dateien ohne Testbezug

| Datei | Zeilen | Bereich | Einschätzung / Folge |
|---|---|---|---|
| CirqaMlHandshakeProbe.kt | 1995 | BLE/OpenCode | Größte Datei (≈2000 Zeilen), nur per Gerätetest und Phasen-Gates abgesichert; Aufteilung/Tests → OpenCode. |
| TrendCardView.kt | 221 | GUI/Analyse | Rechenlogik getrennt getestet (`TrendAnalysis`, `FitnessOverview`, `PairedCorrelation`); Darstellung ohne UI-Test. |
| CirqaContinuousRecordingService.kt | 184 | Recorder | Foreground-Service mit Reconnect; Statusmodell `CirqaLinkStatus` getestet, Service selbst nur per Gerät (offen, #63). |
| CirqaGattProbe.kt | 105 | BLE/OpenCode | Aktive GATT-Diagnose; Gerätetest. |
| MainNavigationView.kt | 92 | GUI | Seit 2026-11-18 per `MainNavigationViewTest` (Emulator) abgedeckt. |
| CirqaLocalLog.kt | 54 | Recorder | Lokales Ereignislog; Test fehlt (Größenlimit, Fehlerpfade). |
| GarminServiceDiscovery.kt | 25 | BLE/OpenCode | Kleine Hilfsdatei. |
| CirqaHistoricalSyncV1MultiPhaseAttemptGate.kt | 19 | BLE/OpenCode | Kleine Gate-/Hilfsklasse; Testbezug prüfen. |
| CirqaDirectoryAfterChainGate.kt | 17 | BLE/OpenCode | Kleine Gate-/Hilfsklasse; Testbezug prüfen. |
| CirqaDirectoryReadV2AttemptGate.kt | 17 | BLE/OpenCode | Kleine Gate-/Hilfsklasse; Testbezug prüfen. |
| CirqaLegacy5030To5007AttemptGate.kt | 17 | BLE/OpenCode | Kleine Gate-/Hilfsklasse; Testbezug prüfen. |

## Offene Punkte für die Gesamtinventur (#6)
- Zeilenabdeckung messen (JaCoCo) und CI-Bericht einbinden; heute nur Namensabgleich.
- Ressourcen, Manifest, Gradle, Workflows, Dokumentation und OWASP-MASVS-Abgleich sind **nicht** Teil dieses Teils.
- BLE-/Protokolldateien liegen bei OpenCode; hier nur gelistet.
- #6 bleibt offen.
