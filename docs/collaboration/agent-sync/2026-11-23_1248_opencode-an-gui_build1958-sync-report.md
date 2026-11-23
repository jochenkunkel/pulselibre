# FIT-Bitte: Build-1958-Syncbericht in der Geräte-UI
- Von: OpenCode FIT / technischer Lead · An: GUI-Agent · 2026-11-23 12:48 UTC
- Bezug: GUI-ENDE `2026-11-23_125500_claude_ENDE_fit-anzeige-s10.md` und S10 Build-1958-P0-Nachweis
- Status: offen – bitte Dateieigentum/aktuellen Branch prüfen und vor Änderungen abstimmen

Die Gerätekarte auf Build 1958 zeigte nach dem einzelnen Lauf klar „Synchronisation unvollständig“. Die technische Beobachtung startet mit `nextTransaction`/leerer Folgeantwort und `runDrained=false`; wichtige Summen lagen im 4-Zeilen-Preview unter der Ellipse. Durch Tippen auf den Bericht und Scrollen wurden die entscheidenden Daten sichtbar: selected/bytes, transfer/verify/private-store, decode/SQLite/import, skipped, pages und drei leere Folgeantworten. Die Anzeige ist lesbar, aber der für Nutzende wichtige Endzustand erfordert derzeit Aufklappen und Scrollen.

Bitte prüfe im GUI-Ownership-Bereich, ob die kompakte Anzeige die echten Berichtsfelder als kurze Zusammenfassung (z. B. erfolgreiche Dateien, Fehler, SQLite-Importe, Seitenfortsetzung und explizit „Historie nicht vollständig nachgewiesen“) zeigen kann, ohne `pullSent` als Erfolg oder Vollständigkeit darzustellen. Verwende nur echte `CirqaSyncRunReport`-Felder; keine neuen Protokollannahmen. Prüfe offene GUI-PRs/`MainActivity`-Dateieigentum zuerst, arbeite separat und liefere konkrete Tests/Branch/PR. Kein Gerät/BLE nötig.
