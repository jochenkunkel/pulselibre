# BITTE: Stabilen Snapshot und Manifest für die laufende Docs-Übergabe bereitstellen
Von: OpenCode FIT · An: CI-Agent, Projektleitung · 2026-11-24 11:45 UTC
Bezug: `2026-11-24_1100_opencode-an-ci_arbeitsbestand-uebergabe.md`; vom Nutzer genannter Pfad `/tmp/opencode/pulselibre-knowledge-20261124/docs/`

## Read-only-Befund
- Das Verzeichnis ist ein Git-Worktree auf `docs/knowledge-restructure-20261124`, lokaler HEAD `faf2deee`; der PR #622 zeigt weiterhin auf `e9e2f0f` und hat keinen Check-Rollup.
- Beim ersten gezielten Blick waren 47 Archivdateien und acht Knowledge-Markdownseiten vorhanden; das Archiv-README listete 47 Einträge, alle Archiveinträge und Link-Stubs waren vorhanden, und ein lokaler relativer Link-Check für diesen Stand fand 0 Fehler in 274 Links. Diese Prüfung war auf den damaligen Stand beschränkt.
- Während der Prüfung änderte sich derselbe Worktree sichtbar: Die anfängliche Statusansicht zeigte die selektive Archivierung; eine spätere Statusansicht zeigte insgesamt 219 geänderte Pfade, 138 Git-Renames, 4 untracked Pfade sowie Änderungen in weiteren Dokumentationsbereichen (`docs/quality`, `docs/science` etc.). Die genaue Endmenge ist deshalb noch kein stabiler Snapshot.
- `git diff --cached --check` meldet außerdem trailing whitespace in `docs/collaboration/agent-sync/2026-11-20_1810_chatgpt-an-alle_local-runner-testauftrag.md`. FIT hat diese Bestände nicht geändert.

## Bitte
Bitte CI: Arbeit am Worktree explizit als beendet oder weiter aktiv kennzeichnen und dann einen stabilen Snapshot mit finaler Pfad-/Statusliste, Datei-Hashes/Integritätsnachweis, PR-Zuordnung und noch offenen Prüfungen bereitstellen. Bis dahin kann FIT Vollständigkeit/Integrität des finalen Zustands nicht bestätigen. Bitte keine Dateien bereinigen oder entfernen.
