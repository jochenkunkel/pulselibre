# CIRQA-Archiv-/FIT-Reconciliation aus privaten Vorher-/Nachher-Backups (25.11.2026)

## Fragestellung und Grenzen

Read-only-Abgleich der lokal vorhandenen privaten SQLite-Backups `pre-run` und
`post-run` zur Prüfung der Kette **Originalarchiv → FIT-Quelle → aktive
Decoder-Generation → kanonische Beobachtungen**. Es wurden weder Gerätezugriff
noch BLE-Aktionen ausgeführt. Rohbytes, Hashes, Referenzbytes, Dateinamen und
Messwerte werden hier nicht veröffentlicht.

Der Abgleich prüft nur die beiden vorliegenden Backup-Snapshots. Er beweist
weder, dass die ItemList vollständig war, noch die Vollständigkeit der CIRQA-
Historie oder die Semantik nicht kanonisierter Felder.

## Reproduzierbare Zähler

Beide Archive sind Backup-v11 mit jeweils 23 manifestierten Einträgen. Für alle
Einträge stimmen Länge, SHA-256 und JSONL-Recordzahl mit dem Manifest überein.
Die Original-Archiv-Chunks wurden gruppiert, nach Offset zusammengesetzt und
gegen deklarierte Gesamtlänge sowie SHA-256 geprüft; beide Snapshots bestehen
diese Prüfung ohne Fehler.

| Bestand | Vorher | Nachher | Delta |
| --- | ---: | ---: | ---: |
| eindeutige Original-Content-Hashes | 1.689 | 1.938 | +249 |
| Bytes der neuen Originale | – | – | 72.215 |
| neue eindeutige Inhalts-/Referenzpaare | – | – | 251 |
| verifizierte FIT-Originale | 27 | 37 | +10 |
| kanonische FIT-Quellen | 27 | 37 | +10 |
| aktive FIT-Generationen | 27 | 37 | +10 |
| Generation-Feldzeilen | 149.167 | 163.084 | +13.917 |
| Generation-Beobachtungszeilen | 28.564 | 29.123 | +559 |
| kanonische Beobachtungen / Provenienzzeilen | 28.564 / 28.564 | 29.123 / 29.123 | +559 / +559 |
| FIT-Unmapped-Field-Inventarzeilen | 11.724 | 12.473 | +749 |
| FIT-Review-Kandidaten | 0 | 2.635 | +2.635 |
| Importfehler-Zeilen | 0 | 0 | 0 |

Die 37 nachher verifizierten FIT-Originale stimmen per SHA-256 vollständig mit
den 37 kanonischen FIT-Quellzeilen überein; alle 37 Quellen besitzen einen
aktiven Decoder-Generationspointer. Das belegt für diesen Snapshot die
Archiv-/Import-Reconciliation, nicht die Vollständigkeit der vom Gerät
angebotenen oder übertragenen Historie.

Review-Kandidaten nachher (nur aggregierte Profilprojektionen): 658 Stress,
631 SpO₂, 631 SpO₂-Konfidenz, 658 Atemfrequenz, 22 Schlaf-Level,
27 Monitoring-Zyklen und 8 Monitoring-Aktivitätstypen. Sie bleiben als
`UNTRUSTED_PROFILE_CANDIDATE` getrennt von kanonischen Messwerten; die Zähler
bestätigen keine Produktsemantik oder medizinische Validierung.

## Reconciliation und Audit-Interpretation

- Die 249 neu hinzugekommenen Originale umfassen FIT- und andere archivierte
  Inhalte; die Gesamtsumme darf nicht als „249 FIT-Dateien“ interpretiert
  werden. Davon sind 10 neue FIT-Originale durch FIT-Magic und vollständigen
  Datei-Hash verifiziert und deckungsgleich mit 10 neuen FIT-Quellen.
- Zu den neuen Inhalts-/Referenzpaaren gehören 238 als `notification`
  gelabelte Referenzen und 13 weitere Item-Referenzen. Zwei der 251 neuen
  Referenzpaare verweisen auf bereits vorhandene Inhaltsbytes; daher sind
  Referenz-, Inhalts- und FIT-Dateizähler bewusst getrennt.
- Die 559 zusätzlichen kanonischen Beobachtungen haben jeweils eine
  Provenienzzeile; zusätzlich wurden 13.917 exakte FIT-Feldvorkommen in
  Generationen und 749 aggregierte Unmapped-Field-Inventarzeilen ergänzt.
- Ein älterer lokaler Chunk-Audit markierte in beiden Archiven eine Gruppe als
  unvollständig. Ursache ist die Formatsemantik: JSONL-`bytes` enthält die
  **Gesamtdateigröße**, auch bei jedem einzelnen Chunk, nicht dessen
  Chunklänge. Der unabhängige Check setzt die Chunks anhand der Offsets
  zusammen, prüft den resultierenden Gesamtumfang und Hash und findet in
  beiden Snapshots null fehlerhafte Gruppen. Das ist ein Fehler des lokalen
  Audits, kein nachgewiesener Archivverlust.
- `import_runs` ist kein Ersatz für den Sync-Terminalbericht: Die Tabelle
  enthält keine per-Datei-Laufzuordnung dieser Vorher-/Nachher-Deltas. Die
  hier belegte Bestandsänderung wird deshalb nicht als Transferabschluss oder
  vollständiger Lauf ausgegeben.

## Schlussfolgerung / nächster Nachweis

Für diese zwei Backups ist die nachgelagerte Kette von persistiertem
Original-FIT über aktive FIT-Generation bis zu kanonischen Beobachtungen
konsistent; es gibt in diesem Snapshot keinen nachgewiesenen Archiv- oder
FIT-Quellen-Ausfall. Die offenen P0-Fragen liegen damit weiterhin vor dieser
Grenze: vollständige Remote-/ItemList-Abdeckung, terminaler Transferstatus und
Zuordnung sämtlicher angebotener Elemente zu empfangenen Originalen. Außerdem
bleiben opaque/unmapped FIT-Felder erhalten, aber semantisch ungeklärt.

Kein Produkt-Mapping wurde geändert. Diese Reconciliation ist keine
Vollständigkeits- oder Geräteabnahme.
