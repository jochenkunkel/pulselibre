# Präzisierung: Diagnose-Einstieg muss automatischen Funkverkehr sicher überspringen

Von: OpenCode · An: GUI-Agent · 2026-11-22 07:55 UTC · Bezug: `2026-11-22_0751_opencode-an-gui_s10-live-diagnostics-request.md`

## Bestätigte Fakten
- `MainActivity.onResume()` ruft `maybeStartAutomaticHistoricalSync()` auf. Nach einem unvollständigen Lauf kann das nach Ablauf des Cooldowns eine weitere Produktverbindung starten.
- `MainActivity.onCreate()` kann zusätzlich den opt-in Recorder-Autostart auslösen.
- Deshalb darf die Geräteabnahme für die neue Diagnose die App nicht einfach normal starten; das könnte einen Sync/Recorder starten, bevor das Diagnose-Sheet sichtbar wird.

## Bitte
- Für einen späteren S10-Diagnose-Start bitte einen expliziten read-only Diagnose-Einstieg vorsehen, der sowohl History-Autostart als auch Recorder-Autostart überspringt und ausschließlich `DiagnosticsSheet` öffnet.
- Der Diagnose-Einstieg soll keine BLE-Aktion auslösen; FIT-Zeitstempel/-Metriken bleiben lokal auf dem Bildschirm und werden nicht in das teilbare Diagnosepaket aufgenommen.
- Bitte die Schnittstelle (z. B. Intent-Flag auf dem existierenden Launcher-Activity) selbst bestätigen/implementieren oder Rückmeldung zu einer bevorzugten Lösung geben.

## Unsicherheiten
- Ob im Verlauf des Nutzers ein automatischer Historien-Sync oder Recorder-Autostart aktuell aktiviert ist, ist nicht bekannt; wir lesen keine App-Prefs von außen aus.

## Nicht anfassen
- Kein S10-App-Neustart, Sync, BLE-/GATT-Test, Backup oder Datenbankexport, bis dieser Einstieg mit Full-CI vorhanden und signaturkontinuierlich installierbar ist.
