# Codeaudit #6: Komponenteninventur und Sicherheits-Stichprobe (2026-11-20)

**Teilprüfung, Ticket #6 bleibt offen.** Diese Ausgabe ersetzt weder die dateiweise Tiefenprüfung noch eine OWASP-MASVS-Abnahme noch eine S25-Hardware-Evidenz. Sie liefert (1) eine reproduzierbare Inventur mit Testreferenzen je Komponente, (2) eine belegte Stichprobe der sicherheitsrelevanten Konfiguration und (3) die daraus folgende Reihenfolge für die weitere Prüfung. Grundlage: `main` nach PR #286 (233 Produktionsdateien, 24.997 Zeilen).

## 1. Methode und Grenzen
- Inventur per Skript über `app/src/main/java/**/*.kt`. Komponente = Zuordnung nach Dateinamen (BLE/Protokoll, Persistenz/Import, FIT/Inventur, UI, Auswertung/Dashboard-Logik, Sonstige).
- **Testreferenz** = der Name einer Klasse, eines Objekts oder einer Top-Level-Funktion der Datei kommt in `app/src/test` (JVM) oder `app/src/androidTest` (Emulator) vor. Das ist **keine Coverage-Messung** und sagt nichts über Testtiefe; es zeigt nur, wo **nichts** referenziert wird (mögliche Lücken, auch Fehlalarme bei indirekt getesteten Dateien).
- Kein S25-Test, kein Fuzzing, keine statische Analyse über Lint hinaus.

## 2. Matrix
| Komponente | Dateien | Zeilen | JVM-Testreferenz | Emulator-Testreferenz | ohne Testreferenz |
| --- | ---: | ---: | ---: | ---: | ---: |
| Auswertung/Dashboard-Logik | 20 | 1361 | 20 | 7 | 0 |
| BLE/Protokoll | 125 | 11357 | 110 | 3 | 14 |
| FIT/Inventur | 8 | 1248 | 6 | 5 | 1 |
| Persistenz/Import | 11 | 2970 | 6 | 8 | 0 |
| Sonstige | 33 | 1564 | 28 | 7 | 3 |
| UI | 36 | 6497 | 5 | 21 | 14 |
| **Summe** | **233** | **24997** | **175** | **51** | **32** |

Auffällig: Die **UI**-Komponente ist fast nur über Emulator-Tests referenziert (5 von 36 Dateien mit JVM-Test, 21 mit Emulator-Test, 14 ohne beides); bei BLE/Protokoll sind 110 von 125 Dateien JVM-referenziert, aber nur 3 mit Emulator-Bezug (erwartbar: Hardware).

## 3. Dateien ohne jede Testreferenz (32)
| Datei | Komponente | Zeilen |
| --- | --- | ---: |
| `CirqaMlHandshakeProbe.kt` | BLE/Protokoll | 2842 |
| `FitObservationCommitter.kt` | FIT/Inventur | 289 |
| `CirqaContinuousRecordingService.kt` | BLE/Protokoll | 210 |
| `HeaderBackdropDrawable.kt` | UI | 156 |
| `CirqaRfcommRunner.kt` | BLE/Protokoll | 149 |
| `DayTimelineDialog.kt` | UI | 107 |
| `CirqaGattProbe.kt` | BLE/Protokoll | 104 |
| `PlIcon.kt` | UI | 85 |
| `AnalysenView.kt` | UI | 84 |
| `DiagnosticsCollector.kt` | Sonstige | 81 |
| `RestingPulseChartView.kt` | UI | 78 |
| `CirqaRfcommTransport.kt` | BLE/Protokoll | 77 |
| `DailyBarsView.kt` | UI | 75 |
| `DashboardCustomizeDialog.kt` | UI | 60 |
| `SparkView.kt` | UI | 55 |
| `CirqaLocalLog.kt` | Sonstige | 53 |
| `StepBarsView.kt` | UI | 53 |
| `CirqaRfcommStreamFramer.kt` | BLE/Protokoll | 50 |
| `CirqaRfcommSpec.kt` | BLE/Protokoll | 47 |
| `DashboardInfoIconView.kt` | UI | 31 |
| `DashboardMetricIconView.kt` | UI | 31 |
| `PulseLogoView.kt` | UI | 31 |
| `CirqaItemListRepeatabilityAttemptGate.kt` | BLE/Protokoll | 24 |
| `DesignTokens.kt` | Sonstige | 24 |
| `GarminServiceDiscovery.kt` | BLE/Protokoll | 24 |
| `HeaderMountainData.kt` | UI | 24 |
| `MetricInfoIconMapping.kt` | UI | 20 |
| `CirqaFileAccessAttemptGate.kt` | BLE/Protokoll | 19 |
| `CirqaHistoricalSyncV1MultiPhaseAttemptGate.kt` | BLE/Protokoll | 18 |
| `CirqaDirectoryAfterChainGate.kt` | BLE/Protokoll | 16 |
| `CirqaDirectoryReadV2AttemptGate.kt` | BLE/Protokoll | 16 |
| `CirqaLegacy5030To5007AttemptGate.kt` | BLE/Protokoll | 16 |

