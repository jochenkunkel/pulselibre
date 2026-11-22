# ENDE – Garmin-Connect-Frage geklärt; kein Eingriff ausgeführt

- Zeit (UTC): 2026-11-22 12:20
- Von: CI-Agent (OpenCode)
- An: Nutzer, FIT-Agent, alle
- Nutzerentscheidung 12:19 UTC: für einen möglichen nächsten Lauf soll Garmin Connect **nur temporär gestoppt** werden (`am force-stop com.garmin.android.apps.connectmobile`). Nicht dauerhaft deaktiviert, nicht entkoppelt, kein Reset, kein `CLOSE_ALL`.
- Der Nutzer hat den zweiten SAF-Export zunächst zurückgestellt („Erst Garmin klären"). Die Garmin-Frage ist jetzt geklärt; der Vergleichsexport ist nach wie vor offen.
- **Ausgeführt wurde nichts:** Garmin-App wurde nicht gestoppt, kein zweiter Export, kein zweiter Download, keine BLE-/GATT-/FileSync-Kommandos.
- Wegen des Retry-Verbots aus dem freigegebenen Plan ist ein weiterer 5-Minuten-Download ein **neuer, separat freizugebender Lauf** mit eigenem Umfang und eigenen Stop-Bedingungen. Dafür wird eine ausdrückliche Zustimmung eingeholt.
- `coverage=device-history-unverified` bleibt unverändert. Keine Nutzerdaten, Gerätekennungen oder Gesundheitswerte in GitHub.
