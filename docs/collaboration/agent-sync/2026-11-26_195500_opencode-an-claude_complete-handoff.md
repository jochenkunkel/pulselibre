# Vollständige Übergabe: OpenCode FIT → Claude Code / GUI-Agent
Von: OpenCode FIT · An: Claude Code (GUI-Agent), Projektleitung · 26.11.2026
Bezug: Nutzerwunsch, alle OpenCode-Aufgaben an Claude Code zu übergeben.

## Kurzstatus

Bitte übernimm die verbleibende Arbeit sequenziell. Keine Geräteaktion ist für die nächsten Schritte erforderlich. Das wichtigste offene Gate ist #599: gezielte JVM-/Instrumentationstests und FULL/API-35 auf dem finalen PR-Head. #601 hat inzwischen grünes FULL/API-35; #707 hat einen synthetischen Garmin-SDK-Vergleich, aber keine Originaldateien. #628 und #637 bleiben wegen offener History-/Fehlerursachenfragen offen.

## Aufgaben und konkrete Übergaben

### #637 – CIRQA-History / 26.11.-Lauf (teilweise, nicht abgeschlossen)
- PR #712 zur Diagnose-/Berichtskürzung ist gemergt als `b515b873`. CI für den PR-Head war QUICK/API-35-Emulator-job skipped; Build/Unit/Lint/APK und Dependency-Gates waren erfolgreich.
- GUI hatte den abgeschnittenen alten Bericht `runId=bc355145cf95` lease-geschützt nur gelesen: 29 Dateien ausgewählt, 28 Streams archiviert und SHA-256-validiert; der 29. Pull-Write wurde angenommen, aber bis zum 120-s-Limit nicht beantwortet. Kein `result=3` im lesbaren Berichtsausschnitt belegt; wegen damaliger 4.000-Zeichen-Kürzung ist der Rest nicht rekonstruierbar.
- #712 ordnet im Bericht künftig `terminal`/Pull-Ergebnisse vor langen Timing-Daten und speichert bis 64 KiB. Das hilft erst ab einem neuen Lauf; keinen alten Bericht als nachträglich vollständig behandeln.
- **Offen:** vollständige Gerätehistorie, letzte Datei/Antwort, frühere `result=3`-Ursache bleiben unbewiesen. Keine neue Hardwareaktion ohne eigenen Auftrag/Lease und Stop-Gates.
- Maßgebliche lokale FIT-Arbeitskopie ist **dirty**: `/tmp/opencode/issue637-p0-history-followthrough-20261125`, Branch `opencode/issue637-p0-history-followthrough-20261125`, HEAD `343a8f8c`. Nicht resetten, nicht auschecken/überschreiben und nicht blind committen. Geänderte Dateien:
  - `docs/collaboration/agent-activity/sessions/2026-11-25_0851_opencode_issue637-p0-history-followthrough.md`
  - `docs/collaboration/agent-sync/INDEX.md`
  - `docs/protocol/device-tests/CIRQA_P0_S10_BOUNDED_FULL_PULL_PLAN_2026-11-24.md`
  - `docs/protocol/research/CIRQA_CONNECT_NORMAL_HISTORY_COMPARISON_2026-11-25.md`
  - zahlreiche untracked `docs/collaboration/agent-chat/messages/2026-11-25_*` und `2026-11-26_*` Einträge plus `docs/protocol/research/ISSUE637_CHATGPT_HANDOFF_2026-11-25.md`.
- **Nächster Schritt für Claude:** zuerst `git status` und `git diff` auf dieser Kopie prüfen; Änderungen und Handoff-Dateien sichern/zuordnen. Nicht den großen Worktree-Inhalt mit dem inzwischen gemergten PR #712 verwechseln.
- Issue: https://github.com/jochenkunkel/pulselibre/issues/637

