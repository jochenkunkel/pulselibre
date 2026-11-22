# DB-Import nachgewiesen – Vollständigkeit der Historie weiter offen

Von: CI-Agent (OpenCode) · An: Nutzer, FIT-Agent, Claude Code, alle · 2026-11-22 12:36 UTC

## Redaktierter Befund

Drei Exporte desselben App-Profils wurden offline nur anhand von Anzahlen, Bytezahlen und Integritätsstatus verglichen:

| Export (Hostzeit) | Größe | `canonical_observations` | `canonical_fit_sources` | Integrität |
|---|---|---|---|---|
| Baseline 12:03 | 2.786 Byte | 0 | 0 | ok |
| nach Lauf 1 12:18 | 2.786 Byte | 0 | 0 | ok |
| nach Lauf 2 12:34 | 4.043.919 Byte | 27.736 | 23 | ok |

Weitere Anzahlen im dritten Export (vorher jeweils 0): Provenanz 27.736, Generierungsobservationen 27.736, unmapped fields 10.283, FIT-Gerätemetadaten 115,Decoder-Generationen 23, aktive Generationen 23, Sync-Cursor 1, Roh-Gerätemetadaten 1. Unverändert 0: Legacy-Readings, alte FIT-Tabellen, CIRQA-Events, Sync-Cursors. Alle 15 Manifest-Prüfsummen und Bytezahlen stimmen in allen drei Archiven.

## Einordnung

**DB-Import nachgewiesen.** Er erfolgte während Lauf 2 und überstand den Force-Stop um 12:32:55 UTC. Lauf 1 blieb ohne Import. **Vollständigkeit der Historie nicht nachgewiesen** – die Zahlen belegen Import und Verarbeitung, aber nicht, dass alle Banddaten übertragen wurden.

`coverage=device-history-unverified` bleibt daher gültig. Bitte nicht als vollständige Historie kommunizieren; der Teilerfolg ist „DB-Import nachgewiesen".

## Übergabe / Grenzen

- FIT-Agent: offline Auswertung der redaktierten Anzahlen und Abgleich mit den acht bekannten FIT-Dateien bzw. den sechs finalen Band→App-Transfers möglich; keine neuen Geräteaktionen.
- Keine Inhalte, Gerätekennungen, Roh-BLE-Payloads, Tokens oder Gesundheitswerte in GitHub. Archive lokal im 0700-Ordner, Modus 0600.
- Garmin nur temporär gestoppt (kein dauerhafter Eingriff). Keine Instrumentationstests auf dem S10, kein Reset, keine Entkopplung, kein `CLOSE_ALL`.
