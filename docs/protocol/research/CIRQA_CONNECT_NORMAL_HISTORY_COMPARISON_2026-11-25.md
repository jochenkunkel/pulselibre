# CIRQA / Garmin Connect: normale Historie und Transaktionsfortsetzung (2026-11-25)

## Ergebnis in Kürze

**Kein belegter Beweis, dass PulsLibre ausschließlich Aktivitäten anfordert oder
Connect andere normale Messwertkategorien erfolgreich synchronisiert.** Der
Kontrolltest vom 24.11. belegt eine neue Aktivitätsaufzeichnung im PulsLibre-CSV,
aber nicht die Vollständigkeit der Tageshistorie. Frühere Connect-Spuren belegen
hingegen mindestens einen übertragenen FIT-Inhalt mit Daily-History-artigen
Nachrichten (Global Messages 397/398), nicht nur Standard-Aktivitätsnachrichten.
Der Inhalt ist nicht mit dem CSV-Zuwachs gleichzusetzen.

Die am 25.11. gestartete Connect-App wurde nicht explizit zum Sync angetippt.
Im c17-Mitschnitt ließ sich dennoch ein vollständiger FIT-Dateiinhalt aus
einem eingehenden MLR-Kanal rekonstruieren. Er ist eine bereits vor dem Lauf
archivierte Aktivitätsdatei mit passendem Aktivitätszeitfenster. Die vollständige
FileAccess-/Protobuf-Request-Korrelation fehlt weiterhin; insbesondere ist
weder ein vollständiger History-Sync noch eine Anforderung normaler
Messwertkategorien nachgewiesen.

Für PulsLibre c16 ist `next_transaction_id` jetzt konkret feldweise geprüft:
Die initiale ItemList-Antwort hat `status=0`, 38 Einträge, kein
`session_id`-Feld und ein `next_transaction_id`-Feld. Drei passende
Folgeanfragen (IDs 41–43) senden unverändert diesen Token; alle drei
vollständigen Antworten haben `status=0`, null Einträge, kein
`session_id`-Feld und geben denselben Token zurück. Das ist eine **vollständige
leere Antwort**, nicht eine fehlende, verworfene oder unvollständige Antwort.
Es beweist nicht, ob das Gerät bewusst keine weiteren Einträge hatte oder ob
Filter-/Transaktionssemantik weitere Einträge ausschloss.

## Evidenzklassen und Trennung der Datenbereiche

| Bereich | Beobachtung | Aussage / Grenze |
| --- | --- | --- |
| **Action / Aktivitätsaufzeichnung** | Der Kontrolltest #637 vom 24.11. fand im Vorher/Nachher-CSV exakt 680 neue `cirqa-fit`-Beobachtungen: 340 HR + 340 Distanz, über 340 Sekunden. | **Bewiesen:** die aufgezeichnete Aktivität wurde nach dem PulsLibre-Lauf sichtbar. **Nicht bewiesen:** dass alle geladenen FIT-Dateien Aktivitäten waren oder dass Tageshistorie fehlte. |
| **Normale historische Messwerte** | Im privaten Connect-Capture vom 24.11. wurde ein CRC-valider FIT-Transfer mit Global Message 398 (`skin_temp_overnight`) und vielen Global-397-Records rekonstruiert; keine Standard-Activity-`record`/`session`/`activity`-Nachrichten. | **Bewiesen, capture-begrenzt:** Connect übertrug mindestens einen nicht als Standardaktivität erkennbaren Daily-History-Inhalt. Global 397 ist in diesem Befund nicht weiter semantisch aufgelöst. Kein Beleg für Vollständigkeit oder für den 680-Zeilen-CSV-Zuwachs. |
| **Live-Messwerte** | In den ausgewerteten Mitschnitten wurde keine belastbare Live-Messwertsequenz dieser Kategorien isoliert. | **Nicht nachgewiesen.** Nicht aus FIT-History, GATT-Verkehr oder App-Anzeige ableiten. |
| **Metadaten / Dateilisten** | Vorhandene Connect-Spuren enthalten FileAccess-ItemLists und Datei-Pulls; die 24.11.-Liste hat 39 Einträge. Im 25.11.-PulsLibre-Run liegen außerdem FIT-Monitoring-Records im lokalen Nachher-Backup. | Dateiliste und FIT-Inhalt sind keine eigenständigen Messwert-Sync-Kategorien. Die Monitoring-Records aus dem lokalen Backup sind nicht Connect zuzuordnen. |

