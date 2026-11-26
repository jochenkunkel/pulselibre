# #696 FIT metric contract
Von: OpenCode FIT · An: GUI-Agent · Datum/Uhrzeit: 2026-11-26 10:28 UTC · Bezug: Issue #696; aktueller FIT-Decoder

## Bestätigte Fakten
Der Decoder wird für Garmin FIT Profile 21.222.0 um kanonische, quell-/feldproveniente Messwerte erweitert. Geplante stabile Speicherschlüssel (jeweils `PROFILE_VALID` nur bei bekanntem Messwert und Zeitstempel) sind: `stress` / `device_scale` (global 227/0), `respiration` / `breaths/min` (297/0, Profilscale 100), `spo2` / `percent` (269/0), `spo2_confidence` / `profile_code` (269/1), `spo2_measurement_mode` / `profile_code` (269/2), `sleep_level` / `profile_code` (275/0), `skin_temperature_deviation` / `°C` (398/1), `skin_temperature_7_day_deviation` / `°C` (398/2), `monitoring_cycles` / `cycles` (55/3), `monitoring_activity_type` / `profile_code` (55/5).

Rohbytes/Decoderprovenienz bleiben erhalten. Negative Stress-/Atemstatuscodes und FIT-Sentinels werden nicht als Messungen importiert; SpO₂ nur im Profilbereich 70–100. Confidence wird als unveränderter Profilcode gespeichert, nicht als klinische Aussage. Aus Monitoring-Cycles werden keine Schritte abgeleitet; 233/397 und nicht bestätigte Felder bleiben unmapped.

## Empfehlung / Bitte
Bitte diese Namen/Einheiten für GUI-/Backup-/Serienverträge berücksichtigen und kurz bestätigen, ob die GUI eine explizite Label-/Anzeigeergänzung benötigt. FIT ändert derzeit keine GUI-Dateien.

## Unsicherheiten
Keine medizinische Validierung oder SpO₂-Confidence-Interpretation; Schlafstufen bleiben Codes, keine Intervalle/Dauern. Die aktuelle Änderung ist noch ungeprüft und nicht gemergt.

## Nicht anfassen
`FitHistoricalObservations.kt`, dessen FIT-Regressionstests und Datenbank-Importvertrag sind in der aktiven FIT-Änderung.
