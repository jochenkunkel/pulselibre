# Schutzschranke: Instrumentationstests nur auf dem Emulator

Stand: 2026-11-22 11:12 UTC · Verantwortlich: CI-Agent (OpenCode)

## Warum

Am 10:20 UTC lief die lokale Phase `tools/ci/run-ci.sh emulator --scope FULL`
ohne gestarteten Emulator auf dem per USB angeschlossenen S10 (118
Instrumentationstests, 8 UI-Fehlschläge). Instrumentationstests können
Profil- und Einstellungswerte überschreiben; das Gerät war dafür nicht
freigegeben. Details: `docs/ci/COMBINED_COVERAGE_2026-11-22.md` und Chat 10:20.

## Umsetzung

- `.github/scripts/require-emulator-serial.sh` ist die einzige
  Entscheidungsinstanz. Sie lässt Instrumentationstests nur zu, wenn
  `ANDROID_SERIAL` explizit `emulator-...` ist oder kein Gerät mit Status
  `device` sichtbar ist. Ein physisches Serial, ein sichtbares Telefon oder ein
  fehlendes `adb` führen zum Abbruch (fail-closed).
- `tools/ci/run-ci.sh` ruft das Skript unmittelbar vor
  `:app:connectedDebugAndroidTest` auf. Der direkte Aufruf ohne Emulator
  bricht mit `Physisches Android-Gerät sichtbar (...)` ab, **bevor** Gradle
  irgendwelche Tests startet.
- `tools/test_device_safety_gate.py` prüft alle Zweige offline mit einem
  Fake-`adb` und isoliertem PATH: explizites Emulator-Serial, explizites
  physisches Serial, sichtbares Telefon, nur Emulator sichtbar, keine Geräte,
  `offline`-Status, fehlendes `adb` sowie die Verdrahtung in `run-ci.sh`
  (Schranke vor den Tests, keine zweite Inline-Prüfung).

## Nachweise

- 9 neue Tests grün; alle 108 Python-Tests unter `tools/` grün.
- `bash -n` für beide Skripte grün.
- Echtcheck ohne `ANDROID_SERIAL` bei angeschlossenem S10: Exit 1 mit
  `Physisches Android-Gerät sichtbar (RF8M826126Z)`; mit
  `ANDROID_SERIAL=emulator-5554`: Exit 0. Dabei wurde nur `adb devices`
  gelesen – keine Instrumentation, keine Installation, keine Geräteänderung.

## Grenzen

Die Schranke ersetzt keine Nutzerfreigabe für Gerätetests. Sie verhindert nur,
dass ein falsch konfigurierter Aufruf still auf einem Telefon landet. Die
vollständige CIRQA-Historie und der SQLite-Import bleiben unabhängig davon
nicht nachgewiesen (`coverage=device-history-unverified`).
