# Bitte: #601 Persistenz-/Backup-Abnahme auf aktuellem main verifizieren
Von: OpenCode FIT · An: GUI-Agent · Datum/Uhrzeit: 2026-11-26 18:40 UTC
Bezug: Nutzerauftrag, verbleibende offene P0-Tickets; #601; gemergte PRs #607/#613/#616

Gemäß Nutzerwunsch bitte ich GUI, die Testausführung/Installationsschritte zu übernehmen. Die drei Implementierungs-PRs sind bereits gemergt:
- #607 ImportAuditRecorder-Baustein (`a0d15e33`);
- #613 Backfill-Fehler-/Statusereignisse (`0f7259b5`);
- #616 Backup v11, Restore v1–v10 (`e6462373`).

Bitte auf aktuellem `main` die vorhandenen gezielten Tests (ImportAuditRecorder-Idempotenz/Redaktion/FK, `observation_status_events`, Backup-v11-Audit-Tabellen-Roundtrip und Legacy-Restore) ausführen oder die passenden aktuellen GitHub-Actions-Läufe nachweisen. Keine Hardware-/Smartphone-Installation nötig. Bitte die exakten grünen/roten/pending Gates und betroffenen Testnamen nennen; keine grüne CI behaupten, wenn kein Lauf vorhanden ist.

Rückmeldung bitte redigiert in Git/Issue #601. Keine privaten Gerätedaten. Wenn Tests fehlschlagen oder Pflichtchecks fehlen, nicht mergen (es gibt hier keinen neuen PR); Ursache und kleinsten nächsten Schritt melden. OpenCode prüft währenddessen die Abnahmebedingungen und restlichen P0-Abhängigkeiten offline.
