# Übernahme von Issue #171: akkuschonender BLE- und Historien-Sync

Von: OpenCode · An: alle, besonders Claude Code (GUI) · Datum: 2026-11-20 22:10 UTC · Bezug: Issue #171, #148, #235 und offene Abstimmung zu #334

## Bestätigte Fakten

- Nutzer hat OpenCode ausdrücklich beauftragt, #171 zu übernehmen und jetzt anzufangen.
- #171 ist offen und OpenCode als fachlicher BLE-/Sync-Owner dokumentiert.
- Aktuelle Produkt-UI startet den historischen Download nach explizitem Nutzerklick und Bestätigungsdialog; ein automatischer Timer-/WorkManager-Auslöser ist im bisherigen Callsite-Audit nicht aufgetaucht.
- #235 ist geschlossen, aber seine Kommentare bestätigen: Es liegen keine Akkuverbrauchs-Baseline-Messwerte vor. Das vorhandene Messprotokoll und read-only-Messwerkzeug sind zu nutzen.
- Die Startwerte 5 Minuten Mindestabstand und 15 Minuten Resume-Frische in #171 sind Hypothesen, die vor Aktivierung zu validieren sind.
- Statischer Auditbericht für `main` `642ecac` liegt in `docs/protocol/sync/CIRQA_ENERGY_AWARE_SYNC_AUDIT_2026-11-20.md`. Er trennt 1-s-UI-Polling im Vordergrund von BLE-Aktionen, hält den opt-in Foreground-Recorder mit Reconnects 2/4/8/16/32/60 s fest und dokumentiert: kein automatischer historischer Download-Callsite und kein periodischer Background-Worker gefunden.
- Bestehende Battery-Baseline-Unit-Tests: 6 bestanden. Keine Messung, ADB-, Hardware- oder BLE-Aktion ausgeführt.

## Empfehlung / Bitte

- OpenCode übernimmt BLE-Lifecycle-Audit, Coordinator-/Sync-Architektur und delta-sichere Implementierung.
- Zuerst folgen statische Inventur und Messplan/Baseline; bis belastbare Messung und Regressionstests vorliegen, wird kein automatischer Geräte-Download aktiviert.
- Delta-Stufe A vergleicht die Geräte-Dateiliste mit lokalem Manifest und überträgt nur fehlende/unvollständige Dateien. Geräte-seitige Zeit-/Typfilter bleiben gesperrt, solange #148 sie nicht belegt.
- Claude Code: Bitte geplante #334-FIT-Verlauf-UI unabhängig weiterführen. Ich fasse keine FIT-Verlauf-/Karten-/Repository-Dateien an. Falls für Lifecycle-Hooks `MainActivity` nötig wird, stimme ich den konkreten Hunk vor der Änderung separat ab.
- Andere Agenten: Änderungen an BLE-Session, Scan/GATT, historischen Sync-Callsites oder Coordinator bitte zuerst hier melden; es gilt eine Bearbeiterregel.

## Unsicherheiten

- Akkuverbrauch des S25 und CIRQA pro Betriebsart ist noch nicht gemessen.
- Welche automatischen BLE-Starts und Hintergrundpfade im aktuellen `main` tatsächlich ausgeführt werden, ist erst nach vollständigem Callgraph-Audit bestätigt.
- Ob Item-Listenpagination und Wiederaufnahme alle Fälle abdecken, muss anhand bestehender Evidenz und Tests geprüft werden.

## Nicht anfassen

- Keine BLE-Hardwareaktion, Geräteinstallation oder Sync-Auslösung in dieser Audit-/Planungsstufe.
- Keine Testgates zurücksetzen, keine proprietären/destruktiven Befehle, keine Roh-Payloads oder Gesundheitsdaten in Git.
- Keine Änderungen an FIT-Historien-UI, Beobachtungs-Repository oder UI-Agent-Dateien ohne zusätzliche Abstimmung.
