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

### Capture- und Archivmatrix (nicht gleichzusetzende Läufe)

Die Zahlen 27, 29, 38 und 39 stammen aus unterschiedlichen Sitzungen und
bezeichnen nicht dieselbe Geräteliste. **27** bezeichnet hier sowohl einen
früheren Zwei-Listen-No-Pull-Repeatability-Lauf als auch einen späteren
separaten historischen Sync; diese zwei 27er-Befunde sind ebenfalls nicht
gleichzusetzen. Der 31-FIT-Bestand ist seinerseits ein früheres V24/V25-Archiv,
kein Teil der späteren ItemList-Captures.

| Evidenz | Request / Auswahl | Antwort und Transfer | Archiv / Decoder / SQLite / Export |
| --- | --- | --- | --- |
| S10 Repeatability, Lauf 13: **27** | Nach einem `sync_request` wurden im bestätigten No-Pull-Modus zwei ItemList-Abfragen in derselben BLE-Sitzung gesendet; der Plan dokumentiert nur den Listenvergleich und keinen Transfer-Pull. | Beide Antworten enthielten 27 Items; die UID-/Metadatenpaare waren innerhalb des Laufs unverändert. Keine MLR-Chunks, Datei-Pulls oder Importe. Der sitzungsübergreifende Bestand hatte sich zuvor verändert; diese 27 sind keine vollständige Gerätehistorie. | Kein Originalarchiv- oder Decoder-/SQLite-/Exportnachweis aus diesem No-Pull-Lauf. Quelle: [`CIRQA_ITEM_LIST_REPEATABILITY_TEST_PLAN.md`](../device-tests/CIRQA_ITEM_LIST_REPEATABILITY_TEST_PLAN.md). |
| Separater PulsLibre-Historienlauf, 26.11.: **27** | Eine initiale ItemList mit 27 Einträgen und drei begrenzte Transaktions-Folgeantworten; die genaue Filterwirkung ist nicht belegt. Dies ist nicht der Repeatability-No-Pull-Lauf in der Zeile darüber. | Vier vollständige Antworten: zunächst 27 Items, danach drei erfolgreiche leere Antworten. Alle 27 Items wurden transferiert, verifiziert und privat archiviert; 26 waren FIT, eines non-FIT. `runDrained=false`/`device-history-unverified` bleibt bestehen. | 26 FIT-Dateien SQLite-committed; 19 hatten importierbare Werte, 7 keine. Für diesen Lauf wurde hier kein tatsächlicher CSV-Export an Typ-32-Provenienzen korreliert. Capture-Nachtrag auf `origin/main` vom 26.11.; getrennt von c16/29/38/39 führen. |
| PulsLibre c16: **38** | Vollständig reassemblierte initiale ItemList-Antwort (Request-ID 2); drei begrenzte Folgeanfragen (IDs 41–43) mit demselben `next_transaction_id`; keine Connect-Filtersemantik daraus abgeleitet. | Initial 38 Items; jede der drei vollständigen Folgeantworten war `status=0`, hatte 0 Items und wiederholte den Token. Diese c16-ItemList-Evidenz allein korreliert keine ausgewählten Items mit Pulls oder MLR-Streams. | Kein aus c16 abgeleiteter Originalbytes-, Decoder-, SQLite- oder Exportnachweis in diesem Vergleich. Siehe Tabelle unten für die Feld-/Antwortdetails. |
| PulsLibre manueller Lauf 25.11.: **29** | ItemList mit 29 Einträgen; vollständige PullItem-Anfrage/-Antwort-Korrelation. Im hier vorliegenden HCI-Nachweis ist die initiale Request-Form nicht mit einer gleichzeitigen Connect-Aufzeichnung verglichen. | 29 PullItem-Antworten erfolgreich; 28 MLR-Konfigurationen und Streams. 28/28 erreichen zlib-EOF und deklarierte Größe; 25 sind FIT-envelope- und CRC-valide. Eine Pull-Antwort ohne beobachtete MLR-Konfiguration bleibt unzugeordnet. Keine Labels 15/28/32 in dieser Liste. | Lokaler Vorher/Nachher-Hashvergleich: die 25 validen FIT-Streams waren bereits vor dem Lauf archiviert; der Lauf fügte keine neuen FIT-Hashes hinzu. Separates Backup-Audit: 45 FIT-Quellen und 559 kanonische HR-Beobachtungen aus FIT Global 55/Feld 27 über zwei Quellen; nicht als Zuordnung dieser 25 Transfers ausgeben. |
| Garmin Connect, Capture 24.11.: **39** | Eine erfolgreiche ItemList-Antwort mit 39 Items, ohne Session-Cursor und mit Transaktionstoken. Der begrenzte Audit beobachtete sechs Requestformen: Felder 4+5, 5+6, jeweils mit Transaktionsfeld 2, danach Wiederholungen der beiden ersten Formen. Feldformen sind belegt, nicht die Wertewirkung auf die Auswahl. | 39 Pull-Anfragen und erfolgreiche Pull-Antworten. Ein früher Replay-Bericht ordnete 38 Transferkonfigurationen zu; eine spätere strikte Parser-Nachprüfung hat den Registrierungsbefund zurückgenommen, daher wird diese Registrierungskorrelation hier nicht als bestätigt gewertet. Unabhängig davon dokumentiert die private Byte-Rekonstruktion 38 Streams in erwarteter Größe, bytegleich im PulsLibre-Nachher-Archiv; der 39. Transfer ist nicht vollständig belegt. | Unter den 38 rekonstruierten Dateien sind 34 FIT mit gültiger Datei-CRC und Quellenzeile im Nachher-Backup; Beobachtungs-/Review-Kandidaten im Archiv sind dokumentiert. Kein Beweis für vollständige tägliche Abdeckung oder Connect-UI-Gleichheit. Quellen: [`CIRQA_CONNECT_S10_CAPTURE_COMPARISON_2026-11-23.md`](CIRQA_CONNECT_S10_CAPTURE_COMPARISON_2026-11-23.md) und [`CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md`](CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md). |
| Früheres PulsLibre V24/V25-Archiv: **31 FIT** | V24/V25-Übertragung umfasste 33 gelistete Dateien (31 FIT, 2 Nicht-FIT); dies ist kein Request-/ItemList-Vergleich mit den oben genannten späteren Läufen. | Alle 33 gelisteten Dateien wurden im damaligen Lauf als übertragen und privat gespeichert berichtet; keine Aussage, dass die Liste die gesamte Gerätehistorie umfasste. | Die veröffentlichte Inventur dokumentiert 31/31 FIT als Größen-, Magic-, Header-/Datei-CRC- und SHA-256-validiert. Eine zusätzliche lokale, hash-prefix-korrelierte Prüfung der aktuell verfügbaren Originale konnte 30/31 Dateien erneut öffnen und `File_ID.type` aus den FIT-Bytes lesen: 1=`device`×1, 4=`activity`×14, 5=`workout`×4, 32=`monitoring_b`×2; die übrigen Typ-IDs 44×4, 49×1, 68×1, 73×1, 74×1 und 79×1 bleiben hier ohne behauptete Semantik. Unter den 30 neu geprüften Dateien wurde kein Typ 15 oder 28 beobachtet; die fehlende 31. Quelle verhindert diese Aussage für den ganzen Satz. Decoder-/SQLite-Inventur: 294 passende importierbare Kandidaten (129 Herzfrequenz, 129 Distanz, 36 Geschwindigkeit) in SQLite; weitere Monitoring-, Schlaf-, HRV-, Stress-, SpO₂- und Atemfelder blieben je nach Feld/Zeitstempel nicht kanonisch importiert. Die Inventur belegt weder Export/UI-Abdeckung noch vollständige Historie. Quelle: [`CIRQA_OFFLINE_FILE_INVENTORY_2026-11-18.md`](CIRQA_OFFLINE_FILE_INVENTORY_2026-11-18.md); Profilnamen nach Garmin FIT Profile 21.222.0. |

