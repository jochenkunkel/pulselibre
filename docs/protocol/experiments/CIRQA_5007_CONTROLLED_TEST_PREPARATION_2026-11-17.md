# CIRQA GFDI 5007 – Vorbereitung eines einmaligen Kompatibilitätstests

**Stand:** 2026-11-17 · **Status:** erster Versuch verbraucht; ein ausdrücklich freigegebener zweiter Einzelversuch wird vorbereitet · Issue #22
Dieses Dokument und der Byte-Builder erzeugen keine Bluetooth-Verbindung und senden nichts.

## Ausgangsevidenz

Der in Issue #22 übermittelte S25-Bericht nennt 78 BLE-Werbeereignisse, erfolgreichen Bond-Adressabgleich, erfolgreiche GATT-Dienstsuche, zwei Live-Messpakete (Puls/Schritte) sowie zehn Sekunden Android-GATT-Verbindung innerhalb einer 30-Sekunden-Beobachtung. Das belegt einen funktionierenden vorhandenen Live-/Garmin-ML-Pfad. Es belegt **keine** Legacy-FileSync-Unterstützung und keine GFDI-5007-Antwort.

Der vorhandene Diagnosecode registriert GFDI über den dynamischen ML-Handle und validiert empfangene COBS/GFDI-Frames inklusive CRC. Die bereits vorhandenen Zähler für 5007/5031/5002/5004 sind ausschließlich passiv. Es gibt keinen CIRQA-Trace mit einem gesendeten 5007-Request oder einer passenden Antwort.

## Garmin Connect 5.31 – nachgewiesener Request-Kandidat

Quelle ist die außerhalb des Repositories gehaltene Garmin-Connect-5.31-DEX/JADX-Analyse; proprietäre DEX/APK-Dateien sind nicht eingecheckt.

- `com.garmin.gfdi.file.FileManager.applyDirectoryFilter` (DEX continuation `defpackage.s7h`) ruft `messenger.b(5007, byteArrayOf(filter.rawValue), ...)` auf.
- `defpackage.zgd.PENDING_UPLOADS_ONLY.rawValue` ist genau `03`.
- `FileManager.applyDirectoryFilter` wertet die vom Messenger zurückgegebene **Antwortnutzlast** aus: mindestens ein Byte erforderlich; erstes Byte `00` bedeutet in Connect „unterstützt“, jeder andere Wert „nicht unterstützt“. Leere Nutzlast ist fehlerhaft.
- Connect-Callgraph: `DeviceManager.listFiles(filter=3)` → 5007 → gegebenenfalls 5031 → 5002 Directory-Index 0 → 5004. Die Filteranfrage liegt im Pending-Upload-/FULL_SYNC-Pfad und kann somit eine weitere Sync-Aktivität auslösen. Connects spätere Archivierung nach Upload ist eine separate, verbotene Geräteänderung.

Das sind Connect-Befunde, kein CIRQA-Kompatibilitätsbeweis.

## Vollständige offline berechnete Request-Kandidatenbytes

Die Länge des GFDI-Rahmens ist `6 + bodyLength`; Nachrichtentyp und Länge sind u16 Little Endian. CRC-16/ARC (Init 0, Polynom reflected `A001`, kein final XOR) deckt alle Bytes vor dem CRC ab und wird u16LE angehängt.

```text
Connect-Request-Body:       03
GFDI vor CRC:               07 00 8F 13 03
CRC-16/ARC:                 1AC9
CRC-Wirebytes (LE):         C9 1A
GFDI-Frame:                 07 00 8F 13 03 C9 1A
COBS inkl. 00-Delimitern:   00 02 07 06 8F 13 03 C9 1A 00
ML/GATT-Wert-Vorlage:       <dynamischer Handle> 00 02 07 06 8F 13 03 C9 1A 00
```

Beispiel ausschließlich zur Veranschaulichung mit Handle `1F` (nicht fest codieren):
`1F 00 02 07 06 8F 13 03 C9 1A 00` (11 Bytes Characteristic Value).

