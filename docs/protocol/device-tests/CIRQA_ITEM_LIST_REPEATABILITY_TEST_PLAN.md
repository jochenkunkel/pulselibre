# CIRQA Item-List-Wiederholbarkeit – Lab-Testplan

## Zweck

Zwei direkt aufeinanderfolgende FileAccess-Item-List-Anfragen innerhalb derselben
BLE-Sitzung vergleichen. Das prüft, ob die gelesenen Item-UIDs und deren
Metadaten bei einer wiederholten Anfrage stabil erscheinen. Es prüft **nicht**
die Stabilität über getrennte Sitzungen, die Vollständigkeit des Gerätearchivs
oder die Semantik der UIDs bei Retention/Dateiersetzung.

## Ablauf in PulsLibre Testlabor

1. S25 mit dem CIRQA verbunden lassen; sicherstellen, dass kein anderer BLE-
   Test oder Recorder aktiv ist.
2. Im Testlabor **„Item-List zweimal vergleichen (kein Pull)“** wählen.
3. Bestätigungsdialog und Grenzen prüfen; nur bei eindeutigem CIRQA-Ziel starten.
4. Der Test führt frisches Bootstrap/MLR-Setup aus und sendet exakt zwei
   read-only `FileAccess item_list_request`-Anfragen. 40 Sekunden Gesamtbudget,
   kein Retry.
5. Den abschließenden Status **„Item-List-Vergleichsstatus“** sichern.

## Sicherheits- und Datenschutzgrenzen

- Dieser Test sendet **keinen** `sync_request`, `pull_item`, Datei-Pull,
  Archiv-/Flag-/Lösch-/Reset- oder `CLOSE_ALL`-Befehl.
- Es werden keine Datei-Bytes angefordert oder gespeichert.
- UID-Bytes verbleiben nur flüchtig im Arbeitsspeicher. Verglichen werden
  interne SHA-256-Schlüssel; Status und Bericht enthalten ausschließlich
  Counts, keine UIDs, Hashes, Gerätedaten oder Datei-/Messwerte.
- Ein GFDI-5000-Frame zu 5043 wird als Transport-ACK getrennt von einer
  FileAccess-5044-Antwort ausgewiesen. Ein ACK belegt allein keine Annahme der
  Item-List-Anfrage durch die Geräteanwendung.
- Das persistente S25-Live-Test-Gate erlaubt insgesamt höchstens 15 ausdrücklich
  bestätigte Läufe (direkte Item-List- und no-pull-Sync-Diagnose zusammen) und
  wird vor jeder Verbindung atomar fortgeschrieben. Fehlversuche verbrauchen
  ebenfalls einen Versuch; es gibt keinen automatischen Retry oder Gate-Reset.

## Interpretation

- `UID+Metadaten unverändert`: gleicher UID-Schlüssel mit identischem
  `data_type`, `data_size` und `flags` in beiden Antworten.
- `Metadaten geändert`: gleicher UID-Schlüssel, aber mindestens eines der
  Metadatenfelder unterscheidet sich.
- `neu` / `fehlend`: UID nur in der zweiten / nur in der ersten Antwort.
- Doppelte UIDs werden separat gezählt und machen die Liste mehrdeutig.
- Eine vorhandene `session_id` wird nur vermerkt; der Test stoppt vor
  Pagination, damit er genau zwei Anfragen sendet.

## Bisherige S25-Ergebnisse

Drei separat bestätigte direkte Läufe erhielten jeweils ein CRC-validiertes
GFDI-5000-Transport-ACK zu 5043, aber keine FileAccess-5044-Item-List-Antwort.
Daher gab es keine Listen zum Vergleichen und keine Aussage zur UID-Stabilität.
Der vierte Lauf verwendete den getrennten Sync-Request-Pfad und erhielt eine
Item-List mit 45 Einträgen (eine Seite; keine session_id, nextTransactionId
vorhanden). Der Lauf stoppte vor MLR/Pull/Import. UID-Wiederholbarkeit bleibt
ungeprüft; auch ein erfolgreicher Listenlauf beweist keine vollständige
historische Abdeckung.

