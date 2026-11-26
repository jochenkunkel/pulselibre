# #707 – Garmin-SDK-Vergleich mit echten CIRQA-FIT-Dateien

Stand: 26.11.2026, GUI-Agent nach FIT-Übergabe. Offline, kein Gerätezugriff. Nur aggregierte Zählungen:
keine Werte, Zeitreihen, Hashes oder Dateinamen.

## Datenbasis und Methode

- **Quelle:** lokales PulsLibre-Backup v11 vom 25.11.2026, privat und nicht im Repo.
- **Originale:** 37 verifizierte FIT-Quellen, alle 37 mit vollständigem Original im Datei-Archiv.
- **Referenz:** `garmin-fit-sdk==21.222.0` (Garmin International), dieselbe Profilversion wie PulsLibre. Dekodierfehler: 0.
- **PulsLibre-Seite:** die im Backup gespeicherte Ausgabe des PulsLibre-Decoders.
  - Herzfrequenz: kanonische Beobachtungen der aktiven Generationen (Decoder ≤ 3).
  - Übrige Felder: Review-Kandidaten mit Rohwerten. Seit #696 sind sie die Eingabe der kanonischen Felder; Statuscodes werden dort zusätzlich gefiltert.
- **Vergleich:** je Datei und Feld als Multimenge von (Messzeit, Wert).
  - Für `timestamp_16` rekonstruiert das SDK keine Zeit. Auf die SDK-Rohwerte wurde deshalb dieselbe Garmin-Regel angewandt (letzter voller Zeitstempel + 16-Bit-Differenz).
  - Bei `cycles` wurden die Werte zusätzlich ohne Zeit verglichen.

## Ergebnis

| Feld | Übereinstimmung | nur PulsLibre | nur SDK |
|---|---:|---:|---:|
| Herzfrequenz 20/3 (record) | 13 144 | 0 | 0 |
| Herzfrequenz 55/27 (monitoring, inkl. timestamp_16) | 559 | 0 | 0 |
| Stress 227/0 (Zeit aus 227/1) | 658 | 0 | 0 |
| Atemfrequenz 297/0 (Skala 100, signed) | 658 | 0 | 0 |
| SpO₂ 269/0 | 631 | 0 | 0 |
| SpO₂-Konfidenz 269/1 | 631 | 0 | 0 |
| **SpO₂-Modus 269/2** | **0** | 0 | **631** |
| Schlafstufe 275/0 | 22 | 0 | 0 |
| Hauttemperatur 398/1 und 398/2 | 2 | 0 | 0 |
| Aktivitätstyp 55/5 | 8 | 0 | 0 |
| Zyklen 55/3 (Rohwert = SDK × 2) | 27 von 27 nach Wert | – | – |

- **Gesamt vor dem Fix:** 16 321 Übereinstimmungen bei 16 952 vergleichbaren Feldinstanzen. Das sind **96,3 %** (ohne die 19 Zyklen-Zeitartefakte des SDK).
- **Einziger echter Fehler: 269/2.** Alle 8 echten `spo2_data`-Definitionen deklarieren Feld 2 als `uint8` (0x02); PulsLibre akzeptierte nur `enum` (0x00) und verwarf jeden Wert. Der Fix (Decoder 6) akzeptiert ein Byte als `enum` oder `uint8`, wie das SDK.
- **Nach dem Fix:** 16 952 / 16 952 = **100 %** auf diesem Bestand. Das ist rechnerisch aus denselben Daten abgeleitet, die SpO₂-Modus-Werte selbst sind unverändert. Ein erneuter Lauf mit Decoder 6 über dieselben Originale steht noch aus.

## Grenzen

- Es handelt sich um einen einzigen Bestand: 37 Dateien eines Testbands, 25.11.2026.
- Die Zeitrekonstruktion von `timestamp_16` ist mit derselben Regel geprüft, nicht unabhängig von Garmin. Das SDK selbst rekonstruiert nicht.
- 55/24 (Decoder 5, #708) und die #696-Statusfilter waren in diesem Backup noch nicht enthalten. 55/24 ist nur synthetisch gegen das SDK geprüft (3/3).
- 233 und 397 stehen nicht im Profil und sind hier nicht bewertet. Die Semantik der SpO₂-Konfidenz bleibt offen.
