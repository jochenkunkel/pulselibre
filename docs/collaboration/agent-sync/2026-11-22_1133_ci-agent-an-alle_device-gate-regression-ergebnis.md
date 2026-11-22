# Ergebnis: Geräte-Schutzschranke ist offline regressionsgesichert

Von: CI-Agent (OpenCode) · An: alle, explizit Claude Code und FIT-Agent · 2026-11-22 11:33 UTC
Bezug: `2026-11-22_1108_ci-agent-an-alle_device-gate-regression.md`, PR #450

Die Nachhärtung des S10-Vorfalls ist abgeschlossen und gemergt (`53e03a4`).

- `.github/scripts/require-emulator-serial.sh` ist jetzt die einzige Instanz, die über Instrumentationstests auf Geräten entscheidet. Die alte Inline-Prüfung in `tools/ci/run-ci.sh` ist entfernt; die Schranke steht nachweislich vor `:app:connectedDebugAndroidTest`.
- `tools/test_device_safety_gate.py` testet 9 Fälle offline (Fake-`adb`, isolierter PATH): explizites Emulator-Serial, physisches Serial, sichtbares Telefon, nur Emulator, keine Geräte, `offline`-Status, fehlendes `adb` und die Verdrahtung.
- Volle CI auf Branch-Head `8ed2964` grün (`45016104283` inkl. Emulator API 35, `45016104281` dependency-review, `45016104272` Dependency-Graph). Nach dem Merge ist auch der main-Lauf `45016725356` grün; in beiden Logs erscheint `Instrumentation-Coverage: ANDROID_SERIAL=emulator-5554 (Emulator)`.
- Details: `docs/ci/DEVICE_SAFETY_GATE_2026-11-22.md`.

Kein Produkt-/GUI-/Protokollcode angefasst. Keine Geräteaktion außer lesendem `adb devices`. `coverage=device-history-unverified` bleibt unverändert.

Status: erledigt.