Der fünfte Lauf verwendete den ausdrücklich bestätigten Zwei-Listen-Modus:
beide Antworten enthielten 23 Einträge; 23 UID-/Metadatenpaare waren
unverändert, 0 Metadaten geändert, 0 neu/fehlend und 0 doppelte UIDs. Es gab
keine MLR-Chunks/-Bytes, Datei-Pulls oder Importe. Das belegt
Same-session-Wiederholbarkeit nur für diese beiden Antworten. Da der vierte
Lauf 45 Einträge lieferte, bleibt die Abweichung zwischen Listenläufen zu
untersuchen; UID-Stabilität über Sitzungen und Archivvollständigkeit sind
weiterhin unbewiesen.

Der sechste Lauf wurde nach einer zwischenzeitlichen Garmin-Connect-Synchroni-
sation erneut als Zwei-Listen-No-Pull-Test ausgeführt. Ergebnis wieder 23/23,
alle UID-/Metadatenpaare unverändert, 0 geändert/neu/fehlend, keine doppelten
UIDs; keine MLR-Chunks/-Bytes, Datei-Pulls oder Importe. Das passt zu der
Vermutung, dass die zwischenzeitliche Garmin-Connect-Synchronisation den
vorherigen 45-vs-23-Unterschied beeinflusst haben könnte, beweist aber keinen
Kausalzusammenhang. Gemeinsamer Zähler: 6/15.

Der siebte Lauf folgte auf den Hinweis, dass möglicherweise vor dem vierten
Lauf der Aktionsknopf des Armbands gedrückt worden war; Garmin Connect habe
daraufhin eine Meldung angezeigt und die Funktion deaktiviert. Der neue
Zwei-Listen-No-Pull-Vergleich ergab erneut 23/23 identische UID-/Metadatenpaare,
0 geändert/neu/fehlend, keine doppelten UIDs und keine MLR-Chunks/-Bytes,
Datei-Pulls oder Importe. Ein Einfluss der Knopfaktion ist plausibel, aber
unverifiziert. Gemeinsamer Zähler: 7/15.

Der achte Versuch endete vor dem `sync_request`: Das Gerät lehnte die
GFDI-Registrierung ab; es wurden keine Item-Lists empfangen. Der Versuch wurde
trotzdem verbraucht. Beim anschließenden Knopftest startete die CIRQA die
eingestellte „Aktivität mit Zeitangabe“. Die Aktivität wurde kurz in Garmin
Connect gespeichert. Der neunte PulseLibre-Lauf erhielt danach zwei Listen
mit je 24 Einträgen; alle 24 UID-/Metadatenpaare waren identisch, 0 geändert,
neu, fehlend oder doppelt. MLR-Chunks/-Bytes, Datei-Pulls und Importe: 0.
Der Anstieg von 23 auf 24 passt zur gespeicherten Aktivität, ist ohne
Dateiabruf/UID-Zuordnung aber kein Beweis. Gemeinsamer Zähler: 9/15.

Der zehnte Lauf wiederholte den Zwei-Listen-No-Pull-Test: erneut 24 Einträge
je Liste und 24 unveränderte UID-/Metadatenpaare; 0 geändert, neu, fehlend
oder doppelt. Es gab keine MLR-Chunks/-Bytes, Datei-Pulls oder Importe.
Gemeinsamer Zähler: 10/15.

Für Lauf elf drückte der Nutzer unmittelbar davor erneut den Bandknopf. Beide
Listen enthielten weiterhin 24 Einträge und dieselben 24 UID-/Metadatenpaare;
es gab 0 geändert, neu, fehlend oder doppelt. MLR-Chunks/-Bytes, Datei-Pulls
und Importe: 0. In diesem Lauf wurde kein weiterer Listeneintrag sichtbar;
das belegt nicht, ob das Band intern eine Aktion gespeichert hat.
Gemeinsamer Zähler: 11/15.

Im Anschluss wurde die während des vorigen Tests laufende Garmin-Connect-
Aktivität (4:10) gestoppt und gespeichert; Connect zeigte sie danach in
„Aktivitäten von heute“. Der zwölfte Zwei-Listen-No-Pull-Lauf lieferte
26 Einträge je Liste und 26 unveränderte UID-/Metadatenpaare, 0 geändert,
neu, fehlend oder doppelt. Gegenüber dem vorherigen 24er-Bestand sind zwei
Einträge hinzugekommen. Der zeitliche Zusammenhang mit dem Speichern ist
auffällig, aber ohne UID-Zuordnung oder Dateiabruf ist nicht belegt, dass
beide Einträge dieser Aktivität zuzuordnen sind. MLR-Chunks/-Bytes, Pulls und
Importe: 0. Gemeinsamer Zähler: 12/15.

