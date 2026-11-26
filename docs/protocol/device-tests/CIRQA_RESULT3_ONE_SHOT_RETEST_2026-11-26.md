# CIRQA-Pull-Diagnose: `result=3` und 28 archivierte Originale (2026-11-26)

## Ergebnis in Kürze

- Der bisherige `0 von 38`-Befund bleibt separat: Der damals sichtbare erste Pull war `result=3, transport=0, handle=0`, danach stoppte der Lauf fail-closed. Seine CIRQA-Ursache ist weiterhin **nicht nachgewiesen**.
- Ein späterer kontrollierter manueller Lauf endete mit GATT 133 vor Item-Liste/Pull: `listResponses=0`, `pullRequestsQueued=0`, `incompleteReason=sync-not-accepted`. Dieser Lauf erklärt den alten Pull-Fehler nicht.
- In einem weiteren, genau einmal manuell bestätigten Lauf wurden im folgenden Backup **28 neue, vollständig gehashte Datei-Originale** im Testzeitfenster nachgewiesen: 26 FIT- und 2 Nicht-FIT-Referenzen, insgesamt 832.441 Bytes. Die App zeigte „28 von 29 Dateien“. Die 29. Datei und ein neuer terminaler Laufbericht sind nicht belegt; vollständige Gerätehistorie bleibt unbewiesen.
- Kein Transport-/Parserfix wurde abgeleitet oder vorgenommen.

## Test- und Backupgrenzen

Das S10 war für jeden Gerätezugriff in einer eigenen, gehaltenen `s10`-Lease. Vor dem bestätigten Teilabruf waren Build 2371 und Signaturkontinuität geprüft. Ein privates Backup v12 vor und nach dem Lauf wurde lokal vollständig verifiziert (ZIP-CRC, Manifest-Eintragsgrößen und SHA-256). Das Nachher-Backup wurde nicht in Git abgelegt.

Die zweite Beobachtung eines anderen manuellen Laufs hielt irrtümlich 140 statt der im Plan festgelegten 120 Sekunden; diese Abweichung ist unter #637 dokumentiert. Der spätere Einmalversuch verwendete strikt 120 Sekunden. In keiner Sitzung gab es einen Retry, Reset, Delete, Archive-, SetFlags- oder CLOSE_ALL-Befehl.

## Belegte Evidenz

1. **Vorherinventar im privaten Backup:** Backup v12 enthielt 16 Referenzen vom Typ `notification`, aber keine Datei-Referenzen, keine FIT-Quellen und keine kanonischen Beobachtungen.
2. **Einmalige manuelle Bestätigung:** Das lokale Sitzungsprotokoll markiert eine Bestätigung des History-Downloads. Der geprüfte Nachher-Status zeigte „Synchronisation unvollständig, 28 von 29 Dateien“. Ein neuer terminaler Bericht mit neuer `runId` ließ sich aus der UI-Hierarchie nicht sichern; die dortige Accessibility-Description enthielt noch den alten 0/38-Bericht.
3. **Dauerhaft gespeicherte Originalbytes:** Im folgenden privaten Backup wurden 28 gegenüber dem Vorherbestand neue Datei-Referenzen gefunden:
   - 20 `FIT_TYPE_4`, 4 `FIT_TYPE_5`, je 1 `FIT_TYPE_72` und `FIT_TYPE_1`;
   - je 1 Nicht-FIT `format_0` und `format_3`;
   - insgesamt 832.441 Bytes; die gespeicherten Empfangszeitstempel lagen zwischen 17:22:33 und 17:23:53 UTC, nach der Bestätigung um ca. 17:22 UTC.
