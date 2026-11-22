# S10/CIRQA – Offline-Audit der Historien-Downloadkette (2026-11-22)

## Ergebnis in Kürze

Der aktuelle `main`-Code hat nach #432 einen verbundenen Pfad für Item-List, Pull,
MLR-Reassembly, Größen-/FIT-Prüfung, private Ablage und FIT-Import. Er ist aber
bewusst begrenzt und liefert keinen Beweis für alle verfügbaren Armbanddaten.
Insbesondere beendet der beobachtete S10-`next_transaction_id`-Pfad nach drei
unveränderten Folgeanfragen ausdrücklich unvollständig, solange kein terminaler
Abschluss belegt ist. Der geplante SAF-Datenbankexport enthält außerdem keine
Originaldateien aus `verified_fit/` oder `verified_raw/`; er kann daher allein
keinen dateibezogenen Vollständigkeitsvergleich leisten.

Der Zustand bleibt `coverage=device-history-unverified`. Dieser Audit ist
offline/code-only: kein S10-Zugriff, kein Export, keine BLE-Kommunikation.

## Codebefunde

1. **Item-List und Abschluss**
   - `CirqaFileAccessItemListParser.parsePage` gibt Einträge, Status,
     `session_id` und `next_transaction_id` zurück; ungültige Protobuf-Struktur
     wird verworfen.
   - `appendSyncItemListPage` verlangt `status=0`. Session-Fortsetzungen werden
     verfolgt; ohne Session wird ein angekündigtes separates
     `next_transaction_id` nach der beobachteten, festen Zahl von drei
     Folgeanfragen beendet. Bei weiter vorhandenem Token wird
     `syncIncompleteReason` gesetzt. Das ist ein sicherer Stopp, aber kein
     terminaler Nachweis der gesamten Historie.
   - Zusätzlich existiert ein Seitenlimit von 64 sowie ein Gesamtbudget von
     256 Dateien, 8 MiB pro Datei und 64 MiB pro Lauf. Bei Erreichen eines
     Limits meldet der Pfad Unvollständigkeit statt Erfolg.
   - Eine Antwort ohne `session_id` und ohne `next_transaction_id` wird als
     Listenende behandelt. Die S10-Evidenz belegt dieses Ende für die gesamte
     Gerätehistorie nicht.

2. **Datei-Pull und Verarbeitung**
   - Jede ausgewählte Referenz wird mit erwarteter Größe über den vorhandenen
     FileAccess-Pull geladen. #432 ergänzt MLR-Roh-/zlib-Reassembly und die
     Übergabe der vollständig zusammengesetzten Bytes an die Dateiverarbeitung.
   - Exakte Bytezahl wird gegen `data_size` geprüft. Gültige FIT-Dateien werden
     CRC-/Header-validiert, content-addressed privat gespeichert, dekodiert
     und in SQLite importiert. Unbekannte oder nicht-FIT-Rohdateien werden bei
     erfolgreicher Exact-Size-Prüfung privat gespeichert; bekannte
     Geräte-XML-Metadaten werden optional abgeleitet.
   - Erfolgreich empfangene, private nicht-FIT-Dateien sind als
     `VERIFIED` für den beobachteten Lauf zählbar. Das belegt weder, dass alle
     Typen gelistet wurden, noch dass jeder Typ semantisch dekodiert wird.
     FIT-Dateien mit ungültiger FIT-Prüfung werden nicht als erfolgreiche
     FIT-Imports gezählt.

3. **SAF-Export und Nachweisgrenze**
- `DatabaseBackup.write` exportiert Datenbanktabellen einschließlich
  abgeleiteter Geräte-Metadaten, Quellen und Provenienz. Es schreibt keine
  Dateien aus `files/verified_fit/` oder `files/verified_raw/` in das ZIP.
  `ExportActivity` stellt daneben getrennte, nutzerbestätigte ZIP-Exporte für
  FIT-Originale und verifizierte Rohdateien bereit (`FitOriginalsArchive` und
  `CirqaVerifiedRawArchive`).
