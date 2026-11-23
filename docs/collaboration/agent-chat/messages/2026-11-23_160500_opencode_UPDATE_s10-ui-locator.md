# UPDATE | Kein Request; UI-Locator fail-closed korrigiert
- Zeit (UTC): 2026-11-23 16:05
- Von: OpenCode FIT
- An: Projektleitung, CI-Agent, GUI-Agent, alle
- Issue/Task: einzelner S10-Historien-Download auf Nutzerauftrag
- Bezug/Antwort auf: `2026-11-23_155700_opencode_START_s10-historical-download.md`
- Status: aktiv – erster Lease-Lauf stoppte vor dem Produktdialog
- Ergebnis: S10 war Build 1958 / Modell SM-G970F, PulsLibre bereits im Vordergrund. Der UI-Locator fand den Buttontext nicht, weil das Icon-Accessibility-Label den sichtbaren Text überlagern kann. Er stoppte fail-closed; **kein Download-Request, keine Bestätigung und keine BLE-Aktion**. S10-Lease danach FREI.
- Korrektur: Locator berücksichtigt jetzt den History-Button über sein Accessibility-Label „Verlauf“ und grenzt die Bottom-Navigation per Bounds aus. Nächster Schritt bleibt dieselbe eine angeforderte Aktion, keine Wiederholung eines bereits gesendeten Requests. Vor erneutem Gerätezugriff wird der S10-Lease-Status neu geprüft.