**Request-/Antwortgrenze:** Nur c16 und der 25.11.-PulsLibre-Lauf sind in diesem
Bericht feldweise als vollständige ItemList-/Folgeantworten bzw. ItemList-zu-
Pull-Korrelation beschrieben. Für Connect 24.11. ist der erfolgreiche
39er-ItemList-/Pull-/Transferpfad separat dokumentiert. Der 27er-No-Pull-Lauf
ist eine Repeatability-Beobachtung, kein Dateiabruf. Die 31 FIT sind ein
Archiv-/Importbestand aus V24/V25. Diese Daten dürfen daher weder als
identische Stichproben noch als Beleg für dieselbe Auswahlentscheidung
verglichen werden.

### Getrennter Post-Run-Snapshot: Typ 32 bis SQLite/CSV-Codepfad

Ein weiterer, eigenständiger lokaler Post-Run-Backup-Snapshot vom 26.11. wurde
offline anhand vollständiger Archiv-Chunks, SHA-256 und FIT-Envelope geprüft.
Er enthält 3.242 vollständige Originale, darunter 39 FIT. Aus den tatsächlichen
FIT-`File_ID.type`-Bytes wurden drei Typ-32-Dateien identifiziert; jede hat eine
aktive Decoder-Generation und enthält Global Message 55 (`monitoring`). Diese
Snapshot-Zählung ist weder eine Fortsetzung der 29er-ItemList noch die
Rekonstruktion einer 39er-Connect-Liste.

