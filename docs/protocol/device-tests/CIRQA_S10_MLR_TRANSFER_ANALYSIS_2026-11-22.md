# S10 MLR-Transferprüfung (2026-11-22)

## Ergebnis

**Revidierter Befund nach Prüfung der Nicht-GFDI-Kanäle:** Aus der letzten
Verbindung lassen sich sechs zlib-komprimierte Transferkandidaten rekonstruieren:
fünf FIT-Dateien mit gültiger Header-/Datei-CRC und interner Größe, eine
Nicht-FIT-Datei. Vier FIT-Dateien und die Nicht-FIT-Datei stimmen außerdem mit
der UID-zugeordneten Item-List-Größe überein. Für die fünfte FIT-Datei fehlt die
UID-Zuordnung zur beobachteten Liste. Kein SQLite-Import oder vollständiger
Historienbestand nachgewiesen; `coverage=device-history-unverified`.

Die ursprüngliche Beschränkung der Analyse auf GFDI/COBS hatte die
zlib-Dateibytes auf den anderen MLR-Kanälen übersehen. Die folgenden früheren
Negativbefunde gelten daher nur für die untersuchten GFDI-/gzip-Nachrichten,
**nicht für den vollständigen Mitschnitt**.

## Offline-Prüfung

- Quelle: privater lokaler Mitschnitt `/tmp/opencode/private_s10_20261122/`;
  Rohdaten, Kennungen und Nutzdaten bleiben unveröffentlicht.
- HCI-Validator: 15.968 Records, 7.376 ACL-Records und 6.940 vollständige
  ATT-Kandidaten.
- Zeitachsenbasierte Auswertung der letzten CIRQA/FileAccess-Verbindung:
  sechs `PullItemDataResponse`-Ergebnisse mit `result=0` und sechs
  `SetFlags`-Ergebnisse mit `result=0`.
- Sequenzbewusste MLR-Reassemblierung ergibt CRC-valide GFDI-Streams mit
  FileAccess-Protobuf (`5043`/`5044`). **Korrektur nach Abgleich mit PR #413:**
  21 gzip-Streams in großen 5044-Frames dekomprimieren zu Wettervorhersage-JSON
  (u. a. Wetter-/Standortfelder), nicht zu FIT-Dateien. Damit sind
  gzip-komprimierte JSON-Anwendungsdaten belegt; eine Zuordnung dieser Daten zu
  einzelnen Item-Pulls oder FIT-Dateien ist es nicht.
- Weder in den GFDI-Frames noch nach gzip-Dekompression wurde eine FIT-Signatur
  gefunden. Dieser Suchraum enthält nicht die später rekonstruierten
  zlib-MLR-Dateibytes (siehe unten).
- Die beobachteten großen ATT-Notifications sind segmentierte MLR-Werte und
  für sich kein Beleg für FIT-Bytes. Eine frühe COBS-Routebyte-Hypothese wurde
  bei der kanal-/sequenzbewussten Reassemblierung nicht bestätigt.
- In der Aufzeichnung wurde kein Classic/RFCOMM-Datenstrom identifiziert.

## Konsequenz

`result=0` bleibt nur ein erfolgreicher FileAccess-Anwendungsstatus. Die
beobachteten gzip-JSON-Daten sind kein Nachweis für vollständige FIT-Bytes,
FIT-Integrität oder Import.
Aus den Wetterantworten allein folgt keine Produktabweichung.
Der nachfolgend rekonstruierte zlib-Transfer liefert dagegen eine eigenständige
Decodergrundlage. Ein aktiver Folgetest ist nicht Teil dieser Offline-Aufgabe
und benötigt gegebenenfalls eine gesonderte Freigabe.

`coverage=device-history-unverified` bleibt unverändert. Die lokale
Ergebniszusammenfassung und Rohaufzeichnung sind nicht für Git bestimmt.

## Feldpfad- und Richtungskorrelation (Offline-Folgeprüfung)

Die vollständigen Smart-Nachrichten wurden nach ML-Sequenz-, GFDI-Längen- und
CRC-Prüfung sowie Protobuf-Offset-/Gesamtgrößenprüfung rekonstruiert. Nicht nur
die Magic-Position im Frame, sondern die verschachtelten Length-Delimited-Felder
wurden untersucht. Der neue Befund:

| Merkmal | gzip-JSON | FileAccess-Pull |
|---|---|---|
| Smart-Servicefeld | **2** | **43** |
| gzip-Feldpfad | **2 → 6 → 3** (alle 21 Streams) | keiner dieser 21 Streams |
| GFDI-Typ / Richtung | 5044, **App → Band** (Direction 0) | 5043 App → Band; 5044 Band → App |
| Korrespondierende Anfrage | 5043 Band → App, Feldpfad **2 → 5** | Smart.43 → 1; Antwort Smart.43 → 2 |

- Alle 21 gzip-Felder ließen sich mit gzip-Ende und ohne nachlaufende Bytes
  dekomprimieren und als JSON parsen (705–8.353 Bytes). Keine JSON-Werte werden
  veröffentlicht.
- Jede dieser Smart-Nachrichten trägt ausschließlich das Top-Level-Feld 2,
  **kein Feld 43**. Gezählt nach internem ML-Kanal: 3 auf Kanal 3, 9 auf Kanal 4,
  6 auf Kanal 2 und 3 auf Kanal 7. Diese Kanalnummern sind verbindungsabhängig,
  keine festen Service-IDs.
