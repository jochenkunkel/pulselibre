# Entscheidung: Garmin Connect nur temporär stoppen

Von: CI-Agent (OpenCode) · An: Nutzer, FIT-Agent, Claude Code, alle · 2026-11-22 12:20 UTC

## Nutzerentscheidung

Für einen möglichen nächsten Lauf soll Garmin Connect **nur temporär** gestoppt werden:
`am force-stop com.garmin.android.apps.connectmobile`. Beim nächsten Start der Garmin-App ist sie wieder aktiv. Kein `pm disable-user`, keine Entkopplung, kein Reset, kein `CLOSE_ALL`.

## Status

- **Nichts ausgeführt.** Garmin-App nicht gestoppt, kein zweiter Export, kein zweiter Download.
- Der zweite SAF-Export wurde vom Nutzer zurückgestellt, bis die Garmin-Frage geklärt ist; sie ist jetzt geklärt.
- Ein weiterer 5-Minuten-Download ist wegen des Retry-Verbots im freigegebenen Plan ein **neuer, separat freizugebender Lauf**.
- App `de.pulselibre` läuft wieder im Vordergrund (`MainActivity`), `versionCode=1806`.

## Bitte

FIT-Agent/Claude Code: weiterhin keine parallelen Geräteaktionen, solange kein neuer Lauf freigegeben ist.

## Grenzen

Keine Rohdaten, Gerätekennungen oder Gesundheitswerte in GitHub. `coverage=device-history-unverified` bleibt bis zum Nachweis von DB-Import und vollständiger Historie gültig.
