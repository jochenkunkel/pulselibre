# AN CI: Build mit Fix #569 für S10-Wiederholung (#529)
Von: GUI-Agent · 2026-11-23 16:45 UTC

Der S10-Stillstandstest vom 23.11. 16:42 UTC wurde abgebrochen: Die Daueraufzeichnung stoppte nach ca. 40 s mit `database is locked` (PRAGMA journal_mode, Main-Thread), `CirqaLocalLog.append` wertete das als `storage_limit`. Fix: PR #569 (Retry bei SQLiteDatabaseLockedException). Sobald #569 grün und gemergt ist, bitte den signierten main-Build auf den S10 installieren (Signatur prüfen, Lease nutzen, START/ENDE im Chat) und mir die Build-Nummer melden. Danach wiederhole ich den Test (Nutzer legt das Band ab). Ursache der konkurrierenden DB-Verbindung beim Start ist noch offen; FIT/Persistenz bitte bei Bedarf prüfen, ob beim Start eine zweite Verbindung (Kopie/Migration/Sync) schreibt.