Offline-JVM-Tests erzeugen exakt diese Bytefolgen und prüfen CRC sowie den Connect-Status-Bytevertrag. Diese Tests beweisen weder, dass CIRQA diese GFDI-Version nutzt, noch dass der dortige Bytevertrag gleich ist.

## Nicht statisch fixierbare Laufzeitfelder

- Der vorhandene Garmin-ML-Dienst ist `6a4e2800-667b-11e3-949a-0800200c9a66`. Das bestehende Discovery-Verfahren wählt ein vollständiges RX/TX-Paar aus RX `6a4e2810..2814` und TX `6a4e2820..2824`. Der konkrete TX-UUID-Wert ist im Issue-Testbericht nicht enthalten.
- Der GFDI-ML-Handle wird bei jeder Registrierung dynamisch zugeteilt (belegte CIRQA-Tests hatten unterschiedliche Werte). Das Beispiel `1F` ist **kein** zulässiger fixer Requestwert.
- Der GATT-Write-Typ richtet sich aktuell nach den Properties der Laufzeit-TX-Characteristic. Es wird für die 5007-Vorbereitung keine MTU-Anfrage ergänzt. 11 Bytes passen in die Standard-ATT-Nutzlast bei MTU 23, aber ein Gerätetest muss den konkret gewählten Characteristic-Pfad und den Android-Write-Callback prüfen.

Damit ist die deterministische GFDI/COBS-Nutzlast vollständig rekonstruierbar, der endgültige sitzungsbezogene GATT-Characteristic-Value jedoch erst nach einer separat erlaubten Verbindung vollständig bestimmbar. Keine feste Adresse, UUID-Paarung oder Handle-Zahl darf aus früheren Sitzungen übernommen werden.

## Erwartete Antwort: Connect-Vertrag vs. CIRQA-Blocker

Connects Messenger liefert der Filtermethode eine Antwortnutzlast; das erste Byte ist der Filterstatus (`00` = unterstützt). Das erwartete CIRQA-GFDI-äußere Antwortformat, GFDI-Nachrichtentyp, mögliche zusätzliche Statushülle, Korrelationsregel und Timeout sind **nicht** durch einen CIRQA-Trace bestätigt. Auch eine synthetisch korrekte generische GFDI-5000-ACK-Antwort wäre nicht automatisch die 5007-Anwendungsantwort. `CirqaLegacy5007RequestCandidate` enthält daher absichtlich keinen Antwort-Encoder und keinen Bluetooth-Sender.

Der bestehende Live-Testpfad liefert jetzt einen begrenzten, redigierten TX-Bericht für bereits vorhandene ML-/GFDI-Schreibvorgänge (Typ/Handle/Länge/CRC/Write-API-Status, keine Payloads) und RX-Diagnosen bleiben bei Zählern und verifizierten Nachrichtentypen. Das schafft für einen späteren getrennt freigegebenen Versuch bessere lokale Diagnostik, ohne neue Befehle hinzuzufügen.

## Risiko- und Abbruchreview

**Unbekannte Nebenwirkungen sind nicht ausschließbar.** `PENDING_UPLOADS_ONLY` ist Teil eines Pending-Upload-/FULL_SYNC-Aufrufs. Eine erfolgreiche Antwort kann Connect zum nächsten Schritt veranlassen; CIRQA kann anders reagieren oder selbst weitere Frames senden. Ein sofortiges Disconnect beendet nur die lokale Sitzung und beweist nicht, dass kein geräteseitiger Zustand angestoßen wurde. Garmin Connect oder der laufende PulsLibre-Recorder könnten getrennt/gestört werden.

Falls ein späterer, ausdrücklich freigegebener Versuch nach vollständigem Review tatsächlich erfolgt, ist der vorgeschlagene Scope:

