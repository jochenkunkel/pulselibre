# Dokumentationskonsolidierung – Status und Grenzen

> **Stand 24.11.2026:** Die Projektleitung wählte Richtung B. 47 ausgewählte, historische Planungs-, Review- und Zwischenstands-Dokumente wurden vollständig nach [`../archive/2026-11-24/README.md`](../archive/2026-11-24/README.md) verschoben. **Nichts wurde gelöscht.**

## Was zusammengeführt wurde

`docs/knowledge/00_INDEX.md` ist der kuratierte Einstieg. Die sieben Themenseiten bündeln:

- BLE-Protokoll und Evidenzstatus
- Feldkatalog
- Parser-, Import- und Persistenz-Datenpfad
- Geräte-Evidenz
- offene Fragen
- Collaboration-Leseführung
- diese Archivierungsübersicht

Die Wissensseiten sind Navigations- und Zusammenfassungsdokumente, keine Ersatzquelle. Maßgebliche Spezifikationen, aktuelle Projektregeln, FIT-Matrizen und Gerätebelege bleiben an ihren fachlichen Orten und sind vom Index verlinkt.

## Archivierte Gruppen

Das vollständige, dateiweise Manifest mit Original- und Archivpfad steht im [Archiv-README](../archive/2026-11-24/README.md). Die 47 Dateien gehören zu diesen Themen:

| Themenbereich | Beispiele |
|---|---|
| Frühere Produkt-, Roadmap- und Speicherstände | `APP_VISION.md`, `DEVELOPMENT_PLAN.md`, `LOCAL_STORAGE.md` |
| Historische Audits, Reviews, Test- und Incident-Notizen | `AUDIT_*`, `CODE_REVIEW_*`, `CI_INCIDENT_*` |
| Protokollrecherche und Quellenvergleiche | CIRQA-5050-/5024-Referenzanalysen |
| Historische Sync-, Experiment- und Geräteplanung | frühere Sync-Pläne und Testvorschläge |
| CI-Übergaben und punktuelle Offline-Recherche | Runner-Migration, Coverage, Offline-Inventur |

**Umfang:** 47 Originaldokumente. Die vollständige Dateiliste steht im Archivmanifest.

Die Kategorien beschreiben historische Dokumente; das Archiv bewahrt die Dokumente und ihre Git-Historie. Ein vollständiger BLE-Rohframe wurde auf spätere Nutzeranweisung aus der aktuellen Archivfassung entfernt; frühere Versionen bleiben historisch in Git. Interne Verweise wurden angepasst; alte direkte Dateipfade können sich ändern.

## Ausdrücklich nicht verschoben

- `docs/collaboration/**` vollständig; aktueller Bestand und Einstieg in [`06_collaboration-daten.md`](06_collaboration-daten.md).
- Aktuelle Projekt-/Qualitätsregeln, zentrale Protokollspezifikation und aktuelle Statusdokumente.
- Primäre, redigierte Geräteberichte, geprüfte synthetische Fixture-Manifeste und FIT-Feldmatrizen; keine BLE-Rohmitschnitte.
- Datenschutz-, Sicherheits-, Lizenz- und Provenienznachweise.
- Geparkte Architekturarbeit #496.

## Grenzen und Pflege

- Es wurde **nichts gelöscht**. Eine spätere Löschung erfordert eine neue, konkrete Freigabe und einen Verweischeck.
- Bei historischen Befunden sind Datum, Geltungsbereich und spätere Korrekturen mitzulesen; das Archiv ist kein aktueller Handlungsplan.
- Neue Erkenntnisse zuerst in die jeweilige kanonische Quelle einarbeiten, danach den Index bzw. die passende Wissensseite aktualisieren.