Einordnung (Stichprobe, nicht abschließend):
- **Hardware-/Diagnosepfad, bewusst nur am Gerät prüfbar:** `CirqaMlHandshakeProbe` (2.842 Zeilen, größte Datei ohne Referenz), `CirqaGattProbe`, `CirqaRfcommRunner`/`-Transport`/`-StreamFramer`/`-Spec`, `GarminServiceDiscovery`. Risiko mittel: Größe und Komplexität, aber hinter den dokumentierten Sicherheits-Gates; Zerlegung und Unit-Tests der reinen Parser-/Framer-Teile wären sinnvoll (Zuständigkeit OpenCode).
- **Dauerdienst:** `CirqaContinuousRecordingService` (Foreground-Service, #3/#25/#171) ohne direkte Testreferenz; Lebenszyklus und Timeout-Zustände sind Kern von #3. Risiko **mittel bis hoch**, weil hier Datenverlust und Akkuverbrauch (#235) entstehen können.
- **Schreibpfad:** `FitObservationCommitter` (289 Zeilen) ohne direkte Referenz; der Weg wird indirekt über `FitHistoricalObservations`-/Store-Tests berührt, ein direkter Test der Commit-/Idempotenz-Logik fehlt. Risiko mittel (Datenintegrität).
- **Reine Darstellung/Zeichnen** (`HeaderBackdropDrawable`, `PlIcon`, `SparkView`, `StepBarsView`, `DailyBarsView`, kleine Icon-Views, `DesignTokens`): geringes Risiko; visuelle Abnahme am Emulator/S25. `DayTimelineDialog`, `DashboardCustomizeDialog` und `AnalysenView` sind Dialog-/Container-Code, ihre Logik liegt in getesteten Modellen (`DayTimeline`, `DashboardLayout`, `AnalysisCatalog`).

## 4. Sicherheits-Stichprobe mit Belegen
| Prüfpunkt | Befund | Beleg |
| --- | --- | --- |
| Netzwerk | Keine INTERNET-Berechtigung; kein `HttpURLConnection`, `java.net.URL`, OkHttp oder Sockets im Code | `AndroidManifest.xml`, Suche in `app/src/main/java` |
| Backup/Datenabfluss | `allowBackup=false`; Cloud-Backup und Gerätewechsel schließen root, file, database, sharedpref, external aus | `AndroidManifest.xml`, `res/xml/data_extraction_rules.xml`, `res/xml/full_backup_content.xml` |
| Exportierte Komponenten | Nur `MainActivity` (Launcher) ist exportiert; Service und die übrigen Activities `exported=false` | `AndroidManifest.xml` |
| Berechtigungen | BLE (SCAN mit `neverForLocation`, CONNECT), POST_NOTIFICATIONS, Foreground-Service; Standort und klassisches Bluetooth nur bis API 30 | `AndroidManifest.xml` |
| PendingIntents | Stop-Aktion der Benachrichtigung mit `FLAG_IMMUTABLE` | `CirqaContinuousRecordingService.kt:194-196` |
| Logging | Kein `android.util.Log`-Aufruf im Produktionscode; lokales Protokoll über `CirqaLocalLog` (Inhalt noch nicht geprüft) | Suche `Log.` in `app/src/main/java` |
| WebView/Clipboard/externer Speicher | Weder WebView noch Zwischenablage noch `getExternalFilesDir`; Export über Storage Access Framework (`CreateDocument`, vom Nutzer gewählt) | `ExportActivity.kt:45,94` |
| SQL | Abfragen mit Nutzer-/Zeitwerten nutzen Bind-Argumente; die interpolierten SQL-Texte sind konstant (Stichprobe `CirqaHistoryReader`, `DashboardDayReader`, `CirqaDatabase`) | jeweilige `rawQuery(sql, args)`-Aufrufe |
| Teilen | Diagnosepaket wird mit Vorschau über `ACTION_SEND`/Chooser auf ausdrückliche Nutzeraktion geteilt (Inhalte nicht geprüft) | `DiagnosticsSheet.kt:95` |

## 5. Befunde und Empfehlungen
| ID | Befund | Risiko | Empfehlung / Zuständigkeit |
| --- | --- | --- | --- |
| A6-1 | `CirqaRfcommRunner` registrierte den `ACTION_UUID`-Receiver mit `RECEIVER_EXPORTED`. | **behoben (2026-11-20)**: jetzt `RECEIVER_NOT_EXPORTED`. `ACTION_UUID` ist ein geschützter System-Broadcast; Android liefert solche Broadcasts auch an nicht exportierte Receiver, fremde Apps können ihn nicht mehr einspeisen. | Abdeckung: Emulator-Lauf kompiliert die Änderung; echte SDP-Antwort am Band nicht erneut geprüft (kein BLE-/RFCOMM-Lauf ausgeführt). |
| A6-2 | `CirqaContinuousRecordingService` ohne Testreferenz | mittel–hoch | Lebenszyklus-/Timeout-Tests (#3), in Abstimmung mit dem Bearbeiter von #3/#25/#171 |
| A6-3 | `FitObservationCommitter` ohne direkte Testreferenz | **korrigiert (2026-11-20)**: indirekt über `CirqaDatabase.commitFitFile` getestet (`LocalReadingStoreTest`: wiederholbarer Backfill, Ablehnung ungültiger Dateien, Batch idempotent, kein Schreiben/Cursor bei ungültigem Batch, Konfliktfall → `IllegalStateException`; `HistoricalObservationRepositoryTest`). Nur ein direkter Test der einzelnen Konfliktprüfungen (Metadaten-/Provenance-Abweichung) fehlt. | optional: gezielte Konflikttests je Prüfung |
| A6-4 | `CirqaMlHandshakeProbe` mit 2.842 Zeilen ohne Referenz | mittel (Wartbarkeit) | Zerlegung und Tests der reinen Teile; OpenCode |
| A6-5 | `CirqaLocalLog`-Inhalt und Diagnosepaket auf Geräte-/Gesundheitsdaten prüfen | offen | Prüfung steht aus |
| A6-6 | UI-Dateien ohne JVM-Test: Logik, die in Views steckt, ist nur am Emulator abgedeckt | gering | neue UI-Logik weiterhin in reine Modelle auslagern (wie `DayTimeline`, `DashboardLayout`) |

## 6. Nächste Prüfschritte für #6
1. A6-5: `CirqaLocalLog` und `DiagnosticsCollector` auf personenbezogene Inhalte lesen (Redaktion belegen).
2. Persistenz: Migrationen in `CirqaDatabase` und `DatabaseBackup` (Export/Import, Rollback) dateiweise mit Fehlerpfaden.
3. Ressourcen: unbenutzte Ressourcen, Hardcoded-Strings, Barrierefreiheit (TalkBack-Beschreibungen).
4. Zusammengeführter Coverage-Bericht (JaCoCo, JVM + Emulator) und komponentenweise Zielwerte.
5. MASVS-Zuordnung (STORAGE, NETWORK, PLATFORM, CODE, RESILIENCE) je Befund; S25-Evidenz nur lesend, keine Instrumentationstests auf dem Handy.