### #707 – FIT-Standardfelder / unabhängiger SDK-Vergleich (teilweise)
- PR #708 ist gemergt (`b2829b18`): 55/24 `current_activity_type_intensity` als nichtklinischer Review-/Aktivitätscode.
- GUI verglich synthetische CRC-valide Vektoren mit `garmin-fit-sdk==21.222.0`: 32/33 vergleichbare Feldinstanzen matchen; die Abweichung ist die bewusste Ablehnung von HR=0 als Messwert; ein `timestamp_16`-Fall ist nicht direkt vergleichbar, weil das SDK nicht rekonstruiert.
- Originaldateien beim GUI-Agenten `NOT_AVAILABLE`; keine Genauigkeit für reale FITs/keine ≥95-%-Behauptung. Globals 233/397 fehlen im Profil und bleiben roh/unbekannt; SpO₂-Confidence-Semantik weiterhin offen.
- Docs-PR #714 offen, Draft, Head `1102bc9c`; Build/Unit/Lint/APK, Dependency Review und Dependency Graph SUCCESS; API-35-Job in QUICK erwartungsgemäß SKIPPED. Keine Geräteaktion.
- Lokaler Worktree: `/tmp/opencode/issue707-fit-audit-20261126`, Branch `opencode/issue707-fit-audit-20261126`, HEAD `1102bc9c`, clean.
- Bericht: `docs/protocol/research/ISSUE707_GARMIN_SDK_SYNTHETIC_VALIDATION_2026-11-26.md`.
- **Nächster Schritt:** PR #714 selbst prüfen und regulär abschließen; #707 offen lassen, solange echte Originalabdeckung/semantische Fragen nicht gelöst sind.
- Issue: https://github.com/jochenkunkel/pulselibre/issues/707

### #628 – FIT compressed timestamp / historische Importfehler (teilweise, Produktfix integriert)
- PR #630 ist regulär gemergt als `16ebb5d7`. FULL/API-35 run `45467276535` war SUCCESS auf PR head `9c5bece2`; spätere #630 PR-Rollup hatte API-35 SKIPPED, weil QUICK.
- Redigierte S10-Evidenz: 45 Dateien, Decoder v2/v3 identisch, 0 zusätzliche importierbare Werte. Die historischen 20 Importfehler sind nicht pro Datei klassifiziert; fünf `SOURCE_CONFLICT`s aus einem separaten kontrollierten Replay dürfen ihnen nicht zugerechnet werden.
- Historischer Fehler/die 20 Ursachen und vollständige Historienabdeckung bleiben offen; keine Ursache oder zusätzliche Messwerte behaupten.
- Lokaler Doku-Worktree: `/tmp/opencode/issue628-p0-audit-20261126`, Branch `opencode/issue628-p0-audit-20261126`, HEAD `0f64e441`, clean; dokumentiert auch Agenten-START/ENDE.
- Maßgeblicher Forschungsbericht auf `main`: `docs/protocol/research/FIT_COMPRESSED_TIMESTAMP_REPLAY_2026-11-24.md`.
- **Nächster Schritt:** keine redundante Parseränderung; Issue nach fachlicher Beurteilung offen lassen oder gezielt nur anhand neuer per-file Evidenz fortsetzen.
- Issue: https://github.com/jochenkunkel/pulselibre/issues/628

### #601 – Import-Audit/Backup (Implementierung + Testabnahme belegt; Issue noch offen)
- PRs #607 (`a0d15e33`), #613 (`0f7259b5`) und #616 (`e6462373`) gemergt. Aktueller Backup-Stand ist v12, DB-Schema 11.
- GUI-Abnahme auf aktuellem `main=b515b873`: FULL/API-35 run `45877259122` SUCCESS, beide Jobs grün. `ImportAuditRecorderTest` 2/2, `ImportAuditForeignKeyTest` 1/1, `DatabaseBackupTest` 16/16 inkl. Audit-Roundtrip und Legacy v1–v7 Restore, `CirqaDataModelV9InstrumentedTest` 3/3. Keine roten/ausstehenden Gates laut GUI.
- Aktueller #601-Nachtrag zur Abnahme steht unter Issue-Kommentar `7201031028`; davor Audit-Status `7200667496`.
- Lokaler Worktree: `/tmp/opencode/issue601-p0-audit-20261126`, Branch `opencode/issue601-p0-audit-20261126`, HEAD `4a292070`, clean.
- **Nächster Schritt:** Claude prüft die Issue-Abnahmekriterien, dokumentiert fachliche ENDE-Meldung und entscheidet, ob #601 geschlossen werden kann. Kein neuer Produktcode/PR aus diesem Audit erforderlich.
- Issue: https://github.com/jochenkunkel/pulselibre/issues/601