- Zu jeder gzip-Nachricht existiert im selben ML-Kanal eine gegenläufige
  5043-Anfrage mit gleicher GFDI-Protobuf-Transport-ID innerhalb von zehn
  Sekunden. Deren Smart-Pfad ist 2 → 5. IDs bleiben privat; die Korrelation ist
  auf die ausgewertete Spur begrenzt, keine globale Eindeutigkeitsbehauptung.
- Die drei gzip-Nachrichten der letzten Verbindung liegen **vor der ersten
  FileAccess-Listenanfrage** dieser Verbindung (ca. 0,34–0,59 Sekunden vor ihr).
  Sie sind somit weder nachfolgende FIT-Bytes der sechs Pull-Antworten noch
  deren eingebettete FileAccess-Nutzlast.
- Die semantische Bezeichnung des Smart-Diensts 2 wird nicht allein aus
  Feldnummern geraten. Die Einordnung als Wetter-JSON folgt den JSON-Feldnamen,
  die Trennung von FileAccess dagegen aus Feldpfad und Richtung.

### Vergleich mit dem Produktparser

`CirqaFileAccessItemListParser.parsePage()` und `parsePullResult()` wählen
explizit Smart-Feld **43**, danach Service-Feld **10** bzw. **2**. Sie scannen
nicht rekursiv nach passenden Feldern oder gzip-Magic. Der Vergleich belegt
hier **keine Produktlücke**; eine gzip-Dekompression im FIT-Pull-Pfad wäre aus
diesen Wetterantworten nicht begründbar.

`CirqaS10GzipServiceIsolationTest` verwendet ausschließlich synthetische Daten:
gültiges gzip-JSON unter Smart.2 → 6 → 3 wird weder als Seite noch als Pull
interpretiert; pull-ähnliche Felder im falschen Dienst ergeben keinen Erfolg;
ein echter Smart.43-Geschwisterdienst wird in beiden Feldreihenfolgen weiterhin
korrekt gelesen. Die drei Tests und der bestehende S10-Transaktions-Replaytest
bestanden lokal. Dies ist Parser-Isolation, kein FIT-Transfer-/Import-E2E-Test.

## Durchbruch: zlib-Dateibytes auf separaten MLR-Kanälen

Eine zusätzliche Prüfung der Nicht-GFDI-Notifications in den sechs Pull-Fenstern
fand jeweils eine kurze Kontrollantwort und nachfolgende Datenwerte auf dem
zugehörigen Kandidatenkanal. Nach Entfernen der beiden beobachteten
Transportheaderbytes je ATT-Wert bilden die Daten einen **zlib-Stream**, keinen
gzip-Stream und keine COBS-GFDI-Nachricht.

- Alle sechs Kandidaten: zlib-Ende erreicht, kein nachlaufender Rest.
- Fünf Ergebnisse: FIT-Magic an Headeroffset 8, 14-Byte-Header,
  `Headergröße + Datenlänge + Datei-CRC == Dateigröße`, Header-CRC und
  Datei-CRC unabhängig mit CRC-16/ARC gültig.
- Vier dieser FIT-Dateien und die eine Nicht-FIT-Datei: UID der vorhergehenden
  Pull-Anfrage exakt mit dem beobachteten Item-List-Eintrag verglichen; dessen
  `data_size` entspricht der dekomprimierten Länge.
- Die letzte FIT-Datei besteht interne Größe und beide CRCs, aber ihre Pull-UID
  wurde in den rekonstruierten Listenseiten nicht gefunden. Für sie bleibt die
  unabhängige erwartete Größe offen.
- Die fünf FIT-Dateien enthalten zusammen 19.810 Bytes. Die Nicht-FIT-Datei hat
  keine FIT-Signatur und wird nicht als FIT klassifiziert oder importiert.
- Rekonstruierte Dateien und vollständige Analyseskripte bleiben ausschließlich
  im privaten lokalen Verzeichnis; keine Rohbytes, UIDs oder Messwerte in Git.

### Minimaler Decoderbaustein statt ungeprüfter Live-Umschaltung

`CirqaZlibFileDecoder` dekodiert ausschließlich einen bereits vollständig
reassemblierten zlib-Bytestrom mit expliziter erwarteter Größe. Er begrenzt
Ein- und Ausgabe auf 16 MiB und verwirft beschädigte/trunkierte Streams,
Wörterbuchanforderungen, Überschreitung/Unterschreitung der erwarteten Größe
und nachlaufende oder verkettete Streams. gzip/Raw-Deflate werden nicht
stillschweigend akzeptiert. Format-/FIT-CRC-Prüfung bleibt separat.

Die fünf synthetischen Decodertests einschließlich eines CRC-validen leeren
FIT-Fixtures bestanden lokal unter JDK 21. Keine realen FIT-Dateien im Test.
Der Baustein ist **noch nicht im Live-Pfad verdrahtet**. Insbesondere sind
zuverlässiges ML-Routing, Kontrollantwort/Header, Sequenz-/ACK-Regeln und
Kompressionsaushandlung vor einer Live-Integration zu prüfen. Die vorhandene
unmarkierte/raw Produktübertragung wird nicht blind umgestellt.

**Nächste technische Lücke:** den beobachteten zlib-Transfer mit einem expliziten
Transportzustandsmodell und den bestehenden FIT-/Commit-Stufen verbinden.
Offline-FIT-Rekonstruktion ist jetzt belegt; Produktdownload, Import und
vollständige Historie bleiben getrennte, noch offene Nachweise.
