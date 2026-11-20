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
| A6-5 | `CirqaLocalLog`-Inhalt und Diagnosepaket auf Geräte-/Gesundheitsdaten geprüft | **erledigt (2026-11-20)** | Belegt: (1) Das Diagnosepaket besteht aus einer festen Feldliste (`DiagnosticsSnapshot`: Zähler, Versionen, Flags; `FitOfflineInventory.redactedText` nur Zähler); `DiagnosticReportPrivacyTest` prüft Abwesenheit von MAC/Name. (2) `CirqaLocalLog` speichert Sitzungs-/Versuchsereignisse mit Statustext (`result.take(600)`: Phase, GATT-Status, Zähler, Antwort*formen* = Länge/Typ/Service-ID, keine Nutzdaten, keine MAC, kein Gerätename) sowie Messwerte (`reading`: bpm, Schritte usw.). Diese Messwerte sind Gesundheitsdaten und bleiben lokal; `CirqaLocalLog.copyTo` (SAF-Export der Ereigniszeilen) hat im Produktionscode keinen Aufrufer (toter Code, kein Abfluss; Entfernung oder Anbindung ist Sache von OpenCode/Datenmodell). (3) Kein `Log.*`/`println` im Produktionscode; neu abgesichert durch `NoPlatformLoggingTest`. Falls `copyTo` je angebunden wird: Datei ist **kein** redigiertes Paket und muss so gekennzeichnet werden. |
| A6-6 | UI-Dateien ohne JVM-Test: Logik, die in Views steckt, ist nur am Emulator abgedeckt | gering | neue UI-Logik weiterhin in reine Modelle auslagern (wie `DayTimeline`, `DashboardLayout`) |

## 6. Nächste Prüfschritte für #6
1. ~~A6-5~~ erledigt (siehe Tabelle); nächste Stufe: A6-6 und dateiweise Tiefenprüfung.
2. Persistenz: Migrationen in `CirqaDatabase` und `DatabaseBackup` (Export/Import, Rollback) dateiweise mit Fehlerpfaden.
3. Ressourcen: Hardcoded-Strings und Beschreibungen **erledigt (Abschnitt 10)**; `UnusedResources` erledigt (Abschnitt 11).
4. Coverage: JVM-Wert erhoben (Abschnitt 12, 30,9 %); kombinierter Bericht JVM + Emulator offen.
5. MASVS-Zuordnung **erstellt (Abschnitt 13)**; offen: S25-Evidenz nur lesend, keine Instrumentationstests auf dem Handy.

