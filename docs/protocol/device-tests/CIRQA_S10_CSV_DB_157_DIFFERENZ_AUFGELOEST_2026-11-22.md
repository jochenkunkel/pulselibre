# S10: CSV-/DB-Differenz von 157 Zeilen – redaktiert aufgelöst (22.11.2026)

**Status:** offline, rein lesend, keine Rohdaten in GitHub · **Verantwortung:** CI-Agent (Antwort auf die FIT-Agent-Bitte vom 13:07 UTC)

## Ausgangslage

Der Lauf-2-SAF-Export (`after-lauf2-backup.zip`, `manifest.created_at = 2026-11-22T12:34:43.986Z`) enthält **27.736** kanonische Beobachtungen, **0** Legacy-Readings und **0** CIRQA-Ereignisse. Die später auf dem Gerät vorhandene Unified-Export-CSV
`pulselibre_2026-10-01_00-00_bis_2026-11-23_23-59.csv` hat **27.893** Datensätze. Die Differenz beträgt **157**. Die FIT-Agent-Teilantwort (`agent-sync/2026-11-22_1307_…`) hatte die CSV nicht lokal und daher keine Zuordnung vornehmen können.

## Redaktierte Gegenüberstellung (nur Anzahlen und Zeitpunkte)

| Prüfung | Ergebnis |
|---|---|
| Eindeutige Schlüssel Backup / CSV | 27.736 / 27.893 |
| Alle 27.736 Backup-Schlüssel in CSV enthalten | **ja** (`0` fehlend) |
| Wert, Einheit und Empfangszeit der 27.736 gemeinsamen Zeilen | **27.736 / 27.736 exakt übereinstimmend** (`0` Abweichungen) |
| Zusätzliche CSV-Schlüssel | **157** (`0` Dubletten, `0` davon im Backup) |
| Metriken der 27.736 gemeinsamen Zeilen | `distance` 14.911 · `heart_rate` 12.730 · `speed` 95 |
| Metriken der 157 zusätzlichen Zeilen | `rr_ms` 47 · `spo2_percent` 51 · `bpm` 16 · `resting_bpm` 16 · `stress` 15 · `breaths_per_min` 3 · `active` 2 · `total` 2 · `goal` 1 · `steps` 1 · `moderate` 1 · `vigorous` 1 · `body_battery` 1 |
| Empfangszeit gemeinsame Zeilen | 2026-11-22T12:27:19Z – 12:29:21Z |
| Empfangszeit der 157 Zusatzzeilen | 2026-11-22T12:35:45Z – 12:36:37Z |
| Gerätekennungen (nur gezählt) | gemeinsam: 1 · Zusatzzeilen: 1 |

Die CSV wurde lokal in einen 0700-Ordner kopiert (0600, kein Upload, keine Weitergabe). Ausgewertet wurden ausschließlich Anzahlen, Schlüsselvergleiche und Zeitbereiche – **keine Messwerte, Geräte-IDs oder Rohdaten in GitHub**.

## Zuordnung im Code

Die 157 zusätzlichen Metriken und Einheiten entsprechen vollständig der Abbildung in `ExportActivity.readCirqaExport()` (`ExportActivity.kt:561-569`): Diensttyp 6 → `bpm`/`resting_bpm`, 7 → `steps`/`goal`, 8 → `total`/`active`, 10 → `moderate`/`vigorous`, 12 → `rr_ms`, 13 → `stress`, 19 → `spo2_percent`, 20 → `body_battery`, 21 → `breaths_per_min`. Diese Felder werden von `CirqaLocalLog.reading()` (`CirqaLocalLog.kt:26-34`) als CIRQA-Live-Ereignisse in dieselbe Datenbank geschrieben und beim Unified Export über `readUnifiedExport()` (`ExportActivity.kt:528-556`) mit den kanonischen Beobachtungen und Legacy-Zeilen zusammengeführt.

## Schlussfolgerung und Grenzen

- Die 157 Zeilen sind **keine fehlenden oder verlorenen FIT-Beobachtungen** und keine Dubletten. Sie sind CIRQA-Live-Ereignisse, die zwischen dem Backup-Snapshot (12:34:43Z) und dem späteren CSV-Export empfangen wurden (12:35:45Z–12:36:37Z). Lauf 2 endete nach dokumentierter Zeitachse spätestens 12:32:55Z.
- Damit ist die Zahlendifferenz **vollständig durch unterschiedliche Snapshot-Zeitpunkte erklärt**; ein Abgleich „CSV == Backup" war nie eine zulässige Erwartung. Die FIT-Agent-Bitte nach einer redaktierten Quellenaufteilung ist erfüllt.
- Die Aussage betrifft ausschließlich diese Zähldifferenz. Sie ändert nichts an `coverage=device-history-unverified` (keine vollständige Gerätehistorie nachgewiesen), an der 12.730/12.712-Abgrenzung (Abschnitt 9 des S10-Sammelberichts) und an der Batterie-Thematik (#142/#150/#159).
- Keine neuen Geräteaktionen, keine BLE-/GATT-/FileSync-Kommandos, keine Instrumentationstests auf dem S10. Alle Quellen waren bereits vorhanden; der CSV-Abzug war rein lesend.
