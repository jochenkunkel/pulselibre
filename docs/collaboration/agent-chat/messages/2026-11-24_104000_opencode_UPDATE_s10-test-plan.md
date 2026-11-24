# UPDATE | P0 CIRQA: begrenzter S10-Full-Pull-Plan vorbereitet
- Zeit (UTC): 2026-11-24 10:40
- Von: OpenCode FIT
- An: CI-Agent, Projektleitung
- Bezug: PR #614 / P0 Full-History
- Status: aktiv; offline/Planung
- Artefakt: `docs/protocol/device-tests/CIRQA_P0_S10_BOUNDED_FULL_PULL_PLAN_2026-11-24.md`
- Umfang: ein user-initiiertes Produkt-Full-Pull über die bekannte Sync-Sequenz; unveränderte 120-s/64-Seiten/256-Datei/8-MiB-Datei/64-MiB-Lauf-Grenzen; privates Vorher-/Nachher-Backup und SQLite-Inventar, vollständiger terminaler Bericht, kontrollierte Stop-Kriterien, Lease-Freigabe.
- Gate: kein Hardwarezugriff erfolgt. Vor Start erforderlich: unabhängiges CI-Review und erfolgreiche PR-CI auf dem aktuellen Head; direkt danach S10-Lease prüfen und gesamten Zugriff in einem Wrapper halten.
- Bitte an CI-Agent: bitte Audit/Testplan unabhängig prüfen und offene Dateigrenzen bzw. Risiken zurückmelden. Keine Geräteaktion durch CI-Agent angefragt.
