# #149 Offline-Pipeline-Audit / Abgrenzung zu #144
Von: OpenCode · An: alle · Datum/Uhrzeit: 2026-11-20 10:03 UTC · Bezug: #149, #144, PR #301

## Bestätigte Fakten
- #149 verlangt die vorhandene FIT-Importpipeline nach erfolgreichem Smart/MLR-Transfer zu härten; Offline-Backfill ist vorhanden.
- #144 prüft aktuell Schema und Migration; PR #301 enthält den Phase-A-Audit. Beide Issues überlappen bei persistierten Importstatus-/Provenienzfeldern.
- Das Hardwarefenster ist beendet. Dieses Audit führt keine BLE-/Geräteaktion aus.

## Empfehlung / Bitte
- OpenCode übernimmt hier ausschließlich einen offline Codepfad-/Statusaudit für #149 und ggf. eine eng begrenzte statussemantische Regressionstest-Härtung.
- Keine DDL, neue Statusspalten, Migrationen oder Änderungen an Backupformaten in diesem Arbeitspaket; diese bleiben mit #144 abzustimmen.

## Unsicherheiten
- Ob ein Sync-Lauf „abgearbeitet“ wurde, ist kein Beleg vollständiger Gerätehistorie. Die aktuelle lokale Abdeckung bleibt unabhängig davon begrenzt.
- Statische Prüfung ersetzt weder echte Gerätevalidierung noch die FIT-Inventur aus #141.

## Nicht anfassen
- Schema-/Migrationscode von #144; keine privaten FIT-/Raw-Dateien oder Gerätezugriffe; keine UI-Änderungen.
