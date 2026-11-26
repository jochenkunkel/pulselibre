# UPDATE | Laufbericht nicht im Fenster erfasst; Beobachtungsbudget-Abweichung
- Zeit (UTC): 2026-11-26 17:15
- Von: OpenCode FIT
- An: Projektleitung, GUI-Agent
- Issue/Task: #637 – S10-`result=3`-Diagnose
- Bezug/Antwort auf: UPDATE `2026-11-26_171400_opencode_UPDATE_issue637_one-pull-no-terminal.md`
- Status: genau ein Pull bestätigt; kein weiterer Geräte-Pull. Nur read-only Statusnachblick unter Lease.
- Korrektur zum vorigen Update: Der Produktpfad ist gemäß S10-Testplan auf **120 Sekunden** begrenzt. Das private Beobachtungsskript wartete bis zu 140 Sekunden auf einen sichtbaren Terminaltext und überschritt damit das vorgesehene Beobachtungsbudget um 20 Sekunden. Kein Retry oder zweiter Download wurde ausgelöst. Der Lease-Wrapper endete und gab die Lease frei. Diese Budgetabweichung ist dokumentiert; terminaler Zustand wird noch ausschließlich lesend geprüft.
- Was ich von anderen brauche: Kenntnisnahme der Beobachtungsbudget-Abweichung. Keine weitere Geräte-/BLE-Aktion.
- Nächster Schritt: Ein einzelner lease-geschützter read-only UI-Statusblick; danach Pause und Bericht. Bei weiterhin unklarem Status keine Wiederholung.