Für diese drei Monitoring-B-Dateien ist die Pipeline wie folgt belegt:

| Stufe | Capture-spezifischer Befund |
| --- | --- |
| Originalarchiv | 3/3 FIT-Originale vollständig aus Archiv-Chunks rekonstruiert und als Typ 32 gelesen; 3/3 mit aktiver FIT-Generation. |
| Decoder/Feldpersistenz | 3.758 rohe Global-55-Feldvorkommen sind in `canonical_fit_generation_fields` persistiert. Die Originaldateien enthalten zusätzlich Monitoring-Nachrichten 227, 269 und 297; unbekannte bzw. nicht freigegebene Felder bleiben separat inventarisiert. |
| Kanonische SQLite-Beobachtungen | 1.156 Global-55-Herzfrequenz-Beobachtungen sind in `canonical_fit_generation_observations` vorhanden und über 1.156 Provenienzzeilen mit den Dateien verbunden. |
| Review-Kandidaten | 5.529 Kandidaten bleiben explizit `UNTRUSTED_PROFILE_CANDIDATE`: Stress 1.572, SpO₂ 1.120, SpO₂-Konfidenz 1.122, Atmung 1.572 sowie Monitoring-Aktivitätstyp 12 und Zyklen 131. Sie sind nicht mit kanonischen Messungen gleichzusetzen. |
| CSV-Export | Der aktuelle `UnifiedMeasurements`-Code materialisiert aktive FIT-Generationsbeobachtungen in die Unified-Tabelle; `ExportActivity` liest daraus CSV-Zeilen. Damit liegt für die 1.156 kanonischen HR-Beobachtungen ein Codepfad zum Export vor. Ein tatsächlich ausgeführter CSV-Export dieses Snapshots wurde nicht nachgewiesen. Review-Kandidaten und rohe Feldbytes werden dadurch nicht als CSV-Messwerte ausgegeben. |

Das belegt für diesen separaten Snapshot: Typ-32-Historie kann archiviert,
feldweise decodiert und teilweise kanonisch in SQLite persistiert werden; die
HR-Provenienz erreicht den vorgesehenen Unified-/CSV-Codepfad. Es beweist
weder, dass dieselben Dateien in einem der oben genannten HCI-Captures
angeboten wurden, noch dass der Export tatsächlich ausgeführt wurde oder die
Gerätehistorie vollständig ist.

### Nachweis eines vorhandenen CSV-Exports aus einem früheren Snapshot

Zusätzlich wurde eine vorhandene lokale CSV-Datei read-only über ihre
`sample_id`s mit einem älteren, separaten Backup-Snapshot abgeglichen. In diesem
Backup lagen fünf FIT-Dateien mit `File_ID.type=32` und 1.144 aktive, kanonische
Global-55/Feld-27-Herzfrequenz-Beobachtungen. Die 1.488 Datenzeilen umfassende
CSV-Datei enthält **360 sample-id-identische Zeilen** aus genau dieser
Provenienzgruppe; alle 360 sind `heart_rate`/`bpm`. Es wurden weder Messwerte
noch Sample-IDs veröffentlicht. Zwei weitere vorhandene CSV-Dateien hatten
keine `sample_id`-Überschneidung mit dieser Typ-32-Provenienz.

