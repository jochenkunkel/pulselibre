# START – Lauf 2 des begrenzten DB-Import-Tests

- Zeit (UTC): 2026-11-22 12:22
- Von: CI-Agent (OpenCode)
- An: Nutzer, FIT-Agent, alle
- Freigabe: Nutzer 12:19 UTC „Freigabe für den neuen Lauf erteilt"; Garmin Connect nur temporär stoppen.
- Auftrag: neuer, separat freigegebener 5-Minuten-Lauf mit derselben App `2.0.0_build1806` und dem vorhandenen UI-Pfad `Gerät` → `Historie herunterladen` → `Herunterladen`. Kein Retry zu Lauf 1.
- Plan: `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_LAUF2_PLAN_2026-11-22.md`.
- Besondere Vorbedingung: Nutzer wies auf einen früher deaktivierten Garmin-Connect-Dienst hin. Ich prüfe die Dokumentation (lesend) und ändere Garmin erst danach. Bisheriger Befund: zwei Notizen (15.11./21.11.) mit „deaktiviert/deinstalliert", aber kein Servicename; Paket ist aktuell in `pm list packages -e`.
- Geplante Dateieingriffe: Lauf-2-Plan, START/ENDE, agent-sync, Nachtrag zum Garmin-Befund. Kein Produktcode, kein CI, kein Gerätebefehl vor Abschluss der Dokumentationsprüfung.
- Stop-Bedingungen: 5 Minuten ab „gestartet", kein Retry, sofort Abbruch bei unerwarteter Antwort/Pairing/Reset/unbekannter Verbindung/destruktiver Aufforderung.
- Abstimmung: `docs/collaboration/agent-sync/2026-11-22_1222_ci-agent-an-alle_lauf2-start.md`.
