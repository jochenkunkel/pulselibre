# PulsLibre – Dokumentationsabgleich in zwei Prüfdurchgängen (2026-11-19)

**Auftrag:** GitHub-Dokumentation quellengebunden zusammenfassen, Widersprüche entschärfen, nachvollziehbare Tabellen/Diagramme liefern. **Scope:** Dokumentation und Issues, **keine Android-Quellcodeänderung**. Zeitgebundene Aussagen werden nicht als Echtzeit-/Hardwarevalidierung ausgegeben.

## Quellen und Beleggrenze
Direkt auf GitHub-`main` eingesehen: `README.md`, `AGENTS.md`, `.github/workflows/android.yml`, `docs/PROJECT_STATUS.md`, `docs/CHAT_START_HERE.md`, `docs/DASHBOARD_IMPLEMENTATION.md`, `docs/science/SCIENTIFIC_DASHBOARD_TILE_BLUEPRINT.md`, `docs/science/ANALYSEN_NAVIGATION_CONCEPT.md`, `docs/protocol/data-model/CIRQA_DASHBOARD_GROUND_RULES.md`, `docs/protocol/data-model/CIRQA_SLEEP_MOTION_GROUND_RULES.md`, `docs/CLAUDE_CODE_WORKLOG.md`, `docs/CODE_REVIEW_2026-11-19.md`, `docs/ENGINEERING_SCIENTIFIC_STANDARDS.md`, `docs/ANDROID_RELEASE_READINESS.md`. Die Datei `docs/science/SCIENTIFIC_REFERENCES.md` **existiert nicht**; die Richtlinie verweist stattdessen auf `docs/SCIENTIFIC_REFERENCES.md`. Kein Anspruch auf vollständige Inspektion aller Repositorydateien oder aller CI-/Geräteläufe.

## Durchgang 1 – Befunde, Entscheidung und Korrektur

| ID | Schwere | Quellen / Konflikt | Korrektur |
|---|---|---|---|
| D01 | Hoch | `PROJECT_STATUS.md` enthält einen alten Abschnitt „Aktueller Workflow (nach e4a6e23)“ mit schnellen main-Pushes, während `.github/workflows/android.yml` **aktuell** PR- und main-Push-Full-Gates definiert. | Abschnitt als datierten historischen Zwischenstand markieren, aktuellen Workflow an der Spitze nennen. |
| D02 | Mittel | `CHAT_START_HERE.md`-Willkommensprompt sagt, nur ein Agent sei aktiv und CIRQA-Transport gehöre ChatGPT; `PROJECT_STATUS.md`/Agenten-Sync sehen getrennte Zuständigkeiten und OpenCode-Arbeit vor. | Zeitabhängige Agentenaktivität nicht behaupten; GitHub-Sync und abgestimmte Ownership priorisieren. |
| D03 | Mittel | Neuer Analysen-Reiter #227 steht nur als angehängter Block in drei Fachdokumenten, fehlt in Start-/Statusnavigation. | Kanonische Sitemap verlinken, veraltete Vier-Screens-Anweisung als Bestand statt neue unveränderliche IA behandeln. |
| D04 | Mittel | Wissenschafts-Blueprint Stand 17.11. sagt „Kein validierter CIRQA-Historienimport“, während spätere Berichte einen begrenzten V24/V25-FIT-Download nennen. | Aussage als **17.11.-Snapshot** kennzeichnen; Datei-Download ≠ validierte fachliche Schlaf-/HRV-/FIT-Importsemantik. |
| D05 | Mittel | Dashboard-Bestand 17.11. basiert auf `f17f977` und `codex/dashboard-history`, während #41/#45 inzwischen auf main erweitert wurden. | Expliziter Stichtags-/Fortschrittsverweis, ohne historischen Bestand zu überschreiben. |
| D06 | Mittel | `ANDROID_RELEASE_READINESS.md` beschreibt drei gleichgewichtete Navigationseinträge (Stand 15.11.); #227 plant neuen Reiter. | Historische Aussage zeitlich markieren und auf #227 verweisen. |
| D07 | Niedrig | 3 Fachdokumente duplizieren denselben langen Analysen-Block; dadurch künftige Divergenz. | Kanonische Sitemap `docs/science/ANALYSEN_NAVIGATION_CONCEPT.md` als Single Source of Truth; andere Dokumente kurz verlinken. |
| D08 | Niedrig | README nennt lokale JDK-17-Anleitung, CI nutzt JDK 21; beides ist nicht identisch mit Java/Kotlin-Bytecode-Ziel 17. | Explizit unterscheiden: lokale Hinweise, CI-Laufzeit JDK 21, Bytecode-Ziel 17. |
| D09 | Mittel | Wissenschaftliche Referenzen sind unter `docs/SCIENTIFIC_REFERENCES.md` normiert; `docs/science/SCIENTIFIC_REFERENCES.md` ist nicht vorhanden. | Pfad nicht erfinden; Referenzpfad im Audit und Navigationsdokument festhalten. |

## Aktuelle fachliche Landkarte (Planungsstatus, kein Fertigstellungsnachweis)

| Modulgruppe | Tickets | Fachliches Gate |
|---|---|---|
| Herz & Kreislauf | #41 | Geräte-Ruhepuls nur bei belegter Semantik, Datenabdeckung getrennt von Schritten |
| Schlaf & Erholung | #43, #44 | Schlafintervalle und Nacht-/Folgetag-Zuordnung verifizieren |
| Bewegung & Entwicklung | #46, #47 | Belastungsvergleich nur mit Aktivitätskontext; 90 Tage nur mit echter Historie |
| Zusammenhänge & Muster | #45 | Zeitreihenabhängigkeit, Mehrfachtests, Sensitivität, keine Kausalität |
| Datenqualität & Methoden | #144, #148 | Quellen, SHA/CRC, fehlende Daten, Import-/Sync-Gates |
| Historienvisualisierung | #219 | Messdichte ≠ Gesundheitsbewertung |

```mermaid
flowchart TD
 A["GitHub main + aktuelle Workflow-/Codebelege"] --> B["Projektstatus / Agenten-Übergabe"]
 B --> C["Analysen-Navigation #227"]
 C --> D["Wissenschaftliche Modul-Tickets"]
 D --> E["Methoden + Datenqualität + Tests"]
 E --> F["CI- und S25-Nachweis getrennt"]
```

## Durchgang 2 – Nachprüfung
Nach den gezielten Dokumentationskorrekturen werden alle **veränderten** Dateien erneut direkt aus GitHub gelesen. Geprüft werden: #227-Querverweise, historische statt aktuelle CI-Aussagen, relative Links, Referenzpfad, keine Source-Code-Änderungen, konsistente Begriffe „Übersicht“/„Analysen“, und ehrliche Trennung zwischen Plan/Implementierung/CI/S25. Ergebnisse werden hier im zweiten Abschnitt ergänzt.

## Unverändert offene Grenzen
- Kein Anspruch auf vollständige Inspektion des gesamten Git-Trees, aller Issue-Kommentare, aller aktuellen Actions-Runs oder eines angeschlossenen S25.
- Neue wissenschaftliche Ideen sind Backlog, nicht freigegebene, implementierte oder klinisch validierte Features.
- Ein vollständiger Hardware-/BLE-Test ist nicht Teil dieser Dokumentationsarbeit.