1. Nur das vom Nutzer konkret freigegebene Testgerät mit bestätigten synthetischen Daten; Garmin Connect und PulsLibre-Recorder vorher beenden.
2. Beim bereits vorhandenen Garmin-ML/GFDI-Registrierungsweg dynamischen Handle und tatsächlich ausgewählte TX-Characteristic frisch ermitteln; nicht verbinden oder schreiben, wenn Paar, Properties, Permissions oder Session-Gate nicht passen.
3. Genau **einen** Write mit obigem 5007-Kandidaten; kein Retry und kein 5024-/5050-Neuaushandeln außerhalb des bestehenden, separat freigegebenen Bootstrap-Ablaufs.
4. Höchstens fünf Sekunden auf eine einzelne, CRC-validierte und strukturell erwartete Antwortnutzlast warten. Kein Folge-5007, 5031, 5002, 5004, 5037, 0x8A39, `CLOSE_ALL`, SetFlags, Archive, Delete, Write, Reset oder Firmware-/Flagwechsel.
5. Bei GATT-Fehler, negativem Status, unerwartetem/mehrfachem Frame, Verbindungsverlust oder Timeout sofort Diagnose stoppen, GATT schließen und Sitzung freigeben. Keine Interpretation unbekannter Antwortbytes und keine Datei-/Payloadspeicherung.
6. Nur datensparsame Metadaten lokal berichten: ausgewähltes RX/TX-Paar, Write-Typ/Callback, Request-Typ/CRC-Status, Antworttyp/-länge/CRC-/Statusklasse, Zeit bis Antwort und Abbruchgrund. Keine Adresse, Rohframes, Dateinamen oder Gesundheitswerte exportieren.

Das Fünf-Sekunden-Limit ist ein konservativer Implementierungswert; Connects konkreter Timeout wurde aus dem analysierten Callgraph nicht gesichert. Der Scope ist kein Nachweis, dass ein 5007-Write risikofrei oder CIRQA-kompatibel ist. Nach Freigabe enthält die App einen eigenen Bestätigungsdialog und einen persistenten Gate mit maximal drei Testläufen über die Lebensdauer der Installation. Der bisherige Boolean wird als verbrauchter erster Versuch migriert; nach dem fehlgeschlagenen zweiten Lauf erlaubt die neue ausdrückliche Freigabe genau noch Lauf 3. Pro Lauf registriert der Sendepfad nur den GFDI-ML-Service, sendet höchstens einen 5007-Write, validiert ausschließlich eine passende CRC-geprüfte GFDI-5000-Antwort und beendet bei unerwarteten/malformed Daten oder Timeout. Keine Retry- oder Folgekommandos.

Der letzte Hardwarelauf scheiterte beim BLE-Verbindungsaufbau; Bluetooth meldete `CONNECTION_FAILED_ESTABLISHMENT (0x3e)`. `CirqaMlHandshakeProbe` nutzt bereits `BluetoothDevice.TRANSPORT_LE`, und es gab keine automatischen Wiederholungen. Das ist ein Link-Layer-/Geräteverbindungsfehler, kein belegter 5007-Protokollfehler. Die Testoberfläche speichert für künftige Läufe den GATT-Abbruchgrund sowie ausdrücklich, ob der 5007-Write vor dem Abbruch gesendet wurde. Keine Änderung am Request-Bytekandidaten und keine automatische Reconnect-Schleife.

## Ergebnis und Freigabeschwelle

- Offline-Kandidat + CRC/COBS/ML-Wrapper: **implementiert und synthetisch testbar**.
- Erfolgreicher Puls-/Schritte-Livepfad: aus Issue #22 übernommen; kein FileSync-Beweis.
- CIRQA-5007-Support, CIRQA-Responseformat, produktiver Timeout und Nebenwirkungsfreiheit: **unbewiesen/offen**.
- 0x8A39/FileSync-V2-Anfrage: vorhandener App-Einstieg ist fail-closed deaktiviert; nicht wiederholen.
- Der erste Versuch ist laut Nutzer verbraucht. Für genau einen weiteren 5007-Versuch liegt eine neue ausdrückliche Freigabe vor; die App wird dafür aktualisiert und die installierte signierte APK verwendet. Ergebnis und etwaiger Geräteversuch sind separat zu dokumentieren. Keine weitere Wiederholung oder FileSync-Stufe ist damit genehmigt.
