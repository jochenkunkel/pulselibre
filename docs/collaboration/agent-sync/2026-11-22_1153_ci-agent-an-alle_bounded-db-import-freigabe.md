# Freigabe: begrenzter S10-Gerätetest mit frischem APK

Von: CI-Agent (OpenCode) · An: Nutzer, FIT-Agent, Claude Code, alle · 2026-11-22 11:53 UTC

## Freigabe (Nutzerangabe 11:52 UTC)

Der Nutzer hat ausdrücklich die Option „Frisches APK + 5-Min-Download" gewählt und ergänzt, dass S10 und Armband zur Verfügung stehen. Damit ist die Ausführung des schriftlich definierten Tests aus `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_TEST_PLAN_2026-11-22.md` freigegeben.

## Konkreter Umfang

1. Read-only Geräte-/Buildprüfung.
2. Frisches signiertes Release-Update-APK aus erfolgreichem CI-Lauf installieren.
3. SAF-Export durch den Nutzer am Gerät; offline Prüfung der exportierten DB.
4. Falls nötig: vorhandener Historien-Download über die UI, max. 5 Minuten, kein Retry; danach erneut exportieren und vergleichen.
5. HCI nur über die bereits bestehende freigegebene Snoop-Funktion.

## Stop-Bedingungen

Sofort Abbruch bei unerwarteter Geräteantwort, Pairing-/Reset-Aufforderung, unklarem Verbindungsabbruch, destruktiver Aufforderung, Überschreitung von 5 Minuten oder neuer Unklarheit. Kein Retry, keine Schleife, kein „einfach weiterprobieren".

## Für andere Agenten

- **FIT-Agent:** bitte kurz melden, ob du gerade am S10 oder Band arbeitest. Ich starte erst nach deiner Rückmeldung bzw. nach bestätigter Abwesenheit paralleler Gerätearbeit. Keine parallelen Geräteaktionen.
- **Claude Code:** keine GUI-Änderung während des Laufs; rein lesende Beobachtung ist willkommen.
- Nutzer: für den SAF-Export brauche ich deine Interaktion am Gerät; ich melde mich mit der konkreten Bedienkette.

## Nicht anfassen

Instrumentationstests auf physischen Geräten, neue BLE-/GATT-/FileSync-Kommandos, 5008/Archive/Erase/SetFlags, `CLOSE_ALL`, Reset, Entkoppeln, Firmware, Löschen/Überschreiben echter Daten. Keine Rohdaten, Gerätekennungen oder Gesundheitswerte in GitHub; öffentlich nur redaktierte Anzahlen/Größen/CRC-Status.

## Grenzen

`coverage=device-history-unverified` bleibt bis zum Nachweis von DB-Import **und** vollständiger Historie gültig. Ein positiver Befund wäre ein erster DB-Import-Nachweis, kein Beleg für Vollständigkeit.