Der dreizehnte Lauf wurde bei laufendem Garmin-Connect-Dienst ausgeführt. Der
Android-Bluetooth-Status zeigte unmittelbar davor und danach Garmin Connect
als GATT-ACL-Inhaber. PulseLibre erhielt dennoch erfolgreich zwei Listen mit
je 27 Einträgen und 27 unveränderten UID-/Metadatenpaaren; 0 geändert, neu,
fehlend oder doppelt. Gegenüber dem vorherigen Bestand: +1, ohne UID-Zuordnung
nicht interpretierbar. MLR-Chunks/-Bytes, Datei-Pulls und Importe: 0. Das
belegt, dass ein aktiver Connect-GATT-Inhaber eine erfolgreiche PulseLibre-
Registrierung nicht grundsätzlich verhindert; ein transienter Konflikt als
Erklärung für Versuch 8 ist damit nicht ausgeschlossen. Gemeinsamer Zähler:
13/15.

Danach wurde in Garmin Connect „Gerät synchronisieren“ ausgelöst. Im Anschluss
lieferte Lauf 14 zwei Listen mit je 25 unveränderten UID-/Metadatenpaaren;
Lauf 15 lieferte zwei Listen mit je 26 unveränderten Paaren. Beide Läufe
hatten 0 geändert, neu, fehlend oder doppelt und keine MLR-Chunks/-Bytes,
Datei-Pulls oder Importe. Der sitzungsübergreifende Verlauf 27 → 25 → 26
zeigt, dass der Listenbestand nicht stabil blieb; ein kausaler Effekt der
Connect-Synchronisierung lässt sich daraus nicht isolieren. Auch die
geräte-seitige Wirkung des jeweils gesendeten `sync_request` bleibt als
Störfaktor bestehen. Das gemeinsame Limit von 15 bestätigten Läufen ist
ausgeschöpft; keine weiteren Live-Läufe ohne neues, separat bestätigtes
Versuchsbudget.

## Separater Folgeversuch nach Transport-ACK ohne Item-List-Antwort

Die ersten direkten Item-List-Anfragen erhielten einen CRC-geprüften
GFDI-5000-Transport-ACK für 5043 (`requestId=1`, `offset=0`,
`successFlag=0`, `status=0`), aber keine 5044-Item-List-Antwort. Im vierten
bestätigten Lauf wurde deshalb ein einzelner Connect-kompatibler `sync_request`
vor einer Item-List-Anfrage gesendet. Das Gerät kann den `sync_request`
geräteseitig als übertragen markieren. Ergebnis: eine CRC-validierte
Item-List-Seite mit 45 Einträgen, `pullSent=false`, MLR-Chunks/-Bytes 0. Der
Lauf stoppte vor MLR/Pull/Import. 40 s, kein Retry, gemeinsames persistentes
Limit von 15 bestätigten Läufen; Fehlversuche verbrauchen einen Lauf.

Der separat bestätigte no-pull-Modus fordert nach einem
`sync_request` genau zwei Item-Lists in derselben BLE-Sitzung an und vergleicht
UID-Schlüssel sowie Metadaten nur flüchtig im Speicher. Angezeigt/gespeichert
werden ausschließlich Counts. Der Modus stoppt nach der zweiten Antwort vor
MLR-Registrierung, Datei-Pull, Pagination und Import; das mögliche
geräteseitige Markieren durch `sync_request` wird vorher offengelegt. Der
fünfte und sechste Lauf ergaben jeweils 23/23 identische Einträge pro Sitzung.
Der Vergleich testet nur Same-session-Wiederholbarkeit, nicht UID-Stabilität
über Sitzungen oder Archivvollständigkeit.

## Addendum: vorbereiteter Cross-session-No-pull-Test

