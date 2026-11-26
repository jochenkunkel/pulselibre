# OpenCode START – GUI-Abstimmungen und #637 Import-Recherche

- Zeitpunkt: 2026-11-26 08:00 UTC
- Auftrag: offene FIT-Fragen an den GUI-Agenten aus `agent-sync/` und dem jüngsten Projektkontext beantworten, danach offline den CIRQA-Download-/Archiv-/FIT-Importpfad auf reproduzierbare Fehler untersuchen.
- Geplante Dateien: neue Abstimmungsantwort, `agent-sync/INDEX.md`, eigene Aktivitätssitzung; Produktcode nur bei belegtem, testbarem Fehler.
- Erwartetes Ergebnis: konkrete GUI-Entscheidungen/Unsicherheiten schriftlich festhalten und einen reproduzierbaren Download-/Importfehler mit Regressionstest identifizieren oder die verbleibende Evidenzlücke präzise benennen.
- Grenzen: keine S10/S25-/ADB-/BLE-Aktion, keine Laufbudgetänderung, keine öffentlichen Rohdaten.
- Abhängigkeit: GUI-Agent kann nach der Antwort fortfahren; kein Review/Bestätigungsschritt erforderlich.

## UPDATE (2026-11-26 08:20 UTC)
- Offene GUI-Fragen schriftlich beantwortet: Restore braucht Writer-Quieszenz; #528 bleibt bei accepted-sync Notifications, TX ist nicht enthalten; Archiv bleibt inhaltsadressiert, Ereignis-Trace wäre separat.
- Im Import-/History-Projektionspfad ist eine konkrete Cache-Invalidierungslücke gefunden: `UnifiedMeasurements.refreshHistorical()` bildet seine Signatur aus Zeilenzahlen und `output_count`, aber nicht aus den aktiven FIT-Generation-IDs. Ein Wechsel der aktiven Generation mit gleicher Output-Anzahl lässt dadurch die alte `unified_measurements`-Projektion stehen. Ein gezielter Android-Regressionstest und die Korrektur der Signatur sind vorbereitet.
- Kein Hardwarezugriff. Android-SDK war zunächst nicht per Environment konfiguriert; gezielter Gradle-Build/Test läuft jetzt mit vorhandenem SDK.
- Ergebnis: Gradle 9.6.0 `:app:compileDebugAndroidTestKotlin` und `:app:testDebugUnitTest` erfolgreich; Android-Instrumentationstest nur kompiliert, nicht ausgeführt. Befund und Grenzen stehen in `docs/protocol/research/ISSUE637_UNIFIED_HISTORY_PROJECTION_INVALIDATION_2026-11-26.md`.
- Zusätzliche offene GUI-Bitte zum teuren FIT-Reindex beantwortet: unveränderte Quellen dürfen später nur mit vollständigem SHA-/Decoder-/Profilversions- und Completion-Nachweis übersprungen werden; Unknown-Field-Scan benötigt eigenen Complete-Marker, da leere Inventarzeilen nicht „bereits erfolgreich geprüft“ beweisen. GUI kann Worker-Scheduling unverändert lassen; FIT-Folgeoptimierung separat.
