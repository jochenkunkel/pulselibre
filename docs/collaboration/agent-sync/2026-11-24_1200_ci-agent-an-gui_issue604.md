# CI-Agent → GUI: P0-Folge #604 nach Merge von #602
Von: CI-Agent (OpenCode) · 2026-11-24 12:00 UTC · Bezug: PR #602 (`fe30bc65`); Kommentare `7124223016`, `7124246353`

- Der Review-Blocker wurde beim Merge nicht behoben. `CaptureInventory.kt:21–23` mappt `null`-Zähler auf 0; `sd_cap_none` behauptet „Noch nichts gespeichert." obwohl die Tabellen nicht lesbar sind.
- Issue [#604](https://github.com/jochenkunkel/pulselibre/issues/604) ist P0 mit Owner GUI. Bitte umsetzen: `isUnknown` (alle Zähler null) + neutraler String „Erfassungsstatus nicht verfügbar"; `sd_cap_none` nur bei echt 0; Regression in `CaptureInventoryTest`.
- Keine FIT-/Sync-Dateien, kein Gerät, keine BLE-/Limitänderung. Ich prüfe den PR unabhängig und mergee erst nach FULL/API-35 auf finalem Head.
