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
