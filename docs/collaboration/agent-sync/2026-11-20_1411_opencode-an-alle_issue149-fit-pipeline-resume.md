# Wiederaufnahme und Übernahme der FIT-Pipeline-Arbeit (#149)
Von: OpenCode · An: alle · Datum/Uhrzeit: 2026-11-20 14:11 UTC · Bezug: Issue #149; aktueller Branch ab `5d22561`

## Bestätigte Fakten
- Nutzer hat OpenCode erneut ausdrücklich mit der FIT-Pipeline beauftragt, den Arbeitsbereich `/home/smiler/04_src/BLE-FIT-CIRQA` festgelegt und die Integration nach Tests/Full-CI genehmigt.
- Der aktuelle Branch basiert auf `origin/main` `5d22561`; frühere `/tmp/opencode`-Worktrees sind nicht verfügbar. Kein bestehender Checkout mit lokalen Änderungen wird überschrieben.
- #149 ist weiterhin offen. PR #289 hat den bounded Offline-Backfill beigetragen; #304 war ein Status-Audit. Die offenen Abnahmen umfassen Importstatus nach SQLite-Commit, Fehler-/Retry-Pfade, per-Datei-Abdeckung und semantische Decoderprüfung.
- S25 ist via ADB sichtbar; installiert ist `2.0.0_build1438`. Bisher erfolgten nur Gerätemodell- und Paketversionsabfragen, keine App-Start-, Installations-, Instrumentations- oder BLE-Aktion.

## Empfehlung / Bitte
- OpenCode übernimmt #149 und prüft zuerst den aktuellen produktiven MLR→FIT→SQLite-Codepfad. Kleine, schemafreie Status-/Ergebnis- und Fehlerpfadkorrekturen werden mit Regressionstests umgesetzt.
- Für den S25 nur sichere read-only Diagnosepfade nutzen, nach Codeprüfung und ohne `run-as`-Umgehung, Datenänderung, Instrumentation oder neue BLE-Kommandos. Ein erforderlicher aktiver proprietärer BLE-Test bleibt separat freigabepflichtig.
- Merge erfolgt erst nach Prüfung des vollständigen grünen Android-CI-Laufs auf PR-Head und anschließendem Full-CI auf `main`.

## Unsicherheiten
- Der bekannte lokale und S25-Inventurstand (40 FIT / 294 Kandidaten) belegt weder die vollständige FIT-Feldsemantik noch die gesamte CIRQA-Gerätehistorie.
- Ursache und Testbarkeit eines SQLite-Commitfehlers im Live-Transferpfad sind zunächst noch durch Codeprüfung zu bestätigen.

## Nicht anfassen
- Keine DDL-/Backupänderungen aus #144, UI-Änderungen, privaten FIT-/Raw-Daten in Git, V24/V25-Gate-Reset, BLE-Transportänderung oder S25-Instrumentation.
