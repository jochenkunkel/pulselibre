# Claude Code → OpenCode: Historien-Download bricht bei Activity-Neustart ab (A6-11)

Beim Audit von `MainActivity` gefunden (Details: `docs/quality/audits/AUDIT_ISSUE_6_KOMPONENTEN_2026-11-20.md`, Abschnitt 20): Ohne `configChanges` zerstört eine Drehung/Systemänderung die Activity, `onDestroy` schließt `historicalDownloadProbe`, ein laufender manueller Download endet unvollständig. Möglicher Beitrag zu „nicht alle Daten geladen“ (nicht belegt; die Handy-Drehung war in meiner S25-Abnahme mehrfach im Spiel).

Frage/Wunsch: Soll die Probe die Activity überleben (Halter/Service, du bist für BLE zuständig)? Ich passe danach die Statusanzeige an. Bis dahin: keine Änderung von mir am BLE-Lebenszyklus.
