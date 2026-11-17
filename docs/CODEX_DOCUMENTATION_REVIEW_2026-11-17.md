# Dokumentationsabgleich für Codex (2026-11-17)

**Status: Review-Vorlage; kein vollständiges fachliches Audit sämtlicher Dokumente.** Dieser Abgleich dient als nachvollziehbarer Einstieg für Codex und menschliche Reviewer. GitHub-`main` und CI/Hardware-Evidenz sind maßgeblich.

## Umfang der Prüfung

- **Inhaltlich gelesen/abgeglichen:** `README.md`, `docs/CHAT_START_HERE.md`, `docs/PROJECT_STATUS.md`, `docs/PROJECT_HISTORY.md` (historische Auszüge), `docs/DEVELOPMENT_PLAN.md`, `docs/QUALITY_GATE.md`, `docs/CI_TEST_STRATEGY.md`, `docs/AUTONOMOUS_DEVELOPMENT_AGENT.md`, `docs/ENGINEERING_SCIENTIFIC_STANDARDS.md`, `docs/ROADMAP_TO_CIRQA_DATA.md`, `docs/CIRQA_HISTORY_SYNC_PLAN.md`, `docs/CIRQA_5050_THIRD_TEST_AND_PHASE_FIX_2026-11-17.md`, `.github/workflows/android.yml` und die aktuelle 5050-Diagnoseimplementierung.
- **Dateiinventar gesichtet:** Die im Git-Tree vorhandenen `docs/*.md`-Dateinamen einschließlich CI, Datenschutz, BLE, Speicher, Dashboard, UI und Release. **Nicht alle Texte wurden zeilenweise gelesen**; deren Vollprüfung bleibt ausdrücklich offen.
- **Weitere Quellen:** Vom Nutzer im Projektchat übermittelte, redigierte CIRQA-Diagnoseberichte und verifizierter schneller Android-CI-Lauf `44045596436`. Keine privaten Rohframes oder Gerätedaten übernommen.

## Festgestellte Abweichungen und Korrekturen

1. **CI-Gates widersprüchlich:** `docs/CI_TEST_STRATEGY.md` behauptete Full-Checks bei Pull Requests und `full_tests`-Default `true`; `docs/QUALITY_GATE.md` nannte ebenfalls PR-Full-Checks. Tatsächlich führt `.github/workflows/android.yml` Full-Gates bei `v*` oder manuell mit `full_tests=true` aus; Default ist `false`. Beide Dokumente korrigiert.
2. **APK-Verfügbarkeit ungenau:** Die CI-Strategie versprach Upload der Debug-APK bei jedem Build. Tatsächlich erfolgt Upload nur bei erfolgreichem Lauf und eingerichteter stabiler Signing-Konfiguration (und nicht bei PR). Präzisiert.
3. **Projektstatus veraltet:** Der kanonische Status enthielt die jüngsten 5050-ACK-/Post-Write-Ergebnisse nicht. Aktuelle, ausdrücklich begrenzte Evidenz ergänzt; keine Historienfreigabe behauptet.
4. **Codex-Zusammenarbeit fehlte:** `AGENTS.md` ergänzt, bestehende Einstiegspunkte verlinkt. Für den vorliegenden Auftrag Review-Branch/PR statt Direkt-Merge; bestehende Nutzerpräferenz für spätere, gesonderte Entwicklungsaufträge nicht pauschal überschrieben.
5. **Historische Aussagen:** Ältere Architektur-, Entwicklungs- und Projektchronikdokumente enthalten frühere, heute nicht mehr aktuelle Test-/Protokollstände. Diese bleiben als datierte Zeitzeugen erhalten; für aktuelle Entscheidungen auf `PROJECT_STATUS.md` und `AGENTS.md` verwiesen statt historische Einträge umzuschreiben.

## Offen zur fachlichen Folgeprüfung

- Zeilenweise Vollprüfung der übrigen Protokoll-, Datenschutz-, UI-, Datenbank-, Release- und Wissenschaftsdokumente; insbesondere zeitlich widersprüchliche Aussagen markieren.
- Vollständige Code-/Testinventur gemäß Issue #6 und offene Reviews #2–#5; ein Dokumentationsabgleich ist **kein** vollständiger Codeaudit.
- Signing-Secrets, stabile APK-Updates mit Datenerhalt und vollständige Lint-/Emulator-CI auf dem exakten Release-Commit bleiben separat nachzuweisen.
- Die korrekte CIRQA-Host-Capability-Aushandlung und der Start des Historien-Backfills sind **ungeklärt**. Keine neue BLE-Schreibsequenz aus Dokumentationshypothesen ableiten.
- Wissenschaftliche Referenzen: `docs/ENGINEERING_SCIENTIFIC_STANDARDS.md` verweist auf `docs/SCIENTIFIC_REFERENCES.md`; diese Datei war im geprüften Git-Tree nicht vorhanden. Prüfen, ob eine Referenzliste neu benötigt wird, bevor wissenschaftliche Algorithmen als validiert gelten.

## Review-Checkliste

- Stimmen `AGENTS.md`, `CHAT_START_HERE.md` und `PROJECT_STATUS.md` überein?
- Beschreiben CI-Strategie und Quality-Gate korrekt **Ist** vs. **Soll**?
- Bleiben sensible CIRQA-Daten und Secrets außerhalb von Git?
- Sind keine Anwendungscode-/Workflow-Dateien geändert?
- Keine stillschweigende Behauptung eines vollständigen Dokumentationsaudits oder einer Freigabe.