Nach ausdrücklicher Nutzerfreigabe wurde die Obergrenze des gemeinsamen
No-pull-Test-Gates von 15 auf 40 bestätigte Läufe angehoben. Der vorhandene
persistente Zähler wird dabei nicht zurückgesetzt; die dokumentierten 15
verbrauchten Läufe bleiben erhalten. Jeder weitere Lauf erfordert weiterhin
die ausdrückliche Bestätigung im App-Dialog und verbraucht vor der Verbindung
einen Versuch.

Für einen sitzungsübergreifenden Vergleich liefert die vorbereitete App-Version
zusätzlich einen aggregierten SHA-256-Fingerprint über UID, Typ, Größe und
Flags aller Listeneinträge. Roh-UIDs und Einzel-Hashes werden nicht angezeigt.
Der aggregierte Fingerprint wird lokal im Diagnose-Status gespeichert und ist
dennoch ein linkbarer Geräte-/Listenbezeichner: nicht veröffentlichen oder in
Git protokollieren. Vor dem nächsten Lauf muss der Nutzer ihn privat notieren,
weil der Status beim folgenden Lauf ersetzt wird.

Testablauf nach Bereitstellung einer signaturkompatiblen APK:

1. Drei getrennte bestätigte No-pull-Läufe mit jeweils einer Item-List in
   frischen BLE-Sitzungen ausführen; vor jedem Lauf den Status sichern.
2. Prüfen, dass Listenanzahl und Fingerprint über alle drei Sitzungen
   identisch sind; Abweichungen bedeuten „UID-Stabilität nicht belegt“.
3. Nach jedem Lauf sicherstellen, dass `pullSent=false`, MLR-Chunks/-Bytes 0
   und keine Dateiimporte gemeldet werden.
4. Da jeder Lauf `sync_request` enthält, vor jedem Lauf berücksichtigen, dass
   das Band Daten geräteseitig als übertragen markieren kann.

Auch drei identische Läufe belegen nur beobachtete Stabilität dieses Geräts,
dieser Firmware und dieser Beobachtungsperiode; sie beweisen weder
unbeschränkte UID-Stabilität noch vollständige Gerätehistorie.

## Lauf vom 2026-11-21 nach Freigabe des 40er-Limits

Das signaturkompatible Update `2.0.0_build1595` wurde als Update installiert;
der bestehende App-Datenbestand blieb erhalten. Der vollständige lokale CI-Lauf
einschließlich Emulator war zuvor erfolgreich.

- Ein bestätigter No-pull-Lauf erreichte `sync_request` und empfing eine
  Item-List mit 41 Einträgen. Ergebnis: `accepted=true`, `itemListSent=true`,
  `pullSent=false`, MLR-Chunks/-Bytes 0, Dateiimporte 0. Ein aggregierter
  Listenfingerprint wurde nur lokal für den privaten Folgevergleich verwendet
  und absichtlich nicht in dieses Dokument übernommen.
- Die beiden folgenden, separat bestätigten Läufe endeten jeweils vor
  `sync_request` mit GATT-Status 147, ohne Notifications/Listenantwort,
  Datei-Pull oder Import. Beide Versuche wurden verbraucht.
- Verbrauch: 18/40. Nach zwei aufeinanderfolgenden GATT-147-Abbrüchen wurden
  weitere Funkversuche gestoppt.

Damit existiert nur eine erfolgreiche Sitzung als Baseline. Cross-session-
UID-Stabilität ist **nicht nachgewiesen**; keine Remote-Datei darf aufgrund
dieses Einzelergebnisses übersprungen werden.

## Diagnose des GATT-147-Fehlers und nächster Ablauf