## 7. Persistenz: `DatabaseBackup` (dateiweise Prüfung, 2026-11-20)
Gelesen: `extractArchive`, `prepareRestore`, Versions-/Eintragsprüfung, Grenzen.
| Prüfpunkt | Ergebnis | Beleg |
| --- | --- | --- |
| Zip-Slip (Pfade im Archiv) | nicht möglich (zusätzlich lehnt Androids `ZipInputStream` `..`/absolute Namen selbst mit `ZipException` ab; im Emulator API 35 belegt) | Nur feste Eintragsnamen aus `BACKUP_V6_ENTRIES + manifest.json` werden akzeptiert (`entry.name in allowed`); Verzeichnisse und Duplikate werden abgelehnt, Zieldatei = `File(directory, <fester Name>)` |
| Größenbegrenzung / Zip-Bomb | begrenzt | 128 MB gesamt, 120 MB je Eintrag, 2 MB je Zeile, 1 Mio. Datensätze, 64 kB Manifest, während des Entpackens geprüft |
| Versionierung | V1–V6, Eintragsliste muss exakt zur Version passen | `prepareRestore`; Emulator-Tests für V1–V4-Wiederherstellung nach Schema-Upgrades |
| Validierung vor Überschreiben | vollständig, in privatem Cache-Verzeichnis, vor Bestätigungsdialog | `PreparedDatabaseRestore` (`close()` löscht Staging) |
| Prüfsummen | SHA-256 je Datei im Manifest, beim Einlesen geprüft (`DatabaseBackup.kt:152`) | `DigestInputStream` |
| Verwaiste Provenance/Metadaten | werden abgelehnt | Tests `orphanedFitProvenanceIsRejected…`, `orphanedDeviceMetadataIsRejected…` |
| Verschlüsselung | **keine** (Manifest `encryption: none`, im Klassenkommentar benannt) | Befund B7-1 |
Befunde:
- **B7-1 (gering–mittel, Datenschutz):** Das Backup enthält Gesundheitsdaten unverschlüsselt im vom Nutzer gewählten SAF-Ziel (z. B. Cloud-Ordner). Das Verhalten ist dokumentiert; die Nutzerdokumentation und der Exportdialog sollten es deutlich nennen. Verschlüsselung wäre ein eigenes Ticket (Schlüsselverwaltung, Wiederherstellbarkeit).
- **B7-2 (gering, Testlücke):** Es gibt keinen gezielten Test für ein Archiv mit unbekanntem Eintragsnamen, `../`-Namen, absolutem Pfad und fehlendem Manifest (jetzt `DatabaseBackupArchiveSafetyTest`, Emulator); Duplikat-Eintrag und Größenüberschreitung bleiben ungetestet (ZipOutputStream erzeugt keine Duplikate) (die Logik ist vorhanden; `invalidArchiveIsRejectedBeforeAnyStoredDataChanges` deckt nur den allgemeinen Fall). Ein Emulator-/JVM-Test mit manipuliertem ZIP wäre billig.
Nicht geprüft: Migrationen in `CirqaDatabase` (nächster Schritt), echte Wiederherstellung am S25.
## 8. `CirqaContinuousRecordingService`: Lebenszyklus gelesen (A6-2, Beobachtungen für #3/#235)
- Zustandsmodell bereits getrennt und getestet: `RecorderAttemptGate` (Generation + Owner, genau ein terminaler Callback je Versuch, Stop invalidiert alles; `RecorderAttemptGateTest`), `RecorderStatus` (Herzschlag/Flag). Der Service selbst ist ein dünner Android-Rahmen (Main-Looper-`Handler`, alle Callbacks per `handler.post`, `removeCallbacksAndMessages(null)` bei Stop).
- Neu aus dem Service herausgezogen: `RecorderBackoff.delayMs` (2/4/8/16/32/60 s), unverändertes Verhalten, `RecorderBackoffTest`.
- **Beobachtung O-1 (für #235, nicht geändert):** Der Backoff zählt **alle Versuche seit Dienststart** (`attempts`), nicht aufeinanderfolgende Fehlschläge, und wird nach erfolgreichem Empfang nicht zurückgesetzt. Nach sechs Versuchen beträgt die Wartezeit dauerhaft 60 s, auch nach stundenlanger stabiler Verbindung (Datenlücke bis 60 s je Abbruch). Umgekehrt gibt es keine Verlängerung über 60 s bei dauerhaft abwesendem Band (Verbindungsversuch alle 60 s). Eine Änderung (Reset nach erfolgreichem Empfang, längere Obergrenze ohne Band) ist eine Verhaltensänderung und gehört **nach** der Akku-Baseline (#235) entschieden.
- **Beobachtung O-2:** Ein Instrumentationstest des Services selbst (Start/Stop/Bluetooth aus) fehlt; er würde einen Foreground-Service auf dem Emulator starten. Nicht angelegt, weil er die BLE-Pfade berührt (OpenCode/#3-Owner-Abnahme am S25).

## 9. Persistenz: `CirqaDatabase`-Migrationen (Schema v1 → v6, gelesen 2026-11-20)
- `onUpgrade` ist ein linearer Pfad mit `require(oldVersion in 1..5 && newVersion == 6)`; `SQLiteOpenHelper` führt ihn in **einer Transaktion** aus (Fehler → Rollback, Datenbank bleibt auf der alten Version). Jede Stufe nutzt additive Änderungen (`ALTER TABLE … ADD COLUMN`, neue Tabellen); keine Löschung/Umbenennung bestehender Daten. Fremdschlüssel sind aktiv (`onConfigure`), Provenance hängt per `ON DELETE RESTRICT/CASCADE` an Quelle und Beobachtung.
- Pfadabdeckung: v1 → v6 (`CirqaHistoryReaderTest` legt eine v1-Datenbank an und prüft das Backfill von `dashboard_time`), v2 → v6 (`canonicalSchemaV2UpgradePreservesEventsAndAddsNormalizedTables`), v3 → v6, v4 → v6 (`CirqaStorageMigrationTest`), v5 → v6 (`DatabaseBackupTest.schemaFiveUpgradeAddsInventory…`). Alle fünf Ausgangsversionen sind abgedeckt, kein Befund.
- **Downgrade:** Kein `onDowngrade`; `SQLiteOpenHelper` wirft dann eine Ausnahme (Installation eines älteren Builds über ein neueres Schema startet nicht, Daten bleiben unangetastet). Für ein Datenschutz-/Datenintegritätsziel akzeptabel, aber für Nutzer nicht erklärt (Befund **B9-1**, gering: in der Fehlerbehebung vermerken, dass Downgrades nicht unterstützt werden).
- Schemaänderungen an `CirqaDatabase` gehören laut AGENTS.md zu den koordinierungspflichtigen Dateien; hier wurde nichts geändert.

## 10. Ressourcen: Texte und Beschriftungen (gelesen 2026-11-20)
| Prüfpunkt | Ergebnis | Beleg |
| --- | --- | --- |
| String-Ressourcen | **Keine `strings.xml`, keine `R.string`-Verweise.** Alle sichtbaren Texte und `contentDescription`-Werte stehen als deutsche Literale im Kotlin-Code (Suche `setText("`/`text = "`: 120 Treffer in 13 Dateien; zusätzlich Beschriftungen in `MainActivity` u. a.). Manifest-Labels (`PulsLibre`, `CIRQA · Kontinuierliche Aufzeichnung`) ebenfalls als Literale. | Suche in `app/src/main/java`, `AndroidManifest.xml` |
| Folgen | (1) Keine Übersetzung möglich, ohne den Code anzufassen (die App ist bewusst nur Deutsch, `values/` enthält nur Farben/Themes). (2) Lint-Regel `HardcodedText` greift nur bei XML-Layouts und meldet hier nichts, weil die Oberfläche programmatisch gebaut wird. (3) Gleiche Begriffe können an mehreren Stellen leicht abweichen. | Ressourcenverzeichnis `app/src/main/res/values*` |
| Barrierefreiheit | Die TalkBack-Beschreibungen sind vorhanden, aber ebenfalls Literale; `HistoryDensityChartView` war bis PR #306 die einzige eigene View ohne Beschreibung. | siehe Worklog 2026-11-20 |

**Befund A10-1 (gering, Wartbarkeit):** Texte nicht ausgelagert. Empfehlung: nicht als Sammelumbau, sondern bei jeder ohnehin angefassten Ansicht neue Texte als `strings.xml`-Einträge anlegen; ein Wächtertest (wie `NoPlatformLoggingTest`) kann die Zahl der Literale festschreiben, damit sie nicht wächst. Ob eine Mehrsprachigkeit gewünscht ist, ist eine Produktentscheidung (offen, Nutzer).
Nicht geprüft: ungenutzte Ressourcen (Lint-Lauf `UnusedResources` steht aus), Textlängen bei Schrift 1,3+ und 320 dp (siehe #276).

## 11. Lint-Auswertung `lintDebug` (lokal, `main` nach PR #312, 2026-11-20)
Lauf: `gradle --offline :app:lintDebug`, Bericht `app/build/reports/lint-results-debug.*` (nicht eingecheckt). Ergebnis: **0 Fehler**; Warnungen nach Regel: `UseKtx` 94, `ViewConstructor` 16, `DrawAllocation` 11, `ClickableViewAccessibility` 3, `GradleDependency` 3, `ObsoleteSdkInt` 2, `ApplySharedPref` 1, `NotShrinkingResources` 1, `OldTargetApi` 1, `UnusedAttribute` 1.
| Prüfpunkt | Ergebnis |
| --- | --- |
| Ungenutzte Ressourcen | **Keine Meldung `UnusedResources`.** Gegenprobe ohne Lint: alle 59 Drawables werden im Code oder Manifest referenziert (`R.drawable.*`/`@drawable/*`). Damit ist der in Abschnitt 10 offene Punkt erledigt. |
| `ClickableViewAccessibility` (MainActivity:825, :1084 ×2) | **Bewertet, unkritisch, kein Code geändert.** :825 ist der versteckte 3-s-Long-Press (`attachHiddenLongPress`) an der nicht klickbaren Überschrift „Gerät“; er gibt `true` zurück, verbraucht also nur Touches einer View ohne eigenen Klick, und die Aktion steht zusätzlich als TalkBack-Aktion „Testlabor öffnen“ bereit. :1084 ist der Gewichts-Stepper (+/−): der Touch-Listener gibt `false` zurück und lässt den Klick durch; Klick und Wiederholung laufen über `OnClickListener`/`OnLongClickListener` (TalkBack-tauglich). Die Meldung ist hier ein Fehlalarm. |
| `ApplySharedPref` (`CirqaTestLaboratoryActivity:534`) | `commit()` statt `apply()` entfernt den Zusammenfassungs-Schlüssel des Einmal-Gates synchron vor dem Dialog. Vermutlich Absicht (Gate soll vor dem aktiven Schritt sicher persistiert sein); gehört zu BLE/Gates (OpenCode), nicht angefasst. |
| `OldTargetApi` | `targetSdk 35` bei `compileSdk 37` (AGENTS.md). Kein Play-Store-Vertrieb, Anhebung ist ein eigener Migrationsschritt mit Verhaltensänderungen (Edge-to-Edge, Foreground-Service-Typen) und gehört in ein eigenes Ticket, nicht in #6. |
| `UseKtx`, `ViewConstructor`, `DrawAllocation` | Stil-/Performance-Hinweise ohne Sicherheitsbezug. `DrawAllocation` (11) betrifft Allokationen in `onDraw` der selbst gezeichneten Views; relevant für Ruckeln und Akku (#235), aber ohne Messung nicht priorisiert. |
**Offen für #6:** Coverage-Bericht (JaCoCo), MASVS-Zuordnung, S25-Evidenz, dateiweise Tiefenprüfung der Dateien ohne Testreferenz.

## 12. Coverage (nur JVM-Unit-Tests, lokal, 2026-11-20)
Erzeugt mit `gradle --offline :app:createDebugUnitTestCoverageReport` (JaCoCo ist in `app/build.gradle.kts` aktiviert; die CI lädt `pulselibre-jvm-jacoco` hoch). **Nur JVM-Tests:** 5.080 von 16.457 Zeilen = **30,9 %**.
**Wichtige Einschränkung:** Die Emulator-Instrumentationstests (`androidTest`) sind darin **nicht** enthalten. Deshalb stehen Dateien mit 0 %, die dort getestet werden (`CirqaDatabase`, `DatabaseBackup`, `FitObservationCommitter`, Teile der Views), obwohl sie Tests haben. Der kombinierte Wert fehlt noch; die CI erzeugt ihn bisher nicht. Ein kombinierter Bericht braucht einen Emulatorlauf mit `enableAndroidTestCoverage` (ist gesetzt) und das Einsammeln der `.ec`-Dateien; das ist ein eigener CI-Schritt und wurde hier bewusst nicht lokal gestartet (am selben adb hängt das S25; Instrumentationstests laufen nie auf dem Handy).
Größte JVM-unabgedeckte Dateien (Zeilen ohne Abdeckung): `CirqaMlHandshakeProbe` 2.123, `CirqaTestLaboratoryActivity` 1.287, `MainActivity` 970, `DatabaseBackup` 794, `CirqaDatabase` 685, `DashboardOverviewView` 641, `ExportActivity` 424, `HistoryOverviewView` 368, `TrendCardView` 345, `PersonalSettings` 301. Ausgenommen die zwei Hardware-Probe-Dateien sind das überwiegend Android-UI- und Persistenzcode, dessen Logik in reinen Modellen liegt (siehe A6-6) bzw. am Emulator geprüft wird.
**Vorschlag (eigener Schritt):** kombinierter JaCoCo-Bericht in der Full-CI (Emulator-Job lädt `.ec` hoch, ein Folgeschritt führt JVM + Emulator zusammen) und danach komponentenweise Zielwerte statt einer Gesamtzahl. Bis dahin keine Aussage über „Gesamtabdeckung“.

## 13. Zuordnung zu OWASP MASVS v2 (Stand 2026-11-20)
**Keine MASVS-Abnahme und kein Zertifikat.** Die Tabelle ordnet die bisher belegten Prüfungen den MASVS-Kategorien zu und zeigt, was noch nicht geprüft ist. „belegt“ heißt: in diesem Audit mit Beleg (Abschnitt in Klammern) gesehen, nicht, dass die Anforderung vollständig erfüllt ist.
| MASVS-Kategorie | Bisher belegt | Befunde | Noch nicht geprüft |
| --- | --- | --- | --- |
| **STORAGE** (lokale Daten) | `allowBackup=false`, Cloud-/Gerätewechsel-Backup schließt alles aus; kein externer Speicher, Export nur per SAF (4); kein `Log.*` im Produktionscode, abgesichert durch `NoPlatformLoggingTest` (A6-5); Diagnosepaket = feste Feldliste (A6-5) | **B7-1** manuelles Backup-ZIP unverschlüsselt (im Handbuch erklärt); `CirqaLocalLog.copyTo` toter Code, falls je angebunden: kein redigiertes Paket (A6-5) | Dateiberechtigungen der App-Verzeichnisse am Gerät; Inhalt von Screenshots/Recents (`FLAG_SECURE` nicht gesetzt, Produktentscheidung); Löschung bei Deinstallation am Gerät |
| **CRYPTO** | keine eigene Kryptografie im Anwendungscode gefunden (Stichprobe); SHA-256 für Backup-Integrität (7) | – | Vollständige Suche nach Krypto-APIs; BLE-Pairing/Bonding-Verhalten gehört zu BLE (OpenCode) |
| **AUTH** | nicht anwendbar (kein Konto, kein Server) | – | – |
| **NETWORK** | keine INTERNET-Berechtigung, keine HTTP-/Socket-Klassen (4); damit kein TLS-Thema | – | Bluetooth-Transport (BLE/RFCOMM) ist Gegenstand der Protokollrecherche, nicht dieses Audits |
| **PLATFORM** (IPC, Berechtigungen, UI) | nur `MainActivity` exportiert, Service/übrige `exported=false`; Stop-`PendingIntent` `FLAG_IMMUTABLE`; kein WebView; Berechtigungen minimal (4) | **A6-1** behoben (`RECEIVER_NOT_EXPORTED` im RFCOMM-Runner); `ClickableViewAccessibility`-Funde bewertet (11) | Intent-Filter/Deep Links (keine gefunden, Stichprobe); Verhalten unter Android 14+ Foreground-Service-Regeln am Gerät; Overlay-/Tapjacking-Schutz nicht geprüft |
| **CODE** (Qualität, Abhängigkeiten) | Abhängigkeiten per Dependabot + SCA-Prüfung (#155: 8 → 0 Alerts); Lint 0 Fehler (11); R8-Release in der CI | **A6-2** Dauerdienst ohne Lebenszyklus-Test; **A6-3** nur indirekt getestet; **A6-4** `CirqaMlHandshakeProbe` 2.842 Zeilen ohne Test; JVM-Coverage 30,9 % (12); `targetSdk 35` (11) | Kombinierte Coverage; Abhängigkeitslizenzen laufend (siehe `LICENSE_AGPL_AUDIT`); Fehlerpfade von `FitObservationCommitter` einzeln |
| **RESILIENCE** (Manipulationsschutz) | Release ist R8-verkleinert/obfuskiert, signiert; Schema-Migrationen linear und transaktional (9); Backup-Import validiert vor Überschreiben (7) | **B9-1** kein Downgrade (Hinweis im Handbuch); **B7-2** Testlücke (inzwischen `DatabaseBackupArchiveSafetyTest`) | Root-/Debugger-Erkennung ist für diese App **nicht** Ziel (lokale Gesundheitsdaten, kein Schutzbedarf gegen den Gerätebesitzer); Entscheidung bewusst offen für den Nutzer |
| **PRIVACY** | keine Telemetrie, keine Werbe-IDs, keine Netzwerkberechtigung; Messwerte bleiben lokal; Diagnosepaket mit Vorschau und ausdrücklicher Nutzeraktion (4, A6-5) | – | Datenschutzerklärung/Einwilligungstexte im Handbuch gegen die tatsächliche Datenhaltung abgleichen (#177) |

**Reihenfolge der nächsten Schritte (nach Risiko):** (1) A6-2 Lebenszyklus-/Timeout-Test des Dauerdienstes, zusammen mit dem Bearbeiter von #3/#235; (2) kombinierter Coverage-Bericht in der Full-CI; (3) A6-4 Zerlegung (OpenCode); (4) S25-Evidenz nur lesend nach der FIT-Prüfung, wenn das Handy wieder frei ist. Eine Mehrsprachigkeit (A10-1) ist eine Produktentscheidung und kein Sicherheitsthema.

## 14. S25-Evidenz, nur lesend (Build 2.0.0_build1502, 2026-11-20)
Quelle: `adb shell dumpsys package de.pulselibre` auf dem S25 (nur Lesen; keine Instrumentationstests, kein BLE-Vorgang). Roh-Ausgabe nicht eingecheckt.
| Prüfpunkt | Gerätebefund | Passt zu Manifest/Audit |
| --- | --- | --- |
| Angeforderte Berechtigungen | `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` und die app-eigene `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (von AndroidX für nicht exportierte Receiver) | ja; **keine** `INTERNET`, keine Standort-, Kontakt- oder Speicherberechtigung |
| Zur Laufzeit gewährt | Benachrichtigungen, Bluetooth-Verbindung, Bluetooth-Suche | ja (Nutzer hat gewährt, kein Standort nötig) |
| App-Flags | `HAS_CODE ALLOW_CLEAR_USER_DATA`; **kein** `DEBUGGABLE`, **kein** `ALLOW_BACKUP` | bestätigt `allowBackup=false`; Release-Build nicht debuggbar |
| Signatur/Installation | eine Signatur, `installerPackageName=null` (Sideload per adb/APK) | erwartet; Update-Kontinuität zu früheren Builds durch `adb install -r` ohne Deinstallation belegt (Daten blieben erhalten) |
| Zielversion | `targetSdk=35`, `minSdk=26` | wie `OldTargetApi` in Abschnitt 11 |
**Noch offen (Handy):** Standort-/Speicher-Berechtigungsliste nach Update von sehr alten Builds, echtes TalkBack, Verhalten der App unter Foreground-Service-Regeln bei längerem Lauf (gehört zu #235).

## 15. Kombinierter Coverage-Bericht: Versuch und Befund (2026-11-20)
Versuch: JVM-`.exec` (Artefakt `pulselibre-jvm-jacoco`) und Emulator-`.ec` (Artefakt `pulselibre-instrumentation-coverage`) desselben `main`-Laufs (Commit `3681e22`) lokal gegen die frisch gebauten Debug-Klassen desselben Commits mit JaCoCo (`JacocoReport`, Init-Skript außerhalb des Repos) zusammengeführt.
**Ergebnis: 0 % für alles, auch für die JVM-Daten, die allein 30,9 % ergaben. Das ist ein Messartefakt, kein Befund über die Tests.** JaCoCo ordnet Messdaten nur Klassen mit identischer Prüfsumme zu; lokal übersetzte Klassen sind nicht byteidentisch mit den in der CI übersetzten. Auch **zwischen den beiden CI-Jobs** (Build-Job und Emulator-Job übersetzen getrennt) ist ein Zusammenführen nicht verlässlich möglich.
**Folgerung:** Ein kombinierter Wert entsteht nur **innerhalb eines Jobs** mit denselben Klassen. Vorschlag an den CI-Verantwortlichen (Änderung an `tools/ci/run-ci.sh` und `.github/workflows/android.yml`, nicht von mir umgesetzt): im Emulator-Job zusätzlich `:app:testDebugUnitTest` ausführen (vermutlich +1–2 Minuten), dann `JacocoReport` über `testDebugUnitTest.exec` + `connected …/coverage.ec` mit den Klassen dieses Jobs erzeugen und den XML-/HTML-Bericht als Artefakt hochladen. Bis dahin gilt: **JVM-Wert 30,9 % (Abschnitt 12) ist der einzige belastbare Zahlenwert; eine Gesamtabdeckung ist nicht bekannt.**