### CSV-Delta des Kontrolltests

Gemäß Issue #637: 340 Sekunden mit jeweils einer HR- und einer Distanzzeile,
Messzeit `2026-11-24T20:16:45Z` bis `20:22:24Z`, gemeinsame Empfangszeit
`20:24:27.083Z`. Diese nachgelagerten CSV-Beobachtungen belegen importierte
Aktivitätsdaten. Ein CSV-Delta ist kein Inventar der empfangenen FIT-Originale.

### Separater FIT-/SQLite-Befund im privaten PulsLibre-Nachher-Backup

Das lokale Nachher-Backup des 25.11.-Runs enthält 45 FIT-Quellen und 559
kanonische HR-Beobachtungen aus FIT Global Message 55, Feld 27, verteilt über
zwei Quellen; vier Zeitstempel liegen im Aktivitätsfenster vom 24.11. Diese
Records sind ein eigenständiger lokaler FIT-/SQLite-Befund. Sie sind **nicht**
als Connect-Transfer, als Teil des 680-Zeilen-CSV-Deltas oder als Resultat
normaler Tageshistorie aus einem bestimmten Pull belegt. Im selben Backup liegen
Stress-/SpO₂-/Respirationswerte teilweise als Review-Kandidaten, nicht als
kanonische Beobachtungen; auch dies ist kein Beweis für Connect-Anforderungen.

### 13.039-Byte Daily-History-artiger FIT: Pipeline-Ort

Der private Connect-Rekonstruktionsbericht vom 24.11. dokumentiert eine CRC-valide
13.039-Byte-FIT-Datei, deren rekonstruierte Bytes lokal bytegenau mit einem der
45 privaten FIT-Originale verglichen wurden (Digest nicht veröffentlicht).
Eine getrennte read-only Prüfung eines lokalen 45-Quellen-Backups (Backup v9,
Zeitstempel `2026-11-24T10:25Z`) fand genau eine 13.039-Byte-Quelle mit derselben
charakteristischen FIT-Struktur: 1.405 beobachtete Global-397-Nachrichten und
eine Global-398-Nachricht mit fünf Feldern. Das Backup liegt zeitlich vor dem
Connect-Capture (dessen erster HCI-Zeitstempel 20:48Z ist), was mit einem
bereits lokal vorhandenen Original vereinbar ist. Die genaue Zuordnung dieses
Backup-Eintrags zum im Capture bytegleich gefundenen Original wurde in diesem
Folgeschritt nicht erneut per Digest geprüft. Keine Quelldigests oder Rohfeldwerte
werden veröffentlicht.

Für den strukturell passenden Kandidaten enthält der geprüfte Bestand **keine kanonischen
Beobachtungen und keine FIT-Review-Kandidaten**. Das Unknown-Field-Inventar
verzeichnet ein Feld für Global 397 (in 1.405 Nachrichten beobachtet) und fünf
Felder für Global 398 (in einer Nachricht); keine der beiden Global Messages
lieferte kanonische Werte. Das passt zum FIT-Befund ohne Standard-Activity-
`record`/`session`/`activity`-Nachrichten.

Ein gezielter, nur temporärer JVM-Audit mit dem aktuellen `FitHistoricalObservations`
auf dem lokal hashverifizierten Backup-Kandidaten bestätigte: **0 kanonische
Beobachtungen**, **0 Review-Kandidaten**, 2.862 dekodierte Feldvorkommen;
darunter 2.810 rohe Feldvorkommen aus Global 397 und 6 aus Global 398.
Die Unknown-Field-Zählung ergab 1.405 Vorkommen für Global 397 und fünf Felder
für Global 398. Damit verwirft der aktuelle Decoder diese Inhalte nicht aus dem
FIT-Parsergebnis: er liefert die Feldbytes zur verlustfreien Feldpersistenz,
aber ordnet ihnen keine kanonische Messwertsemantik zu. Der bestehende
`FitObservationCommitter` schreibt `decodedFields` in `fit_generation_fields`;
ein candidate-spezifischer SQLite-Reimport wurde in diesem Audit nicht ausgeführt.

