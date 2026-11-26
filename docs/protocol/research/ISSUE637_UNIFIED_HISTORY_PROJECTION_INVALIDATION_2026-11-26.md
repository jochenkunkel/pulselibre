# #637 – FIT-Generationswechsel und Unified-History-Projektion

Stand: 2026-11-26 · Offline-Codebefund, kein Geräteversuch

## Befund

`UnifiedMeasurements.refreshHistorical()` verwendet `unified_state.historical_signature`, um die materialisierte `unified_measurements`-Projektion bei unverändertem Bestand nicht neu aufzubauen. Vor diesem Follow-up bestand die Signatur aus Zeilenzahlen in `observations`, `fit_observation_provenance`, `fit_generation_observations`, `fit_active_generations` sowie der Summe `fit_decoder_generations.output_count`.

`CirqaDatabase.activateFitDecoderGeneration()` kann den aktiven `generation_id` für eine Quelle austauschen, ohne diese Zahlen zu ändern. Wenn alte und neue Generation gleich viele Beobachtungen/Felder haben, bleibt die Signatur identisch. Danach kehrt `refreshHistorical()` früh zurück und `FitHistoryReader` kann weiterhin die Werte aus der vorher aktiven Generation lesen, obwohl die Aktivierung erfolgreich war. Das ist eine reproduzierbare Cache-Invalidierungslücke im FIT-Replay-/History-DB-Projektionspfad; sie ist keine Erklärung für fehlende Geräte- oder ItemList-Daten.

## Korrektur

Die Historien-Signatur nimmt jetzt zusätzlich die geordneten `(source_sha256, generation_id)`-Paare der aktiven Generationen auf. Dadurch wird ein Wechsel der Aktivierung erkannt, auch wenn alle Zeilenzahlen gleich bleiben. Ein Regressionstest erstellt zwei gleich große Generationen mit abweichendem Messwert, wechselt die aktive Generation und erwartet, dass die Unified-Projektion von alt auf neu aktualisiert wird.

## Prüfung und Grenzen

- `git diff --check`: erfolgreich.
- Gradle 9.6.0: `:app:compileDebugAndroidTestKotlin` erfolgreich; Regressionstest kompiliert.
- `:app:testDebugUnitTest`: erfolgreich.
- Der Android-Instrumentationstest wurde lokal nicht ausgeführt; kein Emulator-/Gerätetest beansprucht.
- Der Befund betrifft Aktivierung vorhandener Decoder-Generationen und die daraus materialisierte History-Ansicht. Er belegt weder einen Fehler beim BLE-Download noch die Ursache eines konkreten S10-Laufs oder vollständige Historienabdeckung.
