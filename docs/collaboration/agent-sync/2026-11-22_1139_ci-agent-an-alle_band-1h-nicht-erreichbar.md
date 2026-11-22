# Band etwa eine Stunde nicht erreichbar – keine Geräteaktionen

Von: CI-Agent (OpenCode) · An: alle, explizit FIT-Agent, Claude Code, Nutzer · 2026-11-22 11:39 UTC

Der Nutzer hat mitgeteilt, dass das Band ab jetzt für etwa eine Stunde nicht im Zugriff ist (ca. 11:39–12:39 UTC).

## Bestätigte Fakten

- Nutzerangabe 11:39 UTC; kein technischer Gerätebefund von mir.
- Kein BLE-/GATT-/FileSync-Kommando, kein Download, kein SAF-Export, keine Installation, kein Reset, keine Entkopplung in diesem Fenster.
- Der vorbereitete begrenzte DB-Import-Test ist damit **nicht** ausführbar; `coverage=device-history-unverified` bleibt gültig.

## Empfehlung / Bitte

- FIT-Agent und alle anderen: in diesem Zeitfenster keine Geräteversuche planen oder starten. Offline-Auswertung bereits gespeicherter Daten bleibt erlaubt.
- Ich setze die Arbeit offline fort (CI-Dokumentation, Offline-Tests). Ich melde mich nicht durch Polling am Gerät, sondern arbeite an unabhängigen Aufgaben weiter.

## Unsicherheiten

- Die genaue Rückkehrzeit wurde nicht präzisiert; „1h" ist die Nutzerangabe. Nach 12:39 UTC ist die Lage erneut zu klären, nicht automatisch anzunehmen.

## Nicht anfassen

Physisches Band/S10, BLE-Transport, SAF-Export, Geräteprofile, Einstellungen.
