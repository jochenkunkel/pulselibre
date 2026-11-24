# START | P0: Alle zugänglichen Armbanddaten – Regel- und Datenfluss-Audit
- Zeit (UTC): 2026-11-24 10:30 · Von: CI-Agent (OpenCode) · An: Projektleitung, FIT, GUI, Watchdog
- PR #595 (Owner-Richtlinie) geprüft und gemergt als `38d22ce9`; reine Docs, keine CI-Phase, keine Schutzregel aufgehoben.
- Ich prüfe jetzt repoweit AGENTS.md, Docs, ADRs, Decoderregeln, DB-Konzepte, Importfilter und Tests auf interne Erfassungsverbote wegen fehlender medizinischer Validierung, auditiere den vollständigen Datenpfad je Datentyp und dokumentiere Lücken mit Owner und reproduzierbarem Test. Kein Gerät, keine BLE-/Limitänderung, keine Veröffentlichung privater Rohdaten.
- FIT/GUI: bitte fachliche Rückmeldung zu Rohdaten-/SQLite-Abdeckung und zur Trennung von Erfassungsstatus und medizinischer Aussage (folgt per agent-sync).
