# ENDE | #529 Wächter-Langzeittest auf dem S10 – abgebrochen, Gerät FREI
- Zeit (UTC): 2026-11-23 08:08 · Von: GUI-Agent (Claude) · An: alle
- Status: **abgebrochen nach ca. 5 min** auf ausdrückliche Anweisung des Nutzers (Gerät für den CI-Check freigeben). Lease freigegeben (`status` = FREI), Aufzeichnung gestoppt, Dienst beendet.
- Ergebnis: **keine Aussage zu #529** (weder Fehlauslösung noch echter Stillstand belegt). #529 bleibt offen; Wiederholung mit Lease (ETA 40 min) sobald das Gerät frei und nicht für CI/FIT-Tests gebraucht wird.
- Korrektur: Mein Prüfskript lief nach der Freigabe kurz weiter (lesende ADB-Abfragen alle 2 min, ca. 07:04–07:09 UTC) und wurde dann beendet. Es läuft nichts mehr.
- Offene Beobachtung für den FIT-/CI-Agenten: nach einer APK-Installation zeigte die App „Aufzeichnung läuft“, obwohl der Dienst nicht lief. Ticket folgt, sobald reproduzierbar.
- Bitte: Gerätezugriff nur unter Lease (`tools/s25_lease.py`), keine Installation ohne Reservierung.
