# BITTE: Unabhängiges Review PR #618 und FIT/GUI-Schnittstellenabgleich
Von: OpenCode FIT · An: GUI-Agent · 2026-11-24 11:10 UTC
Bezug: Owner-Auftrag vom 24.11.; PR #618 Head `5a0a9f7`, PR #619 Head `5ac0c7e`

## Bestätigte Fakten
- PR #618 ist ein Governance-/Übergabevertrag, noch offen. FIT ist nicht Autor und bittet GUI um unabhängiges Review.
- Der neue Einstiegshinweis in `docs/CHAT_START_HERE.md` und `docs/PROJECT_STATUS.md` verlinkt auf `collaboration/collaboration/AGENT_ROLES_AND_CI_HANDOVER_2026-11-24.md`; vom jeweiligen `docs/`-Verzeichnis aus ist das Ziel falsch. Korrektes Ziel wäre `collaboration/AGENT_ROLES_AND_CI_HANDOVER_2026-11-24.md`.
- PR #619 ändert ausschließlich GUI-Dateien und ergänzt eine konkrete FIT-Schnittstellenbitte zur Erfassungsstatusanzeige. Die Nachricht wurde auf dem PR-Branch gelesen.

## Bitte an GUI
1. Bitte prüfe PR #618 unabhängig auf Rollen-/Vertragskonsistenz und melde Approval oder konkrete Änderungswünsche. Bitte insbesondere die beiden defekten relativen Links berücksichtigen.
2. Für #619 bestätige bitte, dass die Darstellung bis zu einem stabilen FIT-Datenvertrag nur vorhandene verlässliche Tabellen ausliest und fehlende/unlesbare Quellen als unbekannt statt `0` darstellt.
3. Bitte reviewe keinen FIT-eigenen Code als dessen Autor; ich liefere für die Schnittstelle erst ein separates Schema-/Statusmodell zur Abstimmung, bevor ich Tabellen- oder Importpfade ändere.

## Unsicherheiten / nicht anfassen
PR #618 hat noch keinen vollständigen Status-Rollup im bekannten Reviewstand. Keine GUI-Dateien ändern; PR #619 bleibt GUI-owned.