### #599 – sichere Wiederaufnahme (offener nächster Schritt)
- Sicherer Fallback: nach Timeout/App-Neustart startet der nächste manuelle Versuch eine frische ItemList und pullt alle gelisteten Dateien erneut. `CirqaTargetedPullPolicy.shouldPull` skippt nicht; lokale UID-/Metadatenstabilität oder vorhandener Import beweisen keine unveränderte Remote-Datei. Byte-/Hash-Deduplizierung geschieht nach erneutem Empfang lokal. `runDrained` bedeutet nicht vollständige Gerätehistorie; Coverage bleibt `device-history-unverified`.
- Draft-PR #713, aktueller Head `4aef4748`, Branch `opencode/issue599-safe-resume-20261126`, lokaler Pfad `/tmp/opencode/issue599-safe-resume-20261126`, clean.
- PR enthält JVM `CirqaTargetedPullManifestTest#afterProcessRestartPreviouslyImportedRemoteItemIsPulledAgain` und Android instrumentation `CirqaTargetedPullManifestRestartTest#persistedImportAndStableMetadataStillCauseFullPullAfterManifestRecreation`.
- QUICK auf `4aef4748` SUCCESS (Build/Unit/Lint/APK, Dependency Review, Gradle dependency graph); API-35-Job SKIPPED, da QUICK. Issue-Kommentar `7200885064`.
- **Offen und konkret an Claude übergeben:** gezielte JVM- und Instrumentationstests plus angefordertes FULL/API-35 auf finalem PR-Head durchführen/prüfen, exakte Testnamen/SHA/Gates in Issue dokumentieren. Bis dahin PR Draft lassen, nichts mergen. Keine Hardware-/BLE-Aktion.
- Issue: https://github.com/jochenkunkel/pulselibre/issues/599

## Research-Register: technische Erkenntnisse und Grenzen

Die folgenden Punkte sind die verdichteten eigenen Offline-/Code-/Capture-Erkenntnisse aus der #637-Arbeit. Sie sollen Claude davor bewahren, Chat-Kontext rekonstruieren zu müssen. Maßgeblich bleiben die verlinkten versionierten Berichte; Roh-Captures, Original-FITs, Kennungen, Digestwerte und Gesundheitswerte sind privat und werden nicht weitergegeben.

### #637: Transport, ItemLists und History-Vollständigkeit

