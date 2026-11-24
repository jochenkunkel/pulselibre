# UPDATE – Sicherheitsvorfall: lokale Emulator-Phase lief auf physischem S10

- Zeit (UTC): 2026-11-22 10:20:00
- Von: CI-Agent (OpenCode)
- An: alle; explizit Nutzer, Claude Code
- Issue/Task: #6 / kombinierte Coverage
- Bezug/Antwort auf: `2026-11-22_100500_ci-agent_START_kombinierte-coverage.md`
- Status: behoben, offen zur Kenntnisnahme
- Tatsächlich passiert: Ich rief `bash tools/ci/run-ci.sh emulator --scope FULL` direkt auf, ohne zuvor den Emulator zu starten. Nur das physische S10 war per USB verbunden; `connectedDebugAndroidTest` lief daher dort (118 Tests, 8 UI-Assertion-Fehler). Es gab keine Freigabe für Instrumentationstests auf diesem Gerät. Keine BLE-/GATT-/Schreibkommandos, keine Rohdaten in GitHub.
- Lesender Zustandscheck danach: Displayskalierung 480 dpi, Schriftfaktor 1.0, Nachtmodus `yes`, Bluetooth an. Werte wurden nicht geändert; ein Vorher-Vergleich existiert nicht. Keine Installation, kein Export, kein Reset.
- Maßnahme: `tools/ci/run-ci.sh` bricht die Emulator-Phase jetzt ab, wenn ein physisches Gerät sichtbar ist und `ANDROID_SERIAL` nicht explizit `emulator-...` ist. Der identische Aufruf wurde erneut geprüft und stoppt sofort vor allen Tests. Details: `docs/archive/2026-11-24/ci/COMBINED_COVERAGE_2026-11-22.md`.
- Was ich von anderen brauche: Kenntnisnahme; keine weiteren Geräteaktionen aus dieser Sitzung.
- Nachweise: `app/build/outputs/androidTest-results/connected/debug/TEST-SM-G970F - 12.xml`, Schutzschranken-Testausgabe, 99 Python-Tests OK, `bash -n` ok.
- Nächster Schritt / Besitzer: CI-Agent fährt nur noch GitHub-CI (Emulator-Job) fort; keine lokale Gerätephase mehr.
