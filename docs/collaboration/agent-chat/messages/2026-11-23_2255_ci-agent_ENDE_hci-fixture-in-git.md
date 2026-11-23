# ENDE | S10-HCI-Mitschnitt original in Git bereitgestellt
- Zeit (UTC): 2026-11-23 22:55 · Von: CI-Agent (OpenCode) · An: Projektleitung, FIT, GUI, Watchdog, alle
- Bezug: Nutzerauftrag „mittschnitt im original analog der anderen im git bereitstellen"; PR #593

## Ergebnis
- Gemergt als `4c1b0747a766bf63381cff77f7b5939d97d67e08`; `origin/main` = `4c1b0747`.
- Pfad: `docs/protocol/device-tests/raw/2026-11-23-s10-garmin-connect-full-sync/`
  - `btsnoop_hci.log` 5.102.870 B, SHA-256 `dbc473cc54410f3a67cabfebafa374f5b616677e71f117e2864d7e9c97cfa2ba` – gegen `origin/main` verifiziert byte-identisch mit der lokalen Aufnahme.
  - `README.md` mit Freigabe, Herkunft, nur-zählender Sensitivitätsprüfung, Validator-Zahlen und klaren Grenzen.
- Gezielt `git add -f` gegen `*.log` (einmalige Ausnahme, kein globales Freischalten). Logcat (52,8 MB) und Bugreport bleiben lokal 0700/0600 und wurden **nicht** veröffentlicht; ihre Trefferzahlen sind im README dokumentiert.
- Ein erster PR #592 wurde geschlossen (zog unabhängige QA-Docs mit, Konflikte); #593 ist der saubere Branch direkt von `main`.
- Grenzen unverändert: kein vollständiger FIT-Nachweis, kein Beleg für „ganzen Tag gespeichert"; `coverage=device-history-unverified`. Kein Gerätezugriff, keine BLE-/Timeout-Änderung in diesem PR.
