# UPDATE | P1 S10-Testbuild: finale APK bereit, #144 hinter P1 gehalten

- Zeit (UTC): 2026-11-23 00:20
- Von: Datenmodell-/Persistenz-Agent (OpenCode) · An: GUI-Agent (Claude), FIT-Agent (OpenCode), CI-Agent, ChatGPT
- Bezug: START `2026-11-23_013000_claude_START_s10-testbuild-48h.md`, PR #533, PR #536, PR #539, PR #540

## Build-Stand (nur lesend geprüft)

- `main` = `42b1f242` (enthält #537 Dashboard mit echten Daten und #539 FIT-Adapter).
- Voller CI auf diesem Head grün: `Build, Unit-Tests, Lint, APK` **und** `Emulator-Instrumentationstests (API 35)` ([Run 45085982396](https://github.com/jochenkunkel/pulselibre/actions/runs/45085982396)).
- Artefakt `pulselibre-signed-release-update-apk`: `de.pulselibre`, **versionCode 1931** / `2.0.0_build1931`; `apksigner verify` erfolgreich, Zertifikat `CN=PulsLibre` (SHA-256 `36e7f3b6…39177`, unverändert); APK-SHA-256 `1da43721524231d3af1b43382be475b98119a7ce0d7e9022cfb06a62e8003a6a`.
- **Die vorige APK (versionCode 1923, `9243ec3e`) ist veraltet** – bitte nur 1931 für die S10-Abnahme nutzen. Lokale Kopie: `/tmp/opencode/s10-apk-final/app-release.apk`.

## Datenvertrag / FIT

- Persistenz-Antwort auf den GUI-Entwurf liegt in PR #536; FIT-Agent hat die Zeitsemantik in PR #539 in Code umgesetzt (nur `PROFILE_VALID` + `DIRECT`/`RECONSTRUCTED_TS16`, Kandidaten und `UNKNOWN`/`RECONSTRUCTED_COMPRESSED` ausgeschlossen). Produktiv-DB: 27.884 FIT-Zeilen, alle `PROFILE_VALID`/`DIRECT`, HR 47–147 bpm; keine Sentinel-Werte.
- SpO₂ bleibt `UNVERIFIED`, Statuscodes werden nicht als Werte gezeichnet; fehlende Messgrößen behalten ihre erklärten Leerzustände. `coverage=device-history-unverified` unverändert.

## #144 / PR #533

Voller CI auf `ebdd9c85` grün inkl. Emulator; Merge **bewusst zurückgehalten**, bis der Nutzer die S10-APK abgenommen hat. Danach Merge + voller Lauf auf `main`. Konfliktprobe gegen aktuelles `main`: keine Überschneidungen, keine Konfliktmarker.

Kein Gerätezugriff durch mich, keine BLE-Kommandos, keine Instrumentationstests auf dem S25. Der Nutzer testet die APK selbst; weitere Schritte erst nach seiner Rückmeldung.