### Abgleich mit dem offiziellen FIT-Profil 21.222.0

Das Garmin-Release `fit-sdk-tools` **21.222.0** (17.11.2026) enthält das
offizielle `Profile.xlsx`; die Prüfung erfolgte lokal auf dieser Version,
passend zu `FitHistoricalObservations.PROFILE_VERSION`. Global 398 ist dort als
`skin_temp_overnight` definiert: Feld 253 `timestamp`, Feld 0 `local_timestamp`,
Feld 1 `average_deviation` und Feld 2 `average_7_day_deviation` (beide
ausdrücklich in °C), Feld 4 `nightly_value` („Final overnight temperature
value“). Feld 3 des Kandidaten ist im Profil nicht definiert. Damit ist die
Semantik von Teilen der Nachricht profilbelegt, aber weder die lokale Feld-3-
Erweiterung noch das konkrete Kandidatenfeld 4 samt Einheit/Sentinel-Regel
vollständig geklärt. Der Produktdecoder erzeugt dafür weiterhin absichtlich
keine kanonischen Hauttemperatur-Beobachtungen.

Global 397 ist im selben offiziellen Profil **nicht als Message definiert**.
Die im Kandidaten wiederholte float32-Feldstruktur bleibt daher opaque; die
frühere Temperatur-Deutung ist nicht bestätigt. Keine Werteverteilung,
Skalierung oder Einheit wurde aus privaten Rohwerten abgeleitet. Es gibt
weiterhin keinen belegten Anlass, 397/398 in eine bestehende Herzfrequenz-,
Distanz- oder Speed-Metrik umzudeuten oder ein Produkt-Mapping zu ergänzen.
Profilquelle: [Garmin FIT SDK Tools, Release 21.222.0](https://github.com/garmin/fit-sdk-tools/releases/tag/21.222.0),
Asset `Profile.xlsx`; ergänzende Message-Übersicht:
[Skin temperature overnight](https://selfsync.c100k.eu/fit-file-format/skin-temp-overnight)
(sekundäre Referenz, nicht CIRQA-spezifisch).

Eine ergänzende **synthetische** Android-Instrumentierungsregression speist
Nachrichten 397/398 durch den bestehenden Backfill in eine isolierte SQLite:
acht FIT-Rohfeldzeilen werden gespeichert, während kanonische Beobachtungen,
Generationsbeobachtungen, Provenienzzeilen und Unified-/CSV-Exportzeilen bei
null bleiben. Das prüft den Codepfad für diese Feldform, nicht den privaten
Kandidaten selbst und nicht dessen exakte Capture-/Item-Zuordnung.

**Pipeline-Folge:** Für diesen Kandidaten ist ein fehlender CSV-Messwert kein Beleg
für einen fehlgeschlagenen BLE-Transfer oder einen alleinigen CSV-Fehler. Die
Bytes sind archiviert, FIT-dekodiert und unbekannte Feldbytes können in der
aktuellen Generation persistiert werden; die verbleibende Lücke liegt bei der
FIT-Feldinterpretation bzw. Erzeugung kanonischer Beobachtungen. Ohne
kanonische Beobachtungen hat die abgeleitete Unified-/Export-Schicht keine
solchen Werte auszugeben. Das beweist weder die Zuordnung des Transfers zu
einem bestimmten Connect-ItemList-Eintrag noch, dass alle Felder aus Global
397/398 Gesundheitsmessungen mit belegter Semantik und Einheit sind. Vor einer
Kanonisierung müssen Feldidentität, Profilversion, Skalierung, Zeitstempel und
Sentinel-Verhalten validiert werden.

## Konkrete `next_transaction_id`-Antworten

Aus dem privaten c16-HCI-Replay; Smart-Protobufs und äußere GFDI-Frames wurden
vollständig reassembliert und die Request-ID-Korrelation geprüft. Zeitbasis:
HCI-Quellkonvertierung, absolut nicht unabhängig als UTC validiert.

| Anfrage | ItemList-Antwort | Vollständig? | `status` | Items | `session_id` | `next_transaction_id` |
| --- | --- | --- | --- | ---: | --- | --- |
| initiale Request-ID 2 | 4 Antwortfragmente, 1.520 Smart-Bytes | ja | 0 | 38 | Feld fehlt | vorhanden |
| Folge-ID 41 | 1 Antwortfragment, 10 Smart-Bytes | ja | 0 | 0 | Feld fehlt | vorhanden, entspricht initialem Token |
| Folge-ID 42 | 1 Antwortfragment, 10 Smart-Bytes | ja | 0 | 0 | Feld fehlt | vorhanden, entspricht initialem Token |
| Folge-ID 43 | 1 Antwortfragment, 10 Smart-Bytes | ja | 0 | 0 | Feld fehlt | vorhanden, entspricht initialem Token |

Alle drei Folgeanfragen tragen denselben initialen Token. Damit ist die
Transportantwort jeweils **empfangen und vollständig, semantisch aber leer**.
Das wiederholte Token allein erklärt nicht, ob die Transaktion terminal sein
soll; die lokale Fortsetzungspolitik begrenzt Wiederholungen und behauptet
ausdrücklich keine vollständige Bandhistorie.

## Decoder-Nachtrag nach dem S10-Lauf (2026-11-25)

Im S10-Vorher-/Nachher-Backup blieben FIT-Quellen (37), unbekannte-Feld-
Inventareinträge (12.473), kanonische FIT-Beobachtungen (29.123) und
FIT-Review-Kandidaten (2.635) unverändert. Der Lauf fügte daher keine neue FIT-
Quelle und keine neuen FIT-Feldsignaturen hinzu. Die +239 verifizierten
Archiv-Originale stammen aus dem separaten empfangenen Roh-/Notification-Archiv;
sie sind kein Nachweis zusätzlicher FIT-Dateien.

Der Produktdecoder klassifizierte Global 398 vorher als vollständig unbekannte
Message, obwohl Profil 21.222.0 F1/F2 als `average_deviation` und
`average_7_day_deviation` in °C definiert. Der Decoder erzeugt jetzt für genau
diese zwei korrekt typisierten endlichen float32-Felder
`UNTRUSTED_PROFILE_CANDIDATE`s (Review-Decoder-Version 4), weiterhin **keine**
kanonischen Beobachtungen. FIT-Sentinel/NaN/Inf werden nicht als Kandidaten
ausgegeben; die exakten Feldbytes bleiben in `decodedFields` erhalten. Global
398/F0, F1, F2 und F4 werden als profilbekannt eingestuft; F3 bleibt unbekannt.
F4 wird mangels abgesicherter Kandidaten-Unit/Sentinel-Regel nicht semantisch
ausgegeben. Global 397 bleibt vollständig opaque, weil es in Profil 21.222.0
keine definierte Message ist.

Ein lokaler Replay des Post-Run-Backups mit dem geänderten Decoder prüfte alle
37 hashverifizierten FIT-Originale ohne Decode-Fehler. Er fand zwei neue
Review-Kandidaten insgesamt (je einer für F1 und F2), 29.123 kanonische
Beobachtungen unverändert, 2.842 rohe Global-397-Feldvorkommen über 1.421
Nachrichten sowie sechs rohe Global-398-Feldvorkommen; davon bleibt F3 eine
opaque Unmapped-Signatur. Der Replay schreibt keine Datenbank und gibt keine
privaten Feldwerte oder Quellhashes aus. Das Gerätesync-/Backup selbst wurde
nicht mit diesem Codebuild wiederholt.

Zum Sync-Abschluss: Der aktuelle Lauf erhielt vier vollständige ItemList-
Antworten (27 Einträge, danach drei erfolgreiche, leere Folgeantworten mit
wiederkehrendem Transaktionskontext). Alle 27 gelisteten Dateien wurden
transferiert/verifiziert/privat gespeichert; 26 wurden FIT-dekodiert und
SQLite-committed (19 mit importierbaren Werten, 7 ohne solche Werte), eine
Datei war non-FIT. Das entspricht dem bereits unabhängig beobachteten
Connect-Muster. Ohne `session_id` gibt es keinen weiteren Seiten-Cursor; der
wiederkehrende `next_transaction_id`-Wert und die leeren Antworten beweisen
weder, dass weitere Dateien existieren, noch dass keine existieren. Deshalb
bleibt der Lauf korrekt `runDrained=false`/`device-history-unverified`; eine
automatische oder manuelle Wiederholung bzw. unbegrenztes Chaining wäre kein
belegter Fix und könnte den CIRQA unnötig belasten.
Der Terminalbericht wird im Produktcode zusätzlich künftig die Item-Anzahlen
aller begrenzten Transaktions-Folgeantworten als redigierte Liste ausgeben
(z. B. `[0,0,0]`); zuvor war nur die letzte Antwort sichtbar. So lässt sich
ein leerer Abschluss von einer davor nichtleeren Antwort unterscheiden, ohne
Tokens oder Rohdaten offenzulegen.

Das ältere Connect-Capture vom 24.11. zeigt eine andere Stichprobe: eine
ItemList-Antwort mit 39 Einträgen, ohne Session-Cursor und mit Transaktionstoken;
die frühere begrenzte Filter-Auditspur sah mehrere ItemList-Requestformen mit
zusätzlichen Feldern (u. a. `exclude_all_flags`, `get_flags`,
`included_data_types`). Das ist eine **Wire-Form-Abweichung**, keine belegte
Ursache leerer Antworten. Filterwerte nicht übernehmen oder erraten: Connect
bezieht sie aus Agent-spezifischer Konfiguration, während PulsLibre ein
ungefiltertes vollständiges Inventar anstrebt.

## Connect-App-Start am 25.11. und Sequenzvergleich

Der vorhandene Connect-Bugreport, Screenshots und BTSnoop wurden offline
chronologisch verglichen. Der Start der App löste eine automatische GATT-
Verbindung aus. Der Screenshot dokumentiert keinen expliziten Sync-Tap; deshalb
wird diese Interaktion nicht als manuell ausgelöster vollständiger Sync
bezeichnet.

Im ausgewählten c17-Zeitraum:

- HCI-Replay zählt 649 Host-Write-Commands und 892 Notifications in der
  Verbindung; ATT-Value-Handles sind 0x0015 (Write) und 0x0012 (Notification).
- Die c17-UUIDs sind im betreffenden Discovery-Abschnitt nicht direkt
  aufgelöst. Die Werte `0x0012` (Notification) und `0x0015` (Write) stimmen
  jedoch mit den ATT-Handles aus zwei früheren Verbindungen desselben Peers
  überein, in denen die Charakteristiken direkt als Garmin-ML RX
  `6a4e2810` und TX `6a4e2820` entdeckt wurden. Die c17-Zuordnung ist damit
  stark abgeleitet, nicht in c17 selbst entdeckt.
- Ein erneuter struktureller Replay ordnet in c17 29 13-Byte-Hostnachrichten
  dem beobachteten Connect-Client-ID-1-Registrierungspräfix zu. Dazu lassen sich
  in Capture-Reihenfolge 29 16-Byte-Antworten nach Service-ID paaren; alle
  Antwortstatusbytes sind 0, beteiligt sind 16 verschiedene Service-IDs. Das
  ist eine sequenzielle Service-ID-Korrelation, keine GFDI-Request-ID-Korrelation.
  Das Antwortbyte an der Handle-Kandidatenposition liegt 21-mal im Bereich
  `0x80..0x87` und 8-mal im Bereich `1..127`. Die gemischte Form ist
  semantisch ungeklärt; daraus wird kein zugewiesener ML-Handle normalisiert
  oder einem GFDI-Datenkanal zugeordnet. Die Statuswerte allein belegen keine
  nachgelagerte GFDI-Semantik und keinen History-Request. Diese Nachrichten
  werden nicht als Datei- oder Messdatentransfer interpretiert.
- Ein eingehender MLR-Datenkanal (nach Routing-Flag-Normalisierung Route 4)
  lieferte nach Abtrennung der ersten, 5 Byte langen Konfigurationsantwort und
  eines ein Byte langen Transportpräfixes je weiterer ATT-Notification einen
  vollständigen zlib-Stream: 4.012 komprimierte Bytes, 12.981 dekomprimierte
  Bytes, EOF. FIT-Signatur, Header-CRC und Datei-CRC sind gültig.
  Ein erneuter datensparsamer ATT-Replay desselben privaten Captures zählt
  49 Notifications auf normalisierter Route 4. Nach derselben dokumentierten
  Konfigurations-/Präfixbehandlung ergeben sich 6.498 Bytes an Route-4-Material:
  der zlib-Decoder konsumiert genau den 4.012-Byte-Stream bis EOF; 2.486
  nachlaufende Bytes bleiben außerhalb dieses Streams. Sie sind unter derselben
  Framingannahme kein zweiter vollständiger zlib-Stream und werden keinem
  weiteren FIT oder einer bestimmten Protokollfunktion zugeschrieben. Das
  qualifiziert den vollständigen FIT-Stream, ohne die übrigen Bytes als
  vollständig analysiert zu behandeln.
  Der FIT-Inhalt enthält je einen Session- und Lap-Record sowie 340
  Global-Message-20-Records; deren 340 Zeitstempel reichen exakt von
  `2026-11-24T20:16:45Z` bis `20:22:24Z`. Dieser Bereich entspricht dem
  separaten 340-Sekunden-Aktivitätsdelta im Kontrolltest. Der rekonstruierte
  vollständige FIT-Byteinhalt stimmt bytegenau mit einer bereits im Vorher-
  Backup vorhandenen, hashverifizierten Archivdatei überein (Hash nicht
  veröffentlicht). Dieselbe Quelle ist im Vorher-Backup auch als kanonische
  FIT-Quelle registriert; ihr sind 680 Beobachtungen/Provenienzzeilen zugeordnet
  (340 Herzfrequenz, 340 Distanz). Damit sind für diese Aktivitätsdatei
  FIT-Transferinhalt, byteidentische lokale Archivquelle und SQLite-Persistenz
  nachgewiesen. Connect hat hier keinen neuen lokalen Datenbestand geliefert.
  Das ist kein Nachweis normaler Tageshistorie. Die
  FileAccess-Request-ID/ItemList-Korrelation für diesen c17-Inhalt bleibt offen.
- Ein erster Rohwertversuch mit falscher Handle-/Framing-Annahme lieferte keine
  verwertbare Dekodierung und wird **nicht** als Protokollergebnis verwendet.
  Die korrigierte ATT-/Handle-Korrelation zeigt umfangreichen ML-Verkehr;
  einzelne FIT-Dateibytes sind nun valide reassembliert. Eine vollständige
  GFDI-5043/5044-Request-/ACK-/FileAccess-Korrelation mit Request-IDs und
  Item-Index ließ sich weiterhin nicht belastbar herstellen. Keine Aussage zu
  normalen Messwertanforderungen wird aus Paketgrößen/Fragmentierung abgeleitet.
- Der BTSnoop-Parser meldet überlappende GATT-Servicebereiche und nicht
  unabhängig verifizierte absolute Zeitbasis; diese Grenzen bleiben bestehen.

Zum Vergleich sind im c16-PulsLibre-HCI-Trace die ItemList-Anfragen und
-Antworten über Request-ID, äußere CRC, vollständige Frame-Reassembly und
Protobuf-Feldformen korreliert. Das ist kein vollständiger Connect-vs-PulsLibre
Side-by-side von Bootstrap, Filter, Dateipulls oder ACK-Semantik, weil für den
25.11.-Connect-Start die Connect-Protobuf-Sequenz noch nicht zuverlässig
dekodiert ist. Frühere Connect-Befunde vom 24.11. bleiben eine getrennte
Capture-Evidenz.

## Hypothesenbewertung und Produktfolgen

Die Arbeitshypothesen werden bewusst eng gefasst; sie bestätigen keine
unbelegte Kausalität:

1. **H1 – PulsLibre lädt nur Action-/Aktivitätsdateien.**
   **Nicht bestätigt.** Die 680 CSV-Zeilen stammen aus einer Aktivität. Das
   beweist nicht, dass alle Transfers Aktivitätsdateien waren. Der frühere
   Connect-FIT mit Global 397/398 widerlegt „Connect überträgt nur
   Standard-Aktivitätsnachrichten“ für den beobachteten Transfer, nicht aber
   einen Unterschied in PulsLibre.
2. **H2 – Connect fordert normale historische Messdaten mit anderen
   Kategorien/Filtern/Zeitbereichen an.**
   **Teilweise plausibel, nicht kausal bestätigt.** Die frühere Connect-Spur
   zeigt Daily-History-artige Datei-Inhalte und zusätzliche ItemList-Felder.
   Für den 25.11.-App-Start wurden keine Kategorien oder Zeitbereiche
   zuverlässig dekodiert; die Bedeutung/Wirkung der Filterfelder auf CIRQA ist
   offen. Aus dem Fehlen einer Vergleichssequenz folgt keine Abweichung.
3. **H3 – abweichende ACK-/Fortsetzungs-/Parserbehandlung erklärt die
   Differenz.**
   **Nicht bestätigt.** PulsLibre-c16 hat drei vollständige leere Antworten
   auf wiederholte, gleich tokenisierte Folgeanfragen. Dies ist ein beobachteter
   Unterschied im Verlauf, kein Nachweis, dass Connect mit anderem ACK oder
   anderer Fortsetzung mehr Daten bekäme. Die 24.11.-Connect-Spur zeigt
   erfolgreiche ItemList-/Pull- und Transferbefunde, aber ACKs sind nicht
   automatisch Dateiabschlussbelege; c17 ist noch nicht semantisch dekodiert.

**Downloaderänderung aus dieser Untersuchung: keine.** Es gibt keinen
nachgewiesenen Parser- oder Steuerungsfehler, dessen Fix durch die neue
Evidenz gerechtfertigt wäre. Bestehende begrenzte Transaktions-Followups
beibehalten; keine Connect-Filterwerte, Zeitfenster, zusätzliche Kommandos
oder ACK-Regeln erraten. Download-, FIT-Decoder-, SQLite-Persistenz- und
CSV-Exportbefunde getrennt führen.

## Nächste Offline-Schritte / Grenzen

1. Für c17 das ML-Transportframing weiter an Kontroll- und Datenkanälen
   prüfen; die Handle-Zuordnung ist jetzt peer-korreliert, aber nur abgeleitet.
2. Nur wenn die private GFDI-Sequenz verlässlich reassembliert ist, Connect
   `sync_request`, ItemList, Pulls, Transaktionstoken und ACKs feldweise
   inventarisieren. Die bestehende Spur nicht durch eine neue Gerätesynchroni-
   sation ersetzen oder ergänzen.
3. Den vorhandenen 24.11.-Connect-FIT weiterhin unabhängig durch
   Originalbytes → Decoder → SQLite-Provenienz → Export verfolgen; fehlende
   SQLite-Zuordnung in untersuchten Snapshots allein lokalisiert noch nicht,
   ob Decoder, Persistenz oder Export die Ursache ist.
4. Keine Roh-Payloads, privaten Werte, Hashes, Gerätekennungen oder
   BTSnoop-Dateien veröffentlichen.

Keine Geräteaktion und kein Code-/Transportverhalten wurde für diese Analyse
geändert. Die Evidenz wurde offline aus vorhandenen privaten Mitschnitten
ermittelt; für den Dokumentationsschritt wurden keine Tests ausgeführt.

## Bezug zu bestehenden Issues

Diese Befunde ergänzen die frühere Connect-Capture-Auswertung und
Transaktionsfilter-Prüfung zu #637. Die technischen Anschlussfragen zu
MLR/GFDI-/ATT-Reassembly (#506), historischem Download (#599), Pagination
(#148) und der früheren History-Arbeit (#7) bleiben offen; dieser Bericht
liefert keinen neuen Beleg, der eines dieser Issues allein schließen würde.
