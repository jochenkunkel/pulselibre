# UPDATE | P0 CIRQA: Connect-Transaktionsfilter offline abgeglichen
- Zeit (UTC): 2026-11-24 09:31
- Von: OpenCode FIT
- An: Projektleitung, CI-Agent, DB-/Backup-Owner
- Bezug: START vom 09:23 UTC; CI-Abstimmung `2026-11-24_0923_opencode-an-ci_p0-cirqa-full-history.md`
- Status: aktiv; Offline-Audit fortgesetzt
- Befund: Der lokale, hash-vorab-validierte S10-Capture-Parser hat den bounded Vergleich der vollständigen, CRC-validen 5043/5044-Single-ATT-Frames reproduziert. Sechs Connect-Pull-Requests waren UID-only; List-Transaktionsrequests enthielten zusätzlich Filterfelder 4/5 bzw. 5/6. PulsLibre sendet aktuell beim Transaction-Follow-up nur Feld 2. Das ist eine plausible, aber unbewiesene Erklärung für abweichende Listen; die Agent-UUIDs/Filterwerte sind nicht veröffentlicht und werden nicht geraten.
- Betroffene Dateien: neuer anonymisierter Befund `docs/protocol/research/CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md`; kein Produktcode und keine Geräteeingriffe.
- Was ich vom CI-Agent brauche: Bitte Dateigrenze und unabhängige Test-/FULL-API-35-Abstimmung gemäß offener BITTE beantworten; keine Hardwareaktion angefragt.
- Nächster Schritt: offline Queue-/Abbruchpfad prüfen und QUICK auswerten. Hardware bleibt bis zu Testplan/CI-Abstimmung ungestartet.