Android dokumentiert Status 147 (`BluetoothGatt.GATT_CONNECTION_TIMEOUT`) als
Verbindungs-Timeout; als wahrscheinliche Gründe nennt die API-Dokumentation
außer Reichweite oder fehlende Connectable-Advertisements:
[BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#GATT_CONNECTION_TIMEOUT).
Beide fehlgeschlagenen Läufe hatten `state=DISCONNECTED`, Notifications 0,
keinen GATT-Write, `sync_request` nicht gesendet und keine Item-List. Es ist
also ein Verbindungsaufbaufehler vor dem CIRQA-Protokoll, nicht ein negativer
Sync-/Item-List-Status. Die konkrete Ursache ist aus Status 147 allein nicht
ableitbar.

Der nächste Geräteablauf ist deshalb passiv zuerst: 15 Sekunden BLE-Scan ohne
GATT-Verbindung, Pairing oder Sync-Request. Die Diagnose wird nur melden, ob
eine Anzeige des bereits gekoppelten Geräts erkannt wurde und ob Android diese
als verbindbar meldete; keine Geräteadresse wird gespeichert. Nur wenn eine
passende verbindbare Anzeige erkannt wird, kann anschließend nach erneuter
Bestätigung höchstens ein No-pull-Listenlauf folgen. Andernfalls wird ohne
weiteren Verbindungsversuch gestoppt. Der GATT-147-Codepfad zeigt künftig
Androids lesbare Statusbeschreibung an; automatische Retries bleiben aus.

### Passiver Scan am 2026-11-21

Mit signiertem `2.0.0_build1601` (Versioncode 1601; Zertifikat passend zum
installierten Paket) wurde ein 15-Sekunden-Scan durchgeführt:

- 106 Werbeereignisse; Abgleich mit der gekoppelten CIRQA-Anzeige: ja.
- Android meldete die Anzeige als verbindbar; Median-RSSI −56 dBm.
- Kein GATT-Verbindungsversuch, kein Sync-Request und kein Datei-Pull.
- Es wurden keine Geräteadresse oder Rohdaten in die Dokumentation übernommen.

Der passive Befund erfüllt nur die Vorbedingung für eine mögliche erneute
Bestätigung. Er belegt weder eine erfolgreiche GATT-Verbindung noch
Cross-session-UID-Stabilität.

### Bestätigter No-pull-Lauf nach Passivscan am 2026-11-21

Nach erneuter Nutzerbestätigung wurde genau ein Lauf ausgeführt. Er empfing
nach `sync_request` eine Item-List mit 41 Einträgen und stoppte vor Datei-Pulls:
`accepted=true`, `itemListSent=true`, `pullSent=false`; MLR-Chunks/-Bytes 0,
Dateiimporte 0. Der lokale aggregierte Listenfingerprint stimmte mit dem der
vorherigen erfolgreichen Sitzung überein; der Fingerprint wird hier nicht
veröffentlicht. Gate-Verbrauch: 19/40. Es gab keinen Retry.

Damit stimmen zwei erfolgreiche, getrennte Listenbeobachtungen in Anzahl und
aggregiertem Fingerprint überein. Die zwei dazwischenliegenden GATT-147-
Verbindungsabbrüche lieferten keine Listen und sind kein Gegenbeleg zur
Gleichheit der beiden empfangenen Listen. Die kleine Zahl erfolgreicher
Beobachtungen belegt noch keine dauerhafte Identitätsstabilität oder sichere
Überspringbarkeit künftiger Dateien; gezielte Pull-Auswahl bleibt bis zu
weiterer Evidenz fail-safe.

### Dritter erfolgreicher No-pull-Lauf am 2026-11-21

Im Rahmen der Tagesfreigabe wurde genau ein weiterer Lauf ohne Retry ausgeführt.
Er empfing wieder 41 Einträge; `accepted=true`, `itemListSent=true`,
`pullSent=false`, MLR-Chunks/-Bytes 0 und Dateiimporte 0. Gate-Verbrauch:
20/40. Der private aggregierte Listenfingerprint entsprach erneut den beiden
vorherigen erfolgreichen Sitzungen; kein Fingerprint und keine Einzel-UID
wird hier veröffentlicht.

Somit sind drei erfolgreiche getrennte Listenbeobachtungen in diesem Test
untereinander identisch (je 41 Einträge und gleicher aggregierter Fingerprint).
Das ist positive Evidenz für beobachtete Listen-/UID-Wiederholbarkeit auf
diesem Gerät in dieser Testperiode; die zwei fehlgeschlagenen GATT-Verbindungen
stellen keine zusätzlichen Listenbeobachtungen dar. Es ist keine Garantie für
zukünftige Sitzungen oder unveränderte Datei-Inhalte. Der Lauf hat keine FIT-
Datei-Pulls und keinen erfolgreichen Import erzeugt, daher ist die Bedingung
„gleiche stabile Identität und zugehöriger erfolgreicher lokaler Import“ der
gezielten Skip-Policy nicht praktisch validiert. Bis zur Integration und
Validierung dieser Zuordnung bleibt Überspringen fail-safe deaktiviert.

## Offline-Skip-Gate

`CirqaUidStabilityEvidence` und `CirqaTargetedPullManifest` bilden die
konservative Mindestschwelle pro Remote-Identität ab: mindestens drei
unterschiedliche erfolgreiche Sync-Sitzungen müssen dieselbe UID-/Typ-/Größen-/
Flags-Identität gesehen haben. Fehlversuche, doppelte Sitzungs-IDs,
unvollständige Beobachtungen oder Persistierungsfehler erzeugen keinen
Stabilitätsnachweis. Der bestehende Produkt-Sync beobachtet erfolgreiche
Item-List-Einträge, speichert aber ausschließlich gehashte Identitäten in
app-privaten, nicht gesicherten SharedPreferences. Nach einem erfolgreichen
FIT-Import wird die Zuordnung zur vollständigen Inhalts-SHA-256 gespeichert.

Vor einem Skip verifiziert der Produktpfad erneut die lokale FIT-Datei samt
Hash/CRC sowie einen passenden `fit_sources`- und
`fit_observation_provenance`-Nachweis in der kanonischen SQLite-Datenbank.
`CirqaTargetedPullPolicy` überspringt nur bei dieser gesamten Evidenzkette;
alle unbekannten Fälle bleiben pull-pflichtig. Bestehende lokale FIT-Dateien
ohne aufgezeichnete Remote-UID-Zuordnung werden beim ersten Sync nach dem
Update weiterhin gezogen, nicht anhand Größe/Typ geraten. Die drei bereits
erhobenen Gerätelisten wurden vor Einführung dieser Persistenz erfasst und
werden nicht in die App importiert; nach dem Update müssen drei erfolgreiche
Sitzungen die lokale Evidenz neu aufbauen. Unit-Tests und vollständiger lokaler
CI-Lauf sind grün. Der folgende Hardwaretest validiert den Skip-Pfad für die
beobachteten Einträge; die Gerätehistorie bleibt unvollständig.

## Hardwarevalidierung der Manifest-Policy am 2026-11-21

Der signierte Update-Build `2.0.0_build1609` wurde auf dem Gerät als Update
installiert; bestehende App-Daten blieben erhalten. Der zuvor erstellte Build
1602 wurde wegen des bereits installierten Codes 1608 nicht installiert
(Downgrade vermieden). Keine GATT-/Protokollsequenz wurde ergänzt.

1. Erster bestätigter Produkt-Sync: Item-List 43 Einträge, `pullSent=true`,
   638.954 übertragene Bytes. 43 Dateien übertragen/verifiziert/privat
   gespeichert, 0 Übertragungsfehler, 40 SQLite-Commits, davon 19 importierte
   FIT-Dateien und 21 FIT-Dateien ohne importierbare Beobachtungen; drei
   weitere Dateien waren Nicht-FIT. Der Lauf stoppte nach der Item-List-Session
   wegen angekündigter separater Transaktion (`next-transaction-pending`);
   keine weitere Transaktion wurde angefragt.
2. Zwei getrennt bestätigte No-pull-Sitzungen lieferten je 43 Einträge,
   `pullSent=false`, 0 MLR-Chunks/Bytes; Gate-Verbrauch 22/40.
3. Nächster bestätigter Produkt-Sync: 43 Einträge, 19 exakte
   `manifest-skip:verified-local-import`-Entscheidungen, 24 verbleibende
   Dateien gepullt, 150.637 Bytes, 24 erfolgreich, 0 fehlgeschlagen.
   Erneuter Stopp vor einer separaten Transaktion.

Damit wurde der Skip-Pfad am Gerät praktisch für 19 bereits importierte FITs
bestätigt; unbekannte, nicht importierte oder nicht exakt passende Einträge
blieben im Pull-Pfad. Dies validiert die Implementierung für diese 43
beobachteten Einträge, nicht die Vollständigkeit der Gerätehistorie oder
künftige Firmware. Es wurden keine Einzel-UIDs, Geräteadressen, Roh-Payloads
oder Hashes dokumentiert.
