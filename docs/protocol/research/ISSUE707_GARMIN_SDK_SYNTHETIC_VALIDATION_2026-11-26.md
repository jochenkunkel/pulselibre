# #707 – unabhängiger Garmin-SDK-Vergleich synthetischer FIT-Vektoren

Stand: 26.11.2026. Offline-/synthetische Validierung; kein Gerätezugriff.

## Referenz und Grenzen

Der GUI-Agent meldet `garmin-fit-sdk==21.222.0` (Garmin International,
`garmin/fit-python-sdk`) als unabhängige Profil-/Decoderreferenz. Dekodiert
wurden synthetisch erzeugte FIT-Dateien mit gültigem Header/CRC. Die
FIT-Originale waren in dessen Arbeitsumgebung nicht verfügbar (`NOT_AVAILABLE`);
es gab keinen Vergleich mit privaten Originaldateien und keine repräsentative
Genauigkeitsmessung für reale CIRQA-Daten.

Referenzmeldung: [Issue #707, SDK-Vergleich](https://github.com/jochenkunkel/pulselibre/issues/707#issuecomment-7200705681).

## Aggregiertes Ergebnis

| Feldgruppe | Fälle | Befund |
|---|---:|---|
| Monitoring 55/24 `current_activity_type_intensity` | 3 | 3 SDK/PulsLibre-Matches |
| Monitoring 55/26 `timestamp_16` | 1 | Nicht direkt vergleichbar: SDK gibt Rohwert aus, rekonstruiert nicht; PulsLibre nutzt den validen Zeitanker nach Garmin-Regel |
| Monitoring 55/27 Herzfrequenz | 4 | 3 Matches; einzige Abweichung ist HR=0: SDK gibt 0 aus, PulsLibre klassifiziert 0 bewusst nicht als Messwert |
| Global 297, Feld 0 | 5 | 5 Matches einschließlich signed/invalid-Klassifikation |
| Global 269, Felder 0–2 | 9 | 9 Matches einschließlich SpO₂-Invalidwert und Confidence-Invalidklassifikation |
| Global 398, Felder 1–2 | 2 | 2 Matches |
| Global 227, Feld 0 | 5 | 5 Rohwert-/Invalidmatches; PulsLibre kanonisiert nur den belegten plausiblen Bereich |
| Global 275, Feld 0 | 5 | 5 Matches |
| Global 233 und 397 | — | Im Garmin-Profil nicht vorhanden; semantisch unbekannt, deshalb Roh-/Unknown-Behandlung bleibt |

Gesamt: 32/33 synthetische Feldinstanzen matchen in Wert und Invalid-Klassifikation;
eine absichtliche Abweichung (HR 0), ein Fall ist wegen fehlender SDK-ts16-
Rekonstruktion nicht direkt vergleichbar. Das ist **kein** „32/33 auf
Originaldaten“ und begründet keine ≥95-%-Behauptung.

## Konsequenz für #707

- PR #708 bleibt die bereits gemergte Implementierung von 55/24 als
  nichtklinischer Review-/Aktivitätscode.
- Der synthetische Gegenvergleich stützt die Standardprofilzuordnungen für
  55/24, 269, 297 und 398 in den geprüften Vektoren.
- 55/26 wird von der Referenz nicht rekonstruiert; der PulsLibre-Zeitpfad muss
  weiter durch Garmin-Regel/Anker-Tests belegt werden.
- Die HR-Null-Abweichung ist eine bewusste Import-/Qualitätsentscheidung,
  keine SDK-Parserabweichung.
- Proprietäre 233/397 und die Bedeutung/Skalierung von SpO₂-Konfidenz bleiben
  offene Forschungsfragen. Ein gültiger Profilwert beweist keine klinische
  Semantik.
- Abdeckung/Genauigkeit auf echten CIRQA-Originalen und die bisher geforderte
  ≥95-%-Validierung bleiben offen. Keine neuen Originaldaten wurden ins
  Repository übernommen.
