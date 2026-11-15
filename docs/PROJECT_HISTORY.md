# PulseLibre — Projektchronik und Entscheidungen

## Ursprung
Die Projekte Mein Weg 2 und PulseLibre entstanden im persönlichen Kontext eines Knochenbruchs, einer Operation und der Genesung. Das vom Projektinhaber bereitgestellte Röntgenbild zeigt die rechte Schulter/den Oberarm mit operativer Stabilisierung. **Das Original ist sensibles medizinisches Bildmaterial und wird nicht ohne gesonderte ausdrückliche Freigabe ins Git-Repository übernommen.** Stattdessen wird ein abstrahiertes grafisches Motiv verwendet.

## Projektauftrag
- Eigenständige, private Android-App als datenschutzorientierte Alternative zur Garmin-Cloud.
- Zielgerät: Garmin CIRQA Smartband; aktuell bereits mit Garmin Connect gekoppelt.
- Vorläufig keine Entkopplung, kein Reset und keine Beeinträchtigung vorhandener Daten.
- Direkte BLE-Erkennung, GATT-Diagnose, später verifizierte Sensordaten und lokale Historie.
- Qualitätsstandard nach Mein Weg 2: Build, Unit-Tests, Android Lint, Emulator-Tests und CI vor Geräteinstallation.

## Umgesetzte Schritte
1. Privates Repository `jochenkunkel/pulselibre` eingerichtet.
2. Android/Kotlin-Prototyp: BLE-Scan, Verbindung, GATT-Dienste und Diagnoseansicht.
3. Android-CI, JVM-Unit-Test, Instrumentation-Starttest und Prüfung fehlender INTERNET-Berechtigung.
4. Qualitätsplan in `docs/QUALITY_GATE.md` und Testplan in `docs/TESTPLAN.md`.
5. Fehlgeschlagener CI-Lauf #7 analysiert: `android-actions/setup-android@v3` scheiterte beim veralteten SDK-Paket `tools`; Schritt entfernt, CI-Erfolg muss erneut geprüft werden.
6. Design aus Mein Weg 2 aufgegriffen: Primärfarbe `#5127C9`, helle Flächen, großzügige Rundungen.
7. Eigenständiges PulseLibre-Motiv mit abstrahiertem Oberarm-Fixationsmaterial und Pulskurve sowie Poster gestaltet.

## Grenzen und Wahrhaftigkeit
- Die gezeigten Dashboard-Messwerte auf einem Konzeptposter sind illustrative Beispieldaten, **keine echten Garmin-Messwerte**.
- Die Garmin-CIRQA-Protokollunterstützung ist nicht implementiert oder verifiziert.
- Eine bestandene Emulatorpipeline kann echte Bluetooth-Hardware nicht vollständig simulieren.
- Keine APK-Freigabe, bevor die obligatorischen CI-Prüfungen erfolgreich sind und offene kritische Tests bearbeitet wurden.
- Das originale Röntgenbild und andere personenbezogene Gesundheitsdaten bleiben außerhalb des Repositories.

## Reproduzierbare Designquellen
- `design/pulselibre-icon.svg`: editierbares Vektorlogo.
- `design/pulselibre-poster.svg`: editierbares, datensparsames Poster mit ausschließlich belegbaren Projektzielen.
- `docs/BRANDING.md`: Farbpalette, Claim und Markenbeschreibung.
