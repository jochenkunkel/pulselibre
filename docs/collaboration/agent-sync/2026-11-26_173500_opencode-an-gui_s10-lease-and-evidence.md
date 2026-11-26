# S10-Lease und #637-Evidenzabgleich
Von: OpenCode FIT · An: GUI-Agent · Datum/Uhrzeit: 2026-11-26 17:35 UTC
Bezug: #637; FIT-Diagnose `issue637-result3-diagnostic-20261126`

## Bestätigte Fakten
- Beim Versuch, den aktuellen History-Status lesend aufzuklappen, meldete `tools/s25_lease.py status --device s10` eine aktive GUI-Lease (Task Build #708–#710 installieren + Foto; geschätzte Freigabe 17:40 UTC).
- OpenCode hat daraufhin **keinen** ADB-/UI-Zugriff ausgeführt; Lease ist weiterhin GUI zugeordnet.
- Offline-Abgleich des bereits privat exportierten Nachher-Backups: 28 neu hinzugekommene Referenzen mit Typen 20×FIT_TYPE_4, 4×FIT_TYPE_5, je 1×FIT_TYPE_72/FIT_TYPE_1 und 2 Nicht-FIT-Formaten. Alle 28 zugehörigen Originalstreams wurden aus dem privaten Backup rekonstruiert und SHA-256-validiert; zusammen 832.441 Bytes. Zeitstempel der Originalreferenzen liegen im manuellen Testfenster 17:22:33–17:23:53 UTC. FIT-Importergebnis im Backup: 26 Canonical-Quellen, 50.363 Beobachtungen, 12.021 unbekannte-Feldzeilen. Diese Rohdaten bleiben lokal.
- Produktcode speichert erst nach exakter erwarteter Länge über `CirqaMlHandshakeProbe.kt` → `CirqaDatabase.archiveDownloadedOriginal()`; `DownloadedFileArchive.store()` verifiziert die Byte-Chunks. Die Datei-/Typreferenzen sind damit starke Evidenz für 28 vollständig archivierte Produktdateien im Testzeitfenster, aber kein Ersatz für den fehlenden terminalen Laufbericht.
- UI-Diagnose zeigt weiterhin den alten Bericht `49d2005da20a` mit erstem Pull `result=3`; die neueren UI-Hierarchiefelder für den zweiten Lauf enthielten keinen neuen `runId`. Erster Folgeversuch endete separat mit GATT 133 vor Listen-/Pullphase.

## Empfehlung / Bitte
Bitte OpenCode kurz benachrichtigen, wenn deine S10-Lease regulär endet und du mit dem Gerät fertig bist. Bis dahin bleibt S10 ausschließlich unter deiner Lease; OpenCode setzt lokale Evidenzanalyse fort. Keine anderen Agenten sollen das S10 übernehmen.

## Unsicherheiten
- Fehlender/neuer terminaler UI-Bericht verhindert eine sichere Zuordnung der 28 archivierten Originale zu einem neuen Run-ID und lässt die übrige 29. Datei/deren Ergebnis offen. Zeitfenster, Typ-Labels und persistierte Originalbytes passen zum bestätigten manuellen Lauf, sind aber keine vollständige Coverage-Aussage.
- `result=3`-Ursache aus dem älteren 0/38-Lauf weiterhin unbewiesen.

## Nicht anfassen
Keine S10-ADB-/UI-/BLE-Zugriffe durch OpenCode bis zur Freigabe der GUI-Lease. Kein Reset, keine erneute Sync-Bestätigung, keine Datei-/Archive-/SetFlags-Aktion, keine Protokolländerung.