Damit ist für mindestens diese 360 Typ-32-Herzfrequenz-Beobachtungen nicht nur
der Export-Codepfad, sondern auch eine tatsächliche CSV-Ausgabe mit
FIT-Provenienz belegt. Die übrigen 784 Beobachtungen dieses Backup-Snapshots
wurden in dieser CSV nicht wiedergefunden; Grund und zeitlicher
Datenbankzustand sind nicht festgestellt. Eine weitere redigierte Prüfung
schränkt die Erklärung ein: Alle 1.144 Beobachtungen liegen innerhalb des
breiten Datumsbereichs der CSV und innerhalb der darin vorhandenen
Zeitstempel-Spanne; alle haben `PROFILE_VALID`-Qualität und einen positiven
Herzfrequenzwert. Daher erklären weder das gewählte Export-Zeitfenster noch der
im Unified-Export verwendete Ausschluss nichtpositiver Herzfrequenzwerte die
784 fehlenden IDs. Die 360 Treffer stammen aus zwei der fünf Typ-32-Quellen
(350 aus einer, 10 aus einer weiteren); die übrigen drei Quellgruppen mit
insgesamt 784 Beobachtungen haben keine CSV-Treffer. Da der verwendete
Backup-Snapshot und der tatsächliche Export keinen nachgewiesen identischen
SQLite-Zeitstand teilen, lässt sich daraus nicht entscheiden, ob diese drei
Quellgruppen beim Export noch nicht in der exportierten Datenbankgeneration
vorlagen oder ob ein anderer Zustandspfad wirkte. Keine Ursache ist damit
bewiesen.

Für den getrennten 26.11.-Snapshot mit drei Typ-32-Dateien fand sich in den
geprüften CSV-Dateien keine passende `sample_id`-Überschneidung. Keiner dieser
Exportnachweise ordnet die Dateien einem der 27/29/38/39-ItemList-Captures zu.

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

### FileAccess-Inventar des PulsLibre-Laufs vom 25.11.

Eine zusätzliche private, metadatenbegrenzte Auswertung des HCI-Deltas zum
manuell gestarteten PulsLibre-Lauf korreliert die ItemList-Antwort und Pulls
über ihre Protobuf-Request-IDs und Transfer-Handles:

- Die vollständige ItemList-Antwort enthält 29 Einträge (`status=0`); alle 29
  Items haben eine korrelierte PullItem-Anfrage und -Antwort mit
  `resultCode=0`/Transportstatus 0.
