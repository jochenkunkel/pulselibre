# Wissenschaftliche Messwerte und Berechnungen

**Kanonischer Kachelbauplan:** [Wissenschaftlicher Dashboard-Kachelbauplan](SCIENTIFIC_DASHBOARD_TILE_BLUEPRINT.md) – Metrik-ID, Rohsignal, Formel, Grenzen, Datenqualität, Tests, Quellen.

**Grafisch erklärt:** [Neun Berechnungs- und Datenqualitätsdiagramme](../diagrams/BERECHNUNGEN.md) mit Formeln, Codequellen und Freigabestatus.

## Wissenschaftliche Grundlagen (weitere Dokumente noch unter docs/)

- [Wissenschaftliche Referenzen](../SCIENTIFIC_REFERENCES.md)
- [Verbindliche wissenschaftliche Engineering-Standards](../ENGINEERING_SCIENTIFIC_STANDARDS.md)
- [Dashboard-Messwertregeln](../protocol/data-model/CIRQA_DASHBOARD_GROUND_RULES.md)
- [Schlaf-/Bewegungsregeln](../protocol/data-model/CIRQA_SLEEP_MOTION_GROUND_RULES.md)

## Klare Trennung

1. **Messwert:** vom Gerät empfangene, verifizierte Daten mit Einheit, Herkunft und Zeitstempel.
2. **Berechnung:** aus nachweislich geeigneten Zeitreihen abgeleitete Größe, mit Formel/Tests.
3. **Interpretation:** nur mit passender Studie, Zielgruppe und Messbedingungen.
4. **Darstellung:** Farben, Kachellayout und Navigation sind [UI-Themen](../ui/README.md), keine wissenschaftliche Evidenz.
5. **Protokoll:** welches Datenfeld überhaupt existiert und wie es dekodiert wird, steht im [Protokollbereich](../protocol/README.md).

**Keine universellen Ampelgrenzen** für HRV, Stress, Body Battery oder einzelne SpO₂-Handgelenkwerte erfinden. Wissenschaftlich nicht bestätigte Funktionen als experimentell markieren.
