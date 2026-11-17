# Testnachweise und Verifikationsberichte

Dieser Bereich sammelt **datierte, reproduzierbare Testnachweise** für Hardware, CIRQA-Protokoll, Android und CI. Ein Versuchsbericht ist **keine** Freigabe einer Funktion.

## Dokumenttypen
- **Testplan:** Voraussetzungen, Risiken, Abbruchbedingungen, erwartetes Ergebnis.
- **Testprotokoll:** tatsächlich ausgeführte Schritte, Zeitpunkt, Umgebung, beobachtete Ergebnisse.
- **Verifikationsbericht:** Soll-Ist-Vergleich, nachvollziehbare Evidenz, offene Abweichungen.
- **CI-/Buildnachweis:** Commit, Workflow/Job, Resultat, Logreferenz; unbekannte Resultate als unbekannt markieren.

## Vorhandene Berichte – kanonische Pfade
- [CIRQA Bootstrap-Gerätetest (2026-11-17)](../protocol/device-tests/CIRQA_BOOTSTRAP_DEVICE_TEST_2026-11-17.md)
- [5050-Gerätetest](../protocol/device-tests/CIRQA_5050_TARGETED_DEVICE_TEST_2026-11-17.md)
- [5050 ACK: zweiter Gerätetest](../protocol/device-tests/CIRQA_5050_ACK_SECOND_DEVICE_TEST_2026-11-17.md)
- [5050: dritter Test und Phasenfix](../protocol/device-tests/CIRQA_5050_THIRD_TEST_AND_PHASE_FIX_2026-11-17.md)
- [Post-5050-Evidenz](../protocol/experiments/CIRQA_POST_5050_EVIDENCE_2026-11-17.md)
- [5024 ACK Wire-Audit](../protocol/experiments/CIRQA_5024_ACK_WIRE_AUDIT_2026-11-17.md)
- [S25-BLE-Testplan](../S25_BLE_TEST_PLAN.md)
- [CI-Teststrategie](../CI_TEST_STRATEGY.md)

## Qualitätsregel
Kein Test darf allein aufgrund eines geschriebenen Dokuments als bestanden gelten. Hardwarebeobachtung, Simulation, statische Prüfung und automatisierter Test müssen getrennt ausgewiesen werden. Datenschutz: keine Geräteadressen, personenbezogenen Messdaten oder unbereinigten Rohpayloads veröffentlichen.

**Ablage:** CIRQA-Gerätetests liegen kanonisch unter `docs/protocol/device-tests/`, Protokollversuche unter `docs/protocol/experiments/`. Dieser Index verlinkt die Nachweise fachübergreifend; die alten Weiterleitungsdateien wurden entfernt.