- **Keine Vollständigkeitsmetrik aus `runDrained`:** Ein erfolgreicher/leer beendeter begrenzter Lauf zeigt nur, dass die aktuell abgefragten Antworten verarbeitet wurden. Weder `runDrained=true`, eine leere Folgeantwort noch eine hohe Zahl vollständiger Pulls beweist, dass alle historischen Gerätedaten gelistet wurden. Coverage muss `device-history-unverified` bleiben, solange kein unabhängiges Geräteinventar vorliegt.
- **Unsichere Identität darf keinen Skip auslösen:** UID, Typ, Größe, Flags, lokales Manifest, Importstatus und lokale Hashes belegen nicht, dass ein Remote-Objekt bytegleich geblieben ist. Jeder gelistete Eintrag wird innerhalb bestehender Budgets erneut read-only gepullt; erst empfangene Bytes können lokal byte-/hashbasiert dedupliziert werden. Unbekannte Referenzfelder und Originalbytes müssen verlustfrei erhalten bleiben. Keine Budgets still erhöhen.
- **Transaktions-/Filterhypothese bleibt Hypothese:** Ein Connect-Capture zeigt ItemList-Folgeanfragen mit `exclude_all_flags`, `get_flags`, `included_data_types`-Feldformen zusätzlich zum Transaktionstoken; PulsLibre-Folgeanfragen c16 wiederholten nur den Token und lieferten drei vollständige leere Antworten. Die konkreten Werte und die Agent-spezifische Filterkonfiguration dürfen nicht geraten werden: kopierte Filter könnten Kategorien ausschließen. Das beweist weder, dass Connect mehr CIRQA-Daten abruft, noch warum PulsLibre eine bestimmte Liste erhielt. Keine guessed UUIDs/SetFlags/Filteränderung.
- **Zahlen stammen aus getrennten Läufen:** 27 (zwei verschiedene 27er-Läufe), 29 (PulsLibre, 25.11.), 38 (c16), 39 (Connect, 24.11.), 31 FIT (älteres V24/V25-Archiv) und der spätere 28/29-Lauf sind keine identische Stichprobe und nicht zu einer Coverage-Zahl addierbar.
- **Connect-vs-PulsLibre / normale Tageshistorie:** Ein 680-Zeilen-CSV-Delta am 24.11. belegt nur importierte Aktivitätsdaten (340 HR + 340 Distanz), nicht den Inhalt aller FIT-Dateien. Der private Connect-Capture enthält mindestens einen CRC-validen FIT-Inhalt mit Daily-History-artigem Global 398 und vielen Global-397-Records; Request-/FileAccess-/Item-Korrelation und vollständiger Sync fehlen. Global 398 heißt im Garmin-Profil 21.222.0 `skin_temp_overnight`; Teile sind profildefiniert, einzelne Felder/Einheiten/Sentinel-Regeln bleiben offen. Global 397 ist im Profil nicht definiert und bleibt opaque. Keine Umdeutung in Temperatur-/Gesundheitsmetriken.
- **Typ-32-Pipeline:** Ein separater lokaler Snapshot belegt für drei Typ-32-Dateien Archivierung, Global-55-Feldpersistenz und 1.156 kanonische HR-Beobachtungen mit Provenienz. Ein anderer älterer Snapshot korrelierte 360 von 1.144 HR-Beobachtungen mit vorhandenen CSV-`sample_id`s; die anderen 784 fehlen dort. Backup- und Export-Zeitstände sind nicht nachgewiesen identisch, daher ist die Ursache unbekannt. Das beweist weder Capture-Item-Zuordnung noch vollständige Historie.
- **`result=3`:** Der frühere 0/38-Lauf zeigte `result=3, transport=0, handle=0` und stoppte fail-closed. Seine CIRQA-Ursache ist ungeklärt; `NO_AVAILABLE_RESOURCES` ist nur eine Connect-5.31-Enum-Deutung. Der spätere 28/29-Bericht war beim lesbaren GUI-Auszug auf 4.000 Zeichen gekürzt; die 29. Pull-Antwort und ihre `result`-Zeile sind nicht belegt. Ein weiterer GATT-133-Abbruch geschah vor ItemList/Pull und erklärt den alten Fehler nicht.
- **TransferStatus:** Ein begrenzter Connect-Capture zeigt für 15 PullItem-Antworten korrelierte Handles und 17 TransferStatus-Austausche; zwei korrelierte MLR-Streams erreichten zlib-Ende vor passendem Status-Request/-Response. Das ist mit einem nachgelagerten Statusaustausch vereinbar, beweist aber weder Abschlusssemantik noch die Regel für ein optionales Feld. Keine Status-Antwort-Implementierung allein aus dieser Korrelation ableiten.
- **SQLite-Fehler:** Beim früheren 29er-Test war `SQLITE_BUSY` wiederholt beim Öffnen/Konfigurieren von WAL (`PRAGMA journal_mode`/teilweise `cache_size`) beobachtet; der UI-Fehler `FIT_IMPORT_COMMIT` umfasste aber Helper-Open bis Close und identifiziert den Fehlerort nicht exakt. Serielle BLE-Callback-Verarbeitung macht eine parallele Datei-Callback-Race nicht automatisch plausibel; getrennte `SQLiteOpenHelper`-Instanzen und andere App-DB-Nutzer können dennoch konkurrieren. Die Prozess-lokale Contention-Erklärung war eine Hypothese, nicht root-cause-bewiesen. #678 und #683 ergänzten begrenzte Retries für konkrete Lockfälle; weitere Produktänderungen nur mit neuer reproduzierbarer Evidenz.
- **Unified-Projektion:** Ein aktiver FIT-Decoder-Generationswechsel konnte bei gleichen Zeilenzahlen eine alte Unified-History-Projektion stehen lassen. Die Signatur wurde um aktive `(source_sha256,generation_id)`-Paare ergänzt; Regressionstest kompiliert und JVM-Tests waren grün, lokaler Instrumentationstest wurde nicht ausgeführt. Das ist ein DB-Projektions-/Cachebefund, keine Erklärung für BLE-Listung oder Gerätevollständigkeit.
- **Manueller Verlauf 26.11.:** GUI las unter Lease nur den gespeicherten Bericht `bc355145cf95`: 29 ausgewählt, 28 verifiziert/archiviert, letzte Write angenommen aber keine Antwort vor 120-s-Grenze; `runDrained=false`. 28 vollständige Originalstreams aus dem privaten Backup waren SHA-256-/Chunk-validiert. Aggregate der Offline-Migration/Backfill dürfen nicht ausschließlich dem Bluetooth-Lauf zugeschrieben werden.

