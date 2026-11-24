# PulseLibre – App-Zielbild (Konzept v0.1)

Stand: 2026-11-15. Das im Chat erstellte visuelle Smartphone-Mockup ist ein **Konzept**, keine Aufnahme der laufenden App. Die Bilddatei ist noch nicht im Repository archiviert; diese Spezifikation dient als reproduzierbare Grundlage.

## Design
- Mein-Weg-2-inspirierte Primärfarbe #5127C9, weißer Hintergrund, sanft violette Flächen, abgerundete Karten.
- Header mit PulseLibre-Logo (abstrahierte Knochenfixation und Herz/Puls) und Datenschutzversprechen „Deine Daten. Deine Kontrolle.“
- Navigation: Übersicht, Geräte, Verlauf, Einstellungen.
- Große Pulskarte, darunter Schritte und Schlaf, separate Bluetooth-Verbindungskarte.
- Bei fehlenden echten Messdaten **keine erfundenen Werte** anzeigen. Konzeptdaten immer explizit als Demo-Daten kennzeichnen.
- Offline- und Verbindungsstatus gut sichtbar; zugängliche Beschriftungen und große Touch-Ziele.

## Roadmap
1. Sichere BLE-Statusmaschine mit hardwarefreier Fehler-Demo; Unit- und Emulator-Tests.
2. Trennung zwischen Bluetooth-Transport, Geräteprotokoll und Oberfläche.
3. Lokale Speicherung und sichere Testdaten (keine Garmin-Cloud).
4. Proprietäre Garmin-Protokollanalyse erst nach isolierten Tests; bestehende Garmin-Connect-Kopplung nicht stören.
5. Dashboard mit **echten** Daten erst nach validierter Decodierung.

## Qualitätsregeln
- Keine Installation auf Nutzergerät vor ausdrücklicher Freigabe.
- Emulator kann UI und simulierte Fehler testen, nicht die echte CIRQA-Funkstrecke beweisen.
- Keine persönlichen Röntgen-Originalbilder in Git.
- Keine Internetberechtigung und keine Übertragung von Gesundheitsdaten.
