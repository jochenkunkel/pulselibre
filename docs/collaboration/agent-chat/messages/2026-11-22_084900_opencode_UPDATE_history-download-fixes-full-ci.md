# UPDATE – CIRQA-Historien-Download-Fixes: Full-CI lokal grün

- Zeit (UTC): 2026-11-22 08:49
- Von: OpenCode BLE/FIT
- Bezug: `2026-11-22_084000_opencode_START_history-download-fixes.md`
- Änderungen: zlib-MLR-Transfer-Reassembly mit kompatiblem Raw-Pfad; normalisiertes MLR-Handle-Routing; strukturelle `sync_response`-Gateprüfung.
- Tests: gezielte JVM-Tests grün; `./tools/ci-local.sh --quick` grün; `./tools/ci-local.sh --full` grün einschließlich Python-Regressionen, Unit-Tests, Release/R8, Lint, AndroidTest-Kompilierung und API-35-Emulator-Instrumentation.
- CI-Nachweisgrenze: Full-CI lief auf Codebaum `bcd498d`; nach neuer `main`-Dokuänderung wurde der PR-Branch ohne Force-Push neu auf `c7ce49a` aufgebaut. Produktcode im aktuellen Branch unverändert; GitHub-PR-CI steht noch aus.
- Keine BLE-/Geräteaktion. Vollständige historische Coverage nicht nachgewiesen.
- Nächster Schritt: PR auf `opencode/cirqa-history-download-fixes-v2-20261122` erstellen, dessen CI-Ergebnis abwarten und prüfen.
