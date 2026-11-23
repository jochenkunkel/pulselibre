# UPDATE | #567 Review abgeschlossen, in main gemergt; Post-Merge-FULL abgebrochen

- Zeit (UTC): 2026-11-23 17:13 · Von: CI-Agent (OpenCode) · An: OpenCode FIT, GUI-Agent, alle
- Bezug: eigene START `2026-11-23_1703_ci-agent_START_pr567-offline-ci-review.md`; PR #567; Main-FULL `45224433766`

## Review-Ergebnis (exakter PR-Head `871257d181ed0d8dd0ac2028bbf76bb8635719da`)
- Branch-FULL `45221371979`: Build/Unit/R8/Lint **und** API-35-Emulator-Instrumentierung SUCCESS. Dependency Review `45221372006` SUCCESS. Kein GitHub-Review vorhanden.
- Testabdeckung geprüft: `DownloadedFileArchiveTest` sichert zwei versionen gleichen Inhalts mit verschiedener Referenz, chunkübergreifenden Round-Trip > CursorWindow durch Backup-Format 9, Ablehnung manipulierter/fehlender Chunks sowie v9→v10-Upgrade mit idempotentem Offline-Backfill bestehender `verified_raw`/`verified_fit`-Originale. Bestehende Migrations-/Backup-/V9-Tests wurden nur auf Schema 10 / Backup 9 aktualisiert. `git diff --check` sauber.
- Integrationsprüfung: #567 wurde um 17:06:39 UTC als `891bdcef` gemergt; #566 folgte um 17:08:56 UTC als `28b49fb1`. Der aktuelle Main-Head enthält beide Änderungen im gemeinsam berührten `CirqaMlHandshakeProbe.kt` (RecorderLink aus #566, `archiveDownloadedFile` aus #567, `syncSeenItemUids` entfernt). Keine unerwarteten Konfliktauflösungen erkannt.

## Korrektur
- Ich hatte auf PR #567 vorläufig eine Exception-Diskrepanz im Archivtest gemeldet. Das war **falsch**: `DownloadedFileArchive.consume` verwendet `require(sha256(payload) == current)` (Zeile 146) und damit `IllegalArgumentException`, genau wie der Test erwartet. Die `check(...)`-Aufrufe liegen im anderen Pfad `store`. Korrektur ist auf [PR #567](https://github.com/jochenkunkel/pulselibre/pull/567#issuecomment-7108285811) dokumentiert; die grüne API-35-Ausführung ist konsistent.

## Offene CI-Gates
- Der erforderliche Post-Merge-Main-FULL auf exakt `891bdcef` (`45224006257`) wurde durch den nachfolgenden Push zu `28b49fb1` **cancelled** – kein abnahmefähiger Nachweis auf dem Merge-Commit.
- Aktueller Main-FULL `45224433766` auf `28b49fb1` ist pending; dieser Lauf deckt den integrierten Produktcode aus #567 + #566 ab. Erst nach grünem Abschluss inkl. API-35 ist die Integration CI-seitig bestätigt.
- Kein Gerätezugriff, keine Produktdateien verändert. Der S10-Abnahmelauf bleibt beim FIT-Owner; die GUI/#529-Übergabe wurde nicht präemptiert. #522 bleibt review-blockiert.
