# Garmin temporär gestoppt – Lauf 2 wartet auf „gestartet"

Von: CI-Agent (OpenCode) · An: Nutzer, FIT-Agent, Claude Code, alle · 2026-11-22 12:28 UTC

## Ausgeführt

- 12:26:31 UTC: `am force-stop com.garmin.android.apps.connectmobile` (exakt die freigegebene temporäre Maßnahme). Garmin-Prozess danach nicht mehr vorhanden.
- Paketstatus unverändert: in `pm list packages -e`, nicht in `-d`, `enabled=0`; nur `stopped=true` durch den Force-Stop. Beim nächsten Start der Garmin-App ist sie wieder aktiv. Kein `pm disable-user`, kein Entkoppeln, kein Reset, kein `CLOSE_ALL`, keine dauerhafte Deaktivierung.

## Gerät (lesend)

- S10 erreichbar, PulsLibre `versionCode=1806`. App wechselte von `ExportActivity` zurück auf `MainActivity`. `uiautomator dump` meldet `could not get idle state` – wie schon in Lauf 1 kein lesbarer Statustext.

## Nächster Schritt / Bitte

- Nutzer tippt `Gerät` → `Historie herunterladen` → `Herunterladen` und meldet **„gestartet"**. Erst dann beginnt die 5-Minuten-Frist; kein Retry, kein Sampling-Loop.
- FIT-Agent/Claude Code: weiterhin **keine parallelen Geräteaktionen**, bis Lauf 2 und der SAF-Vergleich abgeschlossen sind.

## Grenzen

Keine neuen BLE-/GATT-/FileSync-Kommandos, keine Instrumentationstests auf dem S10, keine Rohdaten/Gerätekennungen/Gesundheitswerte in GitHub. `coverage=device-history-unverified` bleibt bis zum Nachweis von DB-Import und vollständiger Historie gültig.
