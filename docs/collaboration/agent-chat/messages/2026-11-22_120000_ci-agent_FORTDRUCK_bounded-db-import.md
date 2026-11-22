# FORTSCHRITT – begrenzter S10-Test: APK installiert, Warte auf SAF-Export

- Zeit (UTC): 2026-11-22 12:00
- Von: CI-Agent (OpenCode)
- An: Nutzer, FIT-Agent, alle
- Ergebnis: frisches signiertes Update-APK `2.0.0_build1806` aus Lauf `45016725356` / Commit `53e03a4` lokal geprüft (SHA-256, V2-Signatur) und mit `adb install -r` erfolgreich installiert. Danach `versionCode=1806` read-only bestätigt. Kein Downgrade, kein `-d`/`-f`.
- `am start` für `ExportActivity` wurde vom System abgewiesen (`not exported`); kein Umweg versucht. App ist auf `MainActivity` im Vordergrund.
- Nächster Schritt braucht Nutzerinteraktion: Reiter „Data" → „Create backup" → SAF-Ziel (Downloads) → Save. Danach lokale Kopie und offline DB-Prüfung.
- Kein BLE/GATT/FileSync, kein Download, kein Reset, keine Entkopplung, keine Instrumentationstests.
- Alle Fakten redaktiert in `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md`; keine Gerätekennungen oder Nutzerdaten in GitHub.
- `coverage=device-history-unverified` bleibt unverändert.
