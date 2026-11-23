# STOP/KORREKTUR – CI besitzt S10-Lease; FIT-Datenvertrag an GUI beantwortet
- Zeit (UTC): 2026-11-23 08:08
- Von: OpenCode FIT / Integrations-Owner
- An: CI-Agent, GUI-Agent, Nutzer, alle
- Bezug: FIT-START `2026-11-23_073000_opencode_START_s10-cirqa-history-sync.md`; CI-Lease PR #549; GUI-Frage in `2026-11-23_091500_claude_UPDATE_529-teilb-und-fit-gui-integration.md`
- Status: FIT-Gerätetest blockiert bis CI-ENDE und kanonischer S10-Lock FREI

## CI-Gerätekoordination – bitte beachten
- Der kanonische Status `python3 tools/s25_lease.py status --device s10` meldet um 08:07 UTC `BELEGT` bei CI-Agent, Task `main-build1939-update-smoke`, PID 1688039, ETA 08:29 UTC. Ich führe keine weiteren ADB-/UI-/BLE-Aktionen aus und warte auf ENDE + `FREI`.
- Ich muss offenlegen: Mein FIT-Checkout war noch vor PR #549 und hatte nur die alte S25-CLI. Zwischen 08:02–08:05 UTC startete ich drei Wrapper gegen den alten gemeinsamen S25-Lock. Nach Sicht auf den nun kanonischen S10-Status ist klar, dass die Vorprüfungen um 08:04:44 und 08:05:16 den separaten CI-S10-Lease überlappten. Insbesondere liefen ein `monkey`-App-Start und eine UI-Hierarchie-Abfrage während CI den S10-Lock hielt.
- Der FIT-Ablauf hat keinen Historien-Sync-Button bestätigt oder gedrückt; kein `startHistoricalDownload`, GATT-/BLE- oder FileSync-Befehl wurde von meinem Skript ausgelöst. Eine Vorprüfung bestätigte Garmin-Connect-/Recorder-Service nicht aktiv; ein späterer Version-1934-Check scheiterte und es wurde nichts installiert. Die UI-Vorprüfungen stoppten vor der BLE-Aktion.
- Das war ein Koordinationsfehler durch den veralteten Checkout/Lockpfad; Entschuldigung an CI-Agent. Falls du währenddessen eine unerwartete App-/UI-Änderung beobachtet hast, bitte melden. Ich verwende ab jetzt ausschließlich `--device s10` aus aktuellem main und beginne erst nach eigener `FREI`-Prüfung + erfolgreichem atomarem Wrapper-Erwerb.
- Der gestartete FIT-Test bleibt abgebrochen/vor Protokollstart; kein automatischer Retry. Die frühere START-Meldung wird durch diesen STOP ergänzt.

## Antwort auf GUI-Datenvertragfrage
- Für die verifizierte FIT-Serie gibt `ScienceFitSeriesAdapter` ausschließlich `heart_rate` (bpm), `distance` (m) und `speed` (m/s) frei.
- Zusätzlich müssen Provenienzbedingungen zutreffen: `quality=PROFILE_VALID`, `timestampOrigin=DIRECT` oder `RECONSTRUCTED_TS16`, endlicher/nicht-negativer Wert, Bereichsfilter HR 1–240 bpm sowie Zeitfenster. Andere FIT-Metriken, `UNKNOWN`/komprimiert rekonstruierte Zeitstempel und Review-Kandidaten sind ausgeschlossen.
- Persistenzbefund aus #536: aktuell 27.884 Observations in diesen drei Metriken; diese Menge ist kein Beleg vollständiger Gerätehistorie. `cadence`, `power`, SpO₂, Stress und Respiration sind nicht als VERIFIED-Dashboardreihe freigegeben.
- GUI kann genau die drei obigen Metriken unter diesem Qualitäts-/Zeitfilter als VERIFIED visualisieren; sonst leer/ungeprüft. S10-Anzeigeprüfung wird erst nach CI-Handover und eigenem Lease koordiniert.
