# OpenCode – gzip-Feldpfade / FileAccess-Abgrenzung

- Agent/Umgebung: OpenCode, eigener Worktree
- Beginn / Aktualisiert: 2026-11-22 06:57 UTC
- Status: aktiv
- Auftrag: Offline-Protobuf-Feldpfade der gzip-JSON-Daten und Zuordnung zu Pulls untersuchen.
- Tickets: #404 / #149; keine weiteren Reservierungen.
- Branch: `opencode/s10-historical-download-20261122`
- Dateien: eigener Offline-Analyser und Tests nach Befund; `CIRQA_S10_MLR_TRANSFER_ANALYSIS_2026-11-22.md`.
- Koordination: PR #413 / Agent-sync-Ergebnis gelesen. Keine Änderung seiner Dateien oder am Gerät.
- Nachweisplan: CRC-validierte GFDI-Frames → vollständige Protobuf-Nachrichten → Feldpfade mit gzip-Bytes → JSON-Format/Metadaten; keine Werte veröffentlichen.
- Abhängigkeiten: keine; CI-Agent/FIT-Agent um ggf. bereits vorhandene Feldpfade gebeten.
- Fortschritt: Alle 21 Wetter-gzip-Streams sind Smart.2→6→3 App→Band, keine FileAccess-Pulls. Danach separate Nicht-GFDI-MLR-Daten geprüft: sechs zlib-Streams dekomprimiert, fünf FIT mit gültiger interner Größe/Header-/Datei-CRC; vier FIT plus ein Nicht-FIT exakt mit UID/Listengröße abgeglichen; letzter FIT ohne beobachtete Listen-UID.
- Produktbaustein: begrenzter `CirqaZlibFileDecoder`, fünf synthetische Tests grün unter JDK 21; keine Live-Transportverdrahtung.
- Tests: drei Service-Isolationstests und bestehender S10-Transaktions-Replay ebenfalls grün. FULL-Lauf auf vorherigem Head gestartet; durch neue Dateien kein finaler Head-Nachweis, wird nach Abschluss neu gestartet.
