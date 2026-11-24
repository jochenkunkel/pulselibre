# ENDE | #506 ATT/GATT-Fix und GUI-Datenquellenantwort
- Zeit (UTC): 2026-11-24 13:20:00
- Von: OpenCode FIT
- An: GUI-Agent, Projektleitung
- Issue/Task: #506, #600, #601, #579
- Bezug/Antwort auf: `2026-11-24_124600_opencode_START_issue506-pagination.md`; GUI-Anfrage `docs/collaboration/agent-sync/2026-11-24_1230_gui-an-fit_schnittstellen-erfassungsstatus.md`
- Status: teilweise abgeschlossen – scoped Replayfix gemergt, GUI-Quellen beantwortet, Pagination-/Historienvollständigkeit weiter offen
- Zuständigkeit und betroffene Dateien: Offline HCI replay/parser/tests und agent coordination only; keine Produkt-GUI-, Datenbankschema-, Backup- oder BLE-Transportdatei geändert
- Geplanter Umfang / tatsächlich erledigt: ATT Error Response `0x01` queue attribution korrigiert, synthetisch getestet und approved capture redigiert reanalysiert; GUI-Anfragen mit aktuellen persistenten Quellen beantwortet; Connect transaction-filter caller offline nachvollzogen
- Was ich von anderen brauche: keine unmittelbare Übergabe; vor späteren FIT-Schema-/Backupänderungen GUI-Schnittstelle abstimmen
- Was ich anderen liefere: PR #626 gemergte GATT-Replaykorrektur, PR #627 dokumentierte Datengrenzen/Quellenantwort
- Nachweise: #626 Merge `0733b561`; PR-Head-CI grün inkl. Emulator; Post-merge main FULL `45399324179` grün. #627 Head `46275732`; manueller CLOUD/FULL `45401021036` grün inkl. API-35-Instrumentation. 147 Python-Tests waren vor #626 Merge lokal grün.
- Nächster Schritt / Besitzer: FIT bei Wiederaufnahme von #506: offline weitere Connect-/ItemList-Belege prüfen. Keine Hardwareaktion; keine Änderung der aktiven Filterfelder ohne belegte All-data-Semantik.

## Fachlicher Stand / offene Grenzen
- Approved Capture: ATT/GATT-Neuzuordnung +84 Characteristic-Discovery-Events, 168 passende `0x08`-Error Responses konsumiert; 68 strict-layout response candidates bleiben unmatched. Das beweist GATT-Attribution, nicht MLR-Registrierung, ItemList-Pagination oder FIT-Bytes.
- Connect 5.31 `AgentQueueHelper.requestDeviceFileList` (`yc1.n`) konstruiert den Transaktionsfilter aus Agent-Konfiguration und Transaktions-ID. Capture-Feldformen stimmen als beobachtete optionale Filterfelder damit überein; konkrete Werte, all-data-Semantik und Kausalität zur CIRQA-Listenlücke sind nicht belegt.
- GUI-Antwort: `rawNotifications` ist eine deduplizierte gespeicherte Payload-Inhaltsmenge, keine Laufzählung; ImportAudit ist nur im Backfill, nicht pro Sync-Datei; die aktuelle Sync-UUID wird nicht persistiert; ein komplettes per-file FIT/nicht-FIT/unbekannt-Inventar fehlt.
- `FIT_BYTES_VERIFIED=false` und `coverage=device-history-unverified` bleiben unverändert. Keine privaten Payloads oder Gerätekennungen veröffentlicht.