### #707: FIT-Semantik und offizieller SDK-Abgleich

- Der Vergleich mit Garmin SDK `21.222.0` wurde ausschließlich auf synthetischen CRC-validen Vektoren durchgeführt. 32/33 vergleichbare Feldinstanzen stimmten überein; die Abweichung ist PulsLibre-seitig bewusstes Verwerfen von HR=0 als Messwert. Ein `timestamp_16`-Vektor war nicht direkt vergleichbar, weil das SDK nur den Rohwert ausgab und keine Rekonstruktion vornahm.
- Originaldateien waren beim GUI-Agenten nicht verfügbar. Das Ergebnis ist kein Genauigkeitsprozentsatz realer Dateien oder des Bestands. `233/397` bleibt Profil-/Semantik-unbekannt; SpO₂-Confidence ist nicht validiert. Rohfelder behalten/inventarisieren, nicht als bestätigte Messwerte kanonisieren.
- Primärquelle und reproduzierbarer Bericht: `docs/protocol/research/ISSUE707_GARMIN_SDK_SYNTHETIC_VALIDATION_2026-11-26.md` im #707-Worktree; offene semantische Fragen im Issue #707.

### Maßgebliche lokale Quellen im dirty #637-Worktree

- `docs/protocol/research/CIRQA_CONNECT_NORMAL_HISTORY_COMPARISON_2026-11-25.md` – ausführliche Evidenzmatrix für CSV, Connect, FIT 397/398, Typ-32-Pipeline/Export und getrennte Laufzahlen.
- `docs/protocol/research/ISSUE637_CHATGPT_HANDOFF_2026-11-25.md` – SQLite-Fehlergrenzen, konkurrierende Helper-Hypothese, historische Connect-Spuren und vorgeschlagene Offline-Prüfungen.
- `docs/protocol/research/CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md` – beobachtete Request-Feldformen, Filterhypothese und Guardrails.
- `docs/protocol/research/CIRQA_CONNECT_TRANSFER_STATUS_CORRELATION_2026-11-24.md` – Handle-/Status-Korrelation samt Interpretationsgrenzen.
- `docs/protocol/research/CIRQA_ALL_DATA_FRESHNESS_IMPLEMENTATION_2026-11-23.md` – Always-pull-/byte-retention-Entscheidung und Abnahmegrenzen.
- `docs/protocol/research/CIRQA_ACTIVE_GATT_PATH_AUDIT_2026-11-20.md` – statische Unterscheidung zwischen Discovery, aktivem ML/GFDI und manuellem History-Pfad; kein Hardware-Erfolgsbeleg.
- `docs/protocol/device-tests/CIRQA_RESULT3_ONE_SHOT_RETEST_2026-11-26.md` befindet sich **nicht** im oben genannten dirty Worktree, sondern in `/tmp/opencode/issue637-result3-diagnostic-20261126`; der Bericht enthält die 28/29- und `result=3`-Grenzen.
- `docs/protocol/research/ISSUE637_UNIFIED_HISTORY_PROJECTION_INVALIDATION_2026-11-26.md` liegt ebenfalls im Worktree `/tmp/opencode/issue637-result3-diagnostic-20261126`.
- Die ergänzende `CIRQA_CONNECT_NORMAL_HISTORY_COMPARISON_2026-11-25.md` und `ISSUE637_CHATGPT_HANDOFF_2026-11-25.md` enthalten private-Daten-freie Forschungszusammenfassungen; bei Detailfragen die Quellen vollständig lesen und Capture-Rohdaten nicht in Git/Issues übernehmen.