- Der **Datenbankexport allein** kann daher einen DB-Importzuwachs belegen, aber
  nicht die erhaltenen Rohdatei-Bytes, Nicht-FIT-Dateien oder deren
  Vollständigkeit. Separate Originaldatei-Archive sind verfügbar; sie müssten
  ausdrücklich und zusätzlich zum DB-Export erstellt und dateibezogen geprüft
  werden, um Originalbytes nachzuweisen.
     Ein leerer Datenbankexport nach Fresh Install ist außerdem keine
     historische Band-Baseline.
   - Der Laufbericht enthält redigierte Zähler/Typen und begrenzte
     Dateiergebnisse; er ist kein dauerhaftes, exportiertes Manifest, das jede
     gelistete Referenz mit gespeicherter Datei und Integritätsbeleg verbindet.

## Konsequenz für den bereits autorisierten begrenzten S10-Lauf

- Nach manueller Gerätefreigabe zuerst den SAF-Datenbankexport als
  **Fresh-Install-Baseline** sichern; diese Baseline dokumentiert nur den
  Ausgangszustand der neuen App-Installation.
- Höchstens einen vorhandenen UI-Historien-Download bis fünf Minuten ausführen,
  ohne Retry oder neue Befehle. Vor Erreichen eines Limits oder bei
  unerwartetem Zustand stoppen.
- Den Nachher-Export ausschließlich als DB-/Importvergleich auswerten. Separat
  festhalten, dass Original-FIT-/Nicht-FIT-Bytes im Export nicht enthalten
  sind. Keine Aussage „vollständiger Download“ aus diesem Lauf ableiten.
- Weiterhin keine Instrumentationstests am Gerät, keine Band-Schreibaktionen,
  kein Reset/Entkoppeln, kein `CLOSE_ALL` und keine Datenlöschung.

## Nötiger nächster technischer Nachweis für echte Vollständigkeit

Für einen zukünftigen Vollständigkeitsanspruch braucht es mindestens:

1. einen protokollgestützten, terminalen Listenabschluss oder eine begründete
   Abdeckungsgrenze, die alle Transaktions-/Sessionseiten umfasst;
2. ein persistentes, datensparsames Manifest mit HMAC/Hash der lokalen
   Referenz (keine rohe UID), Typ, deklarierter Größe, empfangener Größe,
   SHA-256, Integritätsstatus, Speicherort und Import-/Nicht-FIT-Status;
3. einen verifizierbaren Abgleich `jede gelistete Referenz → genau ein
   erfolgreich gespeicherter Rohinhalt` einschließlich aller Nicht-FIT-Typen,
   Duplikate und übersprungener Dateien;
4. einen lokalen Export-/Restorepfad, der Originaldateien und Manifest
   gemeinsam oder separat mit Integritätsprüfung sichern kann;
5. Gerätebelege über mindestens einen vollständig abgeschlossenen Lauf, ohne
   ungeklärte Seiten, Budgets, Fehlversuche oder nicht unterstützte Typen.

Kein neuer Transaktionsmechanismus oder aktiver Protokollbefehl wird durch
diesen Audit freigegeben. Neue aktive Varianten benötigen eine eigene
begründete Freigabe und Stop-Kriterien.

## Ergebnis des begrenzten S10-Laufs

Der spätere Einmalversuch wurde vor Item-List und Datei-Pulls durch eine
abgelehnte GFDI-Registrierung beendet. Vorher-/Nachher-DB-Backups blieben leer;
es wurde kein Import und keine Vollständigkeit nachgewiesen. Die redigierte
Ausführungsevidenz steht in
[`CIRQA_S10_BOUNDED_DB_IMPORT_RESULT_2026-11-22.md`](CIRQA_S10_BOUNDED_DB_IMPORT_RESULT_2026-11-22.md).
`coverage=device-history-unverified` bleibt bestehen.