- Die ItemList-Metadaten kategorisieren 19 Items als `FIT_TYPE_4`, vier als
  `FIT_TYPE_5`, je eines als `FIT_TYPE_1`, `FIT_TYPE_72` und `FIT_TYPE_74`,
  zwei als `format_0` sowie eines als `format_3`. Diese sind FileAccess-
  Metadatenlabels, **keine** aus den übertragenen FIT-Bytes gelesenen
  `File_ID.type`-Werte. In Garmins offiziellem [FIT SDK Profile
  21.222.0](https://github.com/garmin/fit-python-sdk/blob/21.222.0/garmin_fit_sdk/profile.py)
  sind die enum-Werte 1=`device`, 4=`activity` und 5=`workout`; für die übrigen
  Labels ist damit keine FIT-Payload-Semantik belegt. Das offizielle Profil
  definiert außerdem FIT-File-Enums 15=`monitoring_a`, 28=`monitoring_daily`
  und 32=`monitoring_b`. Dies bestätigt die Namen als offizielle FIT-File-
  Kategorien, **nicht** deren Auftreten in diesem ItemList-/Transfermitschnitt.
- Für 28 Pull-Antworten wurde eine MLR-Konfiguration beobachtet; die 29.
  Antwort (Item-Metadatengröße 6.010 Bytes) hat im Mitschnitt keine
  nachfolgende Konfiguration. Der Mitschnitt endet rund 56,5 Sekunden nach
  dieser Antwort. Das ist ein unvollständiger Mitschnittbefund, kein Beleg,
  dass PulsLibre den Pull verworfen oder das Band die Datei nicht geliefert
  hat.
- Eine korrigierte Offline-Rekonstruktion ohne zusätzlich angenommene
  Zwei-Byte-Präfixe pro Notification brachte alle 28 konfigurierten MLR-Streams
  bis zum zlib-EOF; bei allen 28 stimmt die dekomprimierte Länge mit der
  PullItem-deklarierten Größe überein. Bei 25 Streams sind zusätzlich FIT-
  Signatur, deklarierte Länge, Header-CRC und Datei-CRC gültig. Die alternative
  Zwei-Byte-Präfix-Interpretation validiert 0/28 und wird daher für diese
  Capture-Rekonstruktion verworfen. Für die drei übrigen Streams ist ein
  erfolgreicher zlib-Abschluss mit passender Größe belegt, aber keine valide
  FIT-Datei.
- Für die 25 validen FIT-Dateien wurde `File_ID.type` unabhängig aus den
  übertragenen Bytes gelesen: die Labels `FIT_TYPE_4`, `FIT_TYPE_5`,
  `FIT_TYPE_1` und `FIT_TYPE_72` stimmen jeweils mit dem jeweiligen Payload-
  Typ überein. Die zwei `format_0`- und eine `format_3`-Datei sind keine FIT-
  Dateien. Daraus folgt weiterhin nicht, dass ein beobachtetes Label
  `FIT_TYPE_15`, `FIT_TYPE_28` oder `FIT_TYPE_32` in der Liste aufgetreten
  wäre: keines dieser drei Monitoring-Labels ist in dieser 29-Item-Liste
  beobachtet. Eine Pull-Antwort hat keine zugehörige MLR-Konfiguration im
  Mitschnitt; ihre Payload-Zuordnung bleibt offen. Alle diese Befunde sind
  capture-spezifisch und ersetzen weder den separaten Terminal-/Backup-
  Nachweis noch einen Nachweis vollständiger Gerätehistorie.

Damit liegt nun eine konkrete **FileAccess-Auswahl** für PulsLibre vor, aber
weiterhin kein belastbarer Vergleich mit Connects ausgewählten Monitoring-
Items oder deren `File_ID.type`. Eine Ursache für fehlende normale
Monitoring-Historie und insbesondere eine Wirkung optionaler ItemList-Filter
ist damit nicht bewiesen. Es gibt weiterhin keinen Grund, Requestfilter oder
Downloader-Auswahl spekulativ zu ändern.

Im ausgewählten c17-Zeitraum:

- HCI-Replay zählt 649 Host-Write-Commands und 892 Notifications in der
  Verbindung; ATT-Value-Handles sind 0x0015 (Write) und 0x0012 (Notification).
- Die c17-UUIDs sind im betreffenden Discovery-Abschnitt nicht direkt
  aufgelöst. Die Werte `0x0012` (Notification) und `0x0015` (Write) stimmen
  jedoch mit den ATT-Handles aus zwei früheren Verbindungen desselben Peers
  überein, in denen die Charakteristiken direkt als Garmin-ML RX
  `6a4e2810` und TX `6a4e2820` entdeckt wurden. Die c17-Zuordnung ist damit
  stark abgeleitet, nicht in c17 selbst entdeckt.
- In c17 wurden 29 vollständige ML-Registrierungsanfragen und die jeweils
  zugehörigen Antworten korreliert: alle Antworten haben Status 0; beteiligt
  sind 16 Service-IDs. Die beobachteten Control-Nachrichten verwenden Client-ID
  1; Antwort-Handlebytes liegen zwischen 0x80 und 0x87. Das belegt angenommene
  ML-Registrierungen, aber weder deren nachgelagerte GFDI-Semantik noch einen
  History-Request. Diese Werte werden nicht als Datei- oder Messdatentransfer
  interpretiert.
- Ein eingehender MLR-Datenkanal (nach Routing-Flag-Normalisierung Route 4)
  lieferte nach Abtrennung der ersten, 5 Byte langen Konfigurationsantwort und
  eines ein Byte langen Transportpräfixes je weiterer ATT-Notification einen
  vollständigen zlib-Stream: 4.012 komprimierte Bytes, 12.981 dekomprimierte
  Bytes, EOF. FIT-Signatur, Header-CRC und Datei-CRC sind gültig.
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
