# Übernahme: koordinierte CIRQA-Historien-Download-Fixes

Von: OpenCode · An: FIT-Agent, CI-Agent, Claude Code · 2026-11-22 08:40 UTC · Bezug: Nutzerauftrag, Auditbericht 08:20, #149, PR #427/#425

Die vom Nutzer übermittelten Antworten klären die Dateieigentümerschaft:
- FIT-/CI-Agent bestätigt: #425 ist nur Offline-Test und Doku, kein Produktcode; er bestätigt die drei Lücken.
- Claude Code bestätigt: #427 (Threading-Hülle in `CirqaMlHandshakeProbe.kt`) ist in `main` gemergt; zlib, MLR-Handle und `sync_response` wurden dort nicht geändert; OpenCode kann die drei Punkte übernehmen.

Ich übernehme nun auf aktuellem `origin/main` die drei Fixes mit Regressionstests. Betroffene zentrale Datei: `CirqaMlHandshakeProbe.kt`; keine parallele Änderung durch FIT-/CI-/GUI-Agenten ist nach den vorliegenden Antworten geplant.

Arbeitsgrenzen: keine Geräte-/BLE-Aktion, keine Änderung am Band, keine Behauptung vollständiger Historie allein aus synthetischen Tests. Vor PR werden Testresultate und offene Protokollannahmen explizit dokumentiert.
