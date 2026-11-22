# START – Lauf 2 des begrenzten DB-Import-Tests

Von: CI-Agent (OpenCode) · An: Nutzer, FIT-Agent, Claude Code, alle · 2026-11-22 12:22 UTC

## Freigabe

Nutzer 12:19 UTC: „Freigabe für den neuen Lauf erteilt". Garmin Connect soll nur temporär gestoppt werden. Umfang: vorhandene UI `Gerät` → `Historie herunterladen` → `Herunterladen`, höchstens 5 Minuten, kein Retry, danach SAF-Export und redaktierter Vergleich. Plan: `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_LAUF2_PLAN_2026-11-22.md`.

## Vorbedingung aus Nutzerhinweis

Der Nutzer erinnerte an einen früher deaktivierten Garmin-Connect-Dienst. Bisheriger Befund: zwei Notizen (15.11. und 21.11.) sagen „deaktiviert" bzw. „deinstalliert", aber ohne Servicename; das Paket ist aktuell in `pm list packages -e`. Ein gründlicher Dokumentationsdurchsuchlauf läuft. **Bis zu dessen Ergebnis wird Garmin nicht verändert und kein Download gestartet.**

## Bitte

FIT-Agent/Claude Code: weiterhin keine parallelen Geräteaktionen. Nutzer: bitte noch nicht `Herunterladen` antippen; ich melde mich, wenn die Vorbedingung geklärt ist.

## Grenzen

Keine Instrumentationstests auf physischen Geräten, keine neuen BLE-/GATT-/FileSync-Kommandos, kein Reset/Entkoppeln/`CLOSE_ALL`, keine dauerhafte Garmin-Deaktivierung. Keine Rohdaten/Gerätekennungen/Gesundheitswerte in GitHub. `coverage=device-history-unverified` bleibt bis zu beiden Nachweisen gültig.
