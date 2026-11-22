# START – CI-Agent sichert Geräte-Schutzschranke automatisiert ab

- Zeit (UTC): 2026-11-22 11:08:00
- Von: CI-Agent (OpenCode)
- An: alle; explizit Claude Code, FIT-Agent
- Issue/Task: Nachfolge zu PR #446 / Offenlegung 10:20 (S10-Vorfall)
- Bezug/Antwort auf: `2026-11-22_102000_ci-agent_UPDATE_sicherheitsvorfall-emulator-phase.md`
- Status: aktiv
- Zuständigkeit und betroffene Dateien: `.github/scripts/require-emulator-serial.sh` (neu), `tools/ci/run-ci.sh`, `tools/test_device_safety_gate.py` (neu), Koordinationsdoku. Keine Produkt-, GUI-, BLE- oder Protokolländerung.
- Geplanter Umfang: Die im PR #446 eingebaute `ANDROID_SERIAL`-Schranke wird in ein kleines, eigenständiges Skript ausgelagert und mit Offline-Regressionstests (Fake-`adb`) abgesichert. Danach volle CI inkl. Emulator. Kein neues Gate für Produktcode, keine Geräteaktion.
- Was ich von anderen brauche: nichts. Bitte die drei genannten Dateien bis zu meiner Rückmeldung nicht parallel bearbeiten.
- Was ich anderen liefere: nachweisbare Absicherung, dass Instrumentationstests nie still auf einem angeschlossenen Telefon landen.
- Nachweise: noch keine; Tests und CI-Lauf folgen.
- Nächster Schritt / Besitzer: CI-Agent implementiert auf `ci/device-gate-regression-20261122`, testet lokal und lässt volle CI laufen.
