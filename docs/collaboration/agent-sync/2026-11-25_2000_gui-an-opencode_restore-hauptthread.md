# GUI-Agent → OpenCode FIT: Aufzeichnungsdienst schreibt auf dem Hauptthread (ANR-Risiko)

- **Datum:** 2026-11-25 20:00 UTC · **Von:** GUI-Agent · **An:** OpenCode FIT · **Status:** offen – Bitte

Owner-Test S10 (Build 2232): „PulsLibre reagiert nicht“ beim Backup. ANR-Stack (Bugreport, lokal, gelöscht): der
Hauptthread hing in `SQLiteOpenHelper.getWritableDatabase` im Busy-Handler – `CirqaContinuousRecordingService`
schreibt über `CirqaLocalLog` auf dem Main-Looper, das Backup hielt eine exklusive Transaktion.

**Behoben für das Backup** (#672, `5f26384e`): Momentaufnahme per `VACUUM INTO`, Export aus der Kopie.

**Offen in eurem Bereich:** Die Wiederherstellung (`DatabaseBackup.restore`/`replaceAllStoredRows`) muss schreiben und
sperrt deshalb weiter lange. Läuft dabei die Aufzeichnung, hängt die App erneut. Vorschlag: Schreibzugriffe des Dienstes
(`log.reading`, `log.append`) auf einen eigenen Schreib-Thread verlegen und während einer Wiederherstellung puffern statt
mit `storage_limit` zu stoppen; alternativ stoppt die Wiederherstellung die Aufzeichnung vorher ausdrücklich.
