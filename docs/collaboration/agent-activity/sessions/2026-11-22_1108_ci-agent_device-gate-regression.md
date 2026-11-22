# CI-Agent – Regressionstest für die Geräte-Schutzschranke

- Agent/Umgebung: CI-Agent (OpenCode, Workflows/Runner)
- Beginn: 2026-11-22 11:08 UTC
- Letzte Aktualisierung: 2026-11-22 11:08 UTC
- Status: aktiv
- Rolle/Zuständigkeit: CI/Gradle/Artefakte; keine Produkt-, GUI- oder Protokolländerung
- Aktueller Auftrag: eigenständige Nachhärtung nach dem S10-Vorfall von 10:20 – Schutzschranke gegen versehentliche Instrumentationstests auf physischen Geräten automatisiert absichern
- Jetzt in Bearbeitung: #6-Nachfolge – auslagerbare GA `require-emulator-serial.sh`, Aufruf in `run-ci.sh`, Offline-Regressionstest mit Fake-`adb`
- Zeitnah geplant: lokale Tests, volle CI inkl. Emulator, Dokumentation und Übergabe
- Betroffene Bereiche/Dateien: `.github/scripts/require-emulator-serial.sh` (neu), `tools/ci/run-ci.sh`, `tools/test_device_safety_gate.py` (neu), Koordinationsdoku
- Branch/Worktree: `ci/device-gate-regression-20261122`, `/tmp/opencode/pulselibre-ci-device-gate-20261122` (Basis `eba9cd7`)
- Abstimmungsbedarf: keine Überschneidung erwartet; GUI (#449/#172) und FIT-/Inventurarbeit laufen in anderen Dateien/Bereichen. Keine Geräteaktion, kein BLE.
- Agent-sync-Nachrichten: `2026-11-22_1108_ci-agent-an-alle_device-gate-regression.md`
- Nachweise: noch keine
- Stop-Grenzen: keine lokale Instrumentation, keine Geräteaktion ohne Freigabe, keine Änderung bestehender Qualitätsgates
- Nächster Schritt: Implementierung und Offline-Tests, danach volle GitHub-CI
