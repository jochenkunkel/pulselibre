# Übernahme: manifestbasierter CIRQA-Skip-Pfad

- Von: de ci-agent (OpenCode) · An: alle, insbesondere Bearbeiter von #171/#148 · 2026-11-21 10:00
- Bezug: `2026-11-20_2333_de-ci-agent-an-alle_targeted-history-pull.md`, #171/#148

## Bestätigte Fakten

- Nutzer hat die Codeintegration der Skip-Policy freigegeben; Geräte-Datei-Pulls sind nicht Bestandteil dieser Freigabe.
- Drei getrennte erfolgreiche No-pull-Sitzungen lieferten je 41 Einträge und denselben privaten aggregierten Listenfingerprint. Keine davon hat einen Dateiimport geprüft.
- `CirqaTargetedPullPolicy` ist bisher nur eine nicht angebundene Policy; der bestehende Sync-Pfad queued aktuell gelistete Dateien zum Pull.
- Es gibt einen offenen Issue-#171-Arbeitsstand für die BLE-/Sync-Architektur. PR #345 ist bereits gemergt; die damalige konkrete Dateikollision ist erledigt.

## Empfehlung / Bitte

- Ich übernehme die gezielte Manifest-/Skip-Integration für #148/#171 in diesem Branch.
- Bitte keine parallelen Änderungen an `CirqaMlHandshakeProbe`, Manifestpersistenz oder der gezielten Pull-Auswahl, bis ich Schnittstelle und betroffene Dateien abgegrenzt habe. Bestehende BLE-Sequenz bleibt unverändert; keine automatische Geräteaktion.
- Vor Schemaänderungen prüfe ich die bestehende Backup-/Migrationsschnittstelle; keine Datenbank-/Backup-Datei wird stillschweigend geändert.

## Unsicherheiten

- `nextTransactionId`/Seitenvollständigkeit und stabile per-UID-Zuordnung sind nicht gleichbedeutend mit nachgewiesener Gerätehistorien-Vollständigkeit.
- Die drei Listenvergleiche belegen keine Inhaltsidentität eines früher importierten FIT und keine dauerhafte UID-Stabilität.
- Das Skip darf erst bei exakter UID-/Typ-/Größenidentität, validierter Persistenz/SQLite-Importzuordnung und ausreichend belastbarem lokalen Stabilitätsnachweis greifen; alle unbekannten Fälle bleiben pull-pflichtig.

## Nicht anfassen

- Keine neuen BLE/GATT-Kommandos oder Sequenzen, keine Änderung am Transportverhalten, kein Geräte-Datei-Pull, kein Reset, keine Flags, kein `CLOSE_ALL`.
- Keine UI-/Dashboard-Dateien.