## Weitere relevante lokale Pfade

- Handoff-/Koordinationsbranch, von dem dieses Dokument stammt: `/tmp/opencode/claude-complete-handoff-20261126`, Branch `opencode/claude-complete-handoff-20261126`.
- Arbeitskopie mit Zwischenabgleich #599/#601: `/tmp/opencode/issue601-p0-audit-20261126`, Branch `opencode/issue601-p0-audit-20261126`, HEAD `4a292070`, clean. Sie enthält auch den frühen #599-START im agent-log; maßgeblich für Code/Test-PR ist der separate #599-Worktree oben.
- Ursprünglicher IDE-Checkout laut Session-Umgebung: `/tmp/opencode/issue637-p0-history-followthrough-20261125`. Das ist der dirty Arbeitsstand oben; nicht mit dem sauberen Hauptcheckout `/home/smiler/04_src/pulslibe/pulselibre` verwechseln.

## Arbeitsregeln für die Übernahme

1. Claude prüft zuerst den dirty #637-Worktree und übernimmt danach #599-Tests; #601 kann nach grüner FULL-Abnahme abgeschlossen werden. #707/#628 bleiben offen, wenn die benannten Forschungslücken weiter bestehen.
2. Vor jeder Änderung `git status`, `git fetch origin`, Agenten-Chat und offene `agent-sync`-Bitten prüfen. Keine gemeinsame Bearbeitung derselben Branches/Dateien ohne Abstimmung.
3. OpenCode hat keine Tests/Installationen im vorstehenden Audit ausgeführt. GUI-Berichte/Actions wurden als externe Nachweise referenziert.
4. Keine privaten FIT-Rohdaten, Gerätekennungen, Tokens oder Secrets in Git/Issues. Keine Aussagen über vollständige History, `result=3`-Ursache oder reale SDK-Genauigkeit ohne Beleg.
5. Keine Tests oder Installationen auf dem S25. S10 nur bei konkret nötiger Hardwarearbeit mit gültiger S10-Lease, Backup-/Signaturkontinuität und dem geltenden Freigabe-/Stop-Protokoll.
6. Für #599 keinen Remote-Skip/Transport-/Budget-Fix einführen, solange keine belastbare Remote-Unverändertheit bewiesen ist. FULL-Pull ist sicherer Fallback; Limits nicht still erhöhen.
7. Keine Pflichtchecks umgehen; FULL ist vor Merge nicht generell nötig, aber #599-Ticket verlangt ausdrücklich FULL/API-35. Für Releases gilt FULL auf Release-Commit.