4. **Byteintegrität:** Alle 28 vollständigen Streams wurden aus den Backup-Chunks zusammengesetzt. Länge, Chunk-Offets und SHA-256 stimmten; alle 28 Referenzblobs passten zu ihren Referenz-SHA-256-Werten. Es wurden keine Hashes, UIDs, Namen oder Payloads veröffentlicht.
5. **Abgleich mit Produktcode:** `CirqaMlHandshakeProbe.kt` archiviert Originalbytes erst nach Prüfung, dass Empfangslänge und erwartete Itemgröße exakt übereinstimmen. Die Archivierung erfolgt über `CirqaDatabase.archiveDownloadedOriginal(...)`; `DownloadedFileArchive.store(...)` legt SHA-256-geprüfte Chunks und Referenztyp ab. Die Offline-Migration `CirqaDatabase.archiveExistingDownloads()` verwendet dagegen Verzeichnislabels wie `verified_fit` und erklärt nicht die neuen Typen `FIT_TYPE_*`/`format_*`. Zusammen mit dem Empfangszeitfenster ist dies starke Evidenz, dass 28 vollständige Produkt-Dateiantworten dauerhaft archiviert wurden.
6. **Importprojektion getrennt vom Transport:** Das Nachher-Backup enthielt 26 kanonische FIT-Quellen, 50.363 kanonische Beobachtungen und 12.021 Unmapped-Field-Zeilen. Die App führt beim Aufbau der Hauptansicht zusätzlich Offline-Archivmigration, FIT-Recovery/Backfill und unbekannte-Feld-Inventur aus (`MainActivity.scheduleUnknownFitInventoryScan()`). Deshalb werden diese Zähler nicht ohne terminalen Bericht ausschließlich dem Bluetooth-Lauf zugeschrieben.

## Einordnung und offene Punkte

- **Bewiesen:** Nach einer manuellen Bestätigung lagen 28 neue vollständige Originalstreams unterschiedlicher FIT-/Nicht-FIT-Typen im lokalen Archiv; ihre Bytes sind im Backup integritätsgeprüft. Die UI zeigte 28/29.
- **Stark gestützt, aber nicht vollständig run-korreliert:** Die neuen Datei-Referenztypen und Empfangszeiten passen zum bestätigten Lauf. Der terminale Bericht dieses Laufs mit konkreter `runId`, Listengröße, Pullantwort der verbleibenden Datei und `runDrained`/`incompleteReason` fehlt.
- **Nicht bewiesen:** Ob die 29. Datei mit `result=3` abgewiesen, aus einem anderen Grund übersprungen/abgebrochen oder in einem unvollständigen Statuslauf noch ausstehend war; ob der Bestand alle historischen CIRQA-Daten abdeckt.
- **Unverändert offen:** Die CIRQA-spezifische Bedeutung/Ursache des früheren `result=3`. Die Connect-5.31-Deutung `NO_AVAILABLE_RESOURCES` bleibt nur eine externe Enum-Interpretation.
- **Öffentliche Protokollrecherche:** Die auffindbare Gadgetbridge-Dokumentation beschreibt andere `DataTransferService`-Statuswerte und `FileAccessItemResponse`-Operationen; sie liefert keine belastbare Definition des CIRQA-`PullItemDataResponse.result=3`. Sie wird daher nicht als Bestätigung der Connect-5.31-Deutung verwendet.
- **Sicherer nächster Schritt:** Die fehlende terminale Evidenz offline nach Möglichkeit aus dem aktuell gespeicherten App-Bericht gewinnen. Weitere manuelle Runs nur unter neuer Geräte-Lease, nach Status-/Backupprüfung und innerhalb der 120-s-/Datei-/Bytebudgets; ein unsicherer Skip-Nachweis darf einen vollständigen erneuten Pull nicht blockieren.

Issue #637: https://github.com/jochenkunkel/pulselibre/issues/637

## Lesende UI-Nachsuche nach dem Lauf

Nach Abschluss des Teilabrufs wurde versucht, den gespeicherten terminalen Bericht ohne erneute Sync-Aktion aus der App auszulesen. Bei der ersten Gelegenheit war die S10-Lease regulär durch den GUI-Agenten belegt; es erfolgte kein Zugriff. Nach Lease-Freigabe konnte der Statusblock auf der gerade sichtbaren Exportansicht nicht gefunden werden. Eine weitere geplante Navigation wurde vor UI-Aktion gestoppt, weil die installierte App nicht die erwartete Build-Version 2371 meldete. Es wurde weder neu installiert noch synchronisiert. Damit bleibt der fehlende neue Run-Bericht ungeklärt; die privat gesicherte Backup-Evidenz oben bleibt davon unberührt.
