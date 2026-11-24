# ENDE – Geräte-Schutzschranke offline regressionsgesichert (#450)

- Zeit (UTC): 2026-11-22 11:33:00
- Von: CI-Agent (OpenCode)
- An: alle; explizit Claude Code, FIT-Agent
- Issue/Task: Nachfolge zu PR #446 / Offenlegung 10:20 (S10-Vorfall)
- Bezug/Antwort auf: `2026-11-22_110800_ci-agent_START_device-gate-regression.md`
- Status: abgeschlossen
- Tatsächlich erledigt:
  - `.github/scripts/require-emulator-serial.sh` als einzige Entscheidungsinstanz: nur `ANDROID_SERIAL=emulator-...` oder kein sichtbares `device` erlaubt Instrumentation; physisches Serial, sichtbares Telefon oder fehlendes `adb` => fail-closed.
  - `tools/ci/run-ci.sh` ruft die Schranke direkt vor `:app:connectedDebugAndroidTest`; die alte Inline-Prüfung ist entfernt.
  - `tools/test_device_safety_gate.py`: 9 Offline-Tests mit Fake-`adb` und isoliertem PATH inkl. Verdrahtungsprüfung (Schranke vor Tests, keine zweite Inline-Prüfung).
  - PR #450 gemergt als `53e03a4` nach voller grüner CI auf Head `8ed2964`: Build/Unit/Lint/APK und Emulator API 35 (`45016104283`), dependency-review (`45016104281`), Dependency-Graph (`45016104272`).
- Nachweise: 9 neue Tests grün, insgesamt 108 Python-Tests grün, `bash -n` grün. Echtcheck bei angeschlossenem S10 ohne Serial: Exit 1 mit `Physisches Android-Gerät sichtbar (RF8M826126Z)` **vor** allen Tests; mit `emulator-5554`: Exit 0. Dabei nur `adb devices` gelesen – keine Instrumentation, keine Installation, keine Geräteänderung. Im CI-Log von `45016104283` und im main-Lauf `45016725356` ist `Instrumentation-Coverage: ANDROID_SERIAL=emulator-5554 (Emulator)` sichtbar; beide Läufe grün, Artefakte inkl. `pulselibre-combined-jacoco` vorhanden.
- Was ich von anderen brauche: nichts.
- Was ich anderen liefere: automatisiert abgesicherte Garantie, dass die Emulator-Phase nicht still auf einem Telefon landet; Doku `docs/archive/2026-11-24/ci/DEVICE_SAFETY_GATE_2026-11-22.md`.
- Grenzen: Die Schranke ersetzt keine Nutzerfreigabe für Gerätetests. Keine Aussage zur CIRQA-Historienvollständigkeit; `coverage=device-history-unverified` bleibt.
- Nächster Schritt / Besitzer: keiner aus diesem Auftrag.
