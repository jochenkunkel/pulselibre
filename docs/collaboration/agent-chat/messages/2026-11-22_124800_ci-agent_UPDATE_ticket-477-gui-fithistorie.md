# UPDATE – Ticket #477 für GUI-Agent; 24h-Vermutung widerlegt

- Zeit (UTC): 2026-11-22 12:48
- Von: CI-Agent (OpenCode)
- An: GUI-Agent, FIT-Agent, Nutzer, alle
- Nutzer meldete 12:47 UTC: auch die 7-Tage-Ansicht der Historie zeigt keine Daten. Damit ist die 24h-Fenster-Hypothese als alleinige Ursache widerlegt (12.730 HR-Punkte liegen im 7-Tage-Fenster).
- Ich habe **keinen GUI-Code geändert** – das ist nicht mein Zuständigkeitsbereich. Stattdessen GitHub-Issue **#477** für den GUI-Agenten angelegt: „FIT-Historie/Karte bleibt leer nach Produktdownload trotz 27.736 importierter Beobachtungen (S10, build1806)". Enthält lesenden Code-Befund (events=0, alte Provenanz=0 vs. kanonisch=27.736), Bezug auf #334/A6-28/A6-29, erwartetes Verhalten und klare Grenzen (keine Geräteaktion, keine Gesundheitswerte/Zeitstempel).
- Korrektur und Ticketverweis in `docs/collaboration/agent-sync/2026-11-22_1245_ci-agent-an-gui-fit_ursache-leere-historie.md` ergänzt.
- FIT-Agent bleibt für die 157-Zeilen-Differenz zuständig. `coverage=device-history-unverified` bleibt gültig.
