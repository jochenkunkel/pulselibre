# CIRQA 5050 SYNC-A/B: Sicherheits- und Freigabegate (2026-11-17)

## Ziel und Status

Ziel ist die **Hypothese**, dass ein minimal gesetztes Host-SYNC-Bit (Capability-Index 3) nach der 5050-Konfiguration einen geräteinitiierten GFDI-5037-Synchronisationsstart ermöglicht. **Nicht nachgewiesen.** Ein A/B-Gerätetest ist **noch nicht implementiert oder freigegeben**. Die Offline-Vergleichslogik `Cirqa5050SyncHypothesis` verändert genau Bit 3 gegenüber der bisherigen 15-Byte-Nullmaske; `Cirqa5037OfflineSynchronization` prüft lediglich die Struktur eines hypothetischen 5037-Bodys.

## Nachweise vor einem aktiven Versuch

1. Erfolgreicher CI-Lauf **auf dem Commit**, der die Offline-5037- und A/B-Tests enthält; insbesondere Unit-Tests für strikte Längen, Bitposition und CRC.
2. Unabhängige Protokollreferenzen belegen nur Bitindex 3 und die Struktur, **nicht** die CIRQA-Reaktion. Diese Evidenzgrenze bleibt im Testdialog sichtbar.
3. Aktive Testfunktion müsste **gesondert implementiert** werden, opt-in und mit Bestätigungsdialog, expliziter Auswahl von A/B, einmaliger Antwortfolge, Zeitlimit, begrenzter Retry-Zahl und Stop bei negativen ACKs. **Kein** automatischer Wechsel auf Variante B, keine Hintergrundausführung.
4. Vor Start Garmin Connect schließen und den PulsLibre-Echtzeit-/Nacht-Recorder stoppen; konkurrierende GATT-Sitzungen vermeiden. Keine Änderung an Pairing oder Geräteinstellungen.
5. Diagnose nur als redigierte Metadaten (Nachrichtentyp, CRC-Gültigkeit, Anzahl, Zeitabstände, ACK-Status) ohne Rohpayloads, Geräteadressen oder Gesundheitswerte.

## Erfolg, Misserfolg und Stop

- **Erfolgskriterium des Experiments:** CRC-validierter eingehender GFDI-5037-Frame oder eindeutig dokumentierter anderer Synchronisationsstart **nach** positivem 5050-ACK, im Vergleich zur A-Kontrolle. Auch dies ist noch **kein** Historien-Download.
- **Negativ:** Nur 5000-ACK für 5050 und danach Stille; weitere Untersuchungen statt erzwungener FileListRequest.
- **Sofortiger Abbruch:** fehlgeschlagene GATT-Schreibbestätigung, negative GFDI-Antwort, nicht erwarteter Nachrichtentyp, verlorene Verbindung, wiederholte 5050-Requests, unerwartete Datenflut oder Zeitlimit.
- **Nicht senden:** proaktive 5037, 5007-Filter, FileListRequest, FileRequest, FileSetFlags, CLOSE_ALL, Reset oder beliebige Capability-Bitmasken.

## Entscheidungspunkt

Erst nach erfolgreichem CI und erneuter Prüfung des aktiven BLE-Schreibpfads den Nutzer ausdrücklich um Zustimmung zum **einmaligen** SYNC-Bit-3-Gerätetest bitten. Diese Datei ist eine **Planung**, keine vorweggenommene Freigabe. Der bisherige A-Test wurde bereits mehrfach durchgeführt; nicht ohne neuen Erkenntnisgewinn wiederholen.

## Implementierungsnachtrag: Offline-Sicherheitsentscheidung

`CirqaSyncExperimentGate` und `CirqaSyncExperimentGateTest` modellieren eine **rein lokale**, fehlersichere Entscheidung: ohne ausdrückliche Zustimmung, gestoppten Recorder, bestätigten GATT-Schreibabschluss und positiven 5050-ACK ist kein Beobachtungserfolg möglich. Negative Antwort, unerwartetes Frame, Verbindungsverlust, wiederholte 5050-Anfrage oder Überschreitung von 10 Sekunden führen zu `STOP`. Ein CRC-validierter 5037-Eingang wird nur als *Sync-Signal* klassifiziert, nicht als Dateidownload. Die Policy ist **noch nicht** an den BLE-Callback angeschlossen und bewirkt dort folglich noch keinen automatischen Abbruch; sie ist Vorarbeit, kein bereits aktiver Schutz.

**Codeprüfungsbefund:** Der bestehende Protokollfehler-Abbruch in `CirqaMlHandshakeProbe` ist derzeit an `fileListRequestMode` gebunden, nicht an `configurationTestMode`. Vor einer aktiven SYNC-Variante müssen sämtliche Stop-Bedingungen tatsächlich mit dem BLE-Lebenszyklus verdrahtet und getestet werden. Der vorhandene `startConfigurationTest` bleibt bei der 15-Byte-Nullmaske; keine SYNC-Bit-Übertragung wurde implementiert.

## Zweiter Implementierungsnachtrag: Ereignisreihenfolge (2026-11-17)

`CirqaSyncDiagnosticSession` ist eine weitere **rein offline** Zustandsmaschine mit den Zuständen `NOT_AUTHORIZED`, `WAITING_FOR_5050_ACK`, `OBSERVING`, `SYNC_SIGNAL` und `STOPPED`. Nur nach Freigabe, bestätigtem Schreibvorgang und positivem 5050-ACK kann ein CRC-validierter 5037-Eingang als Signal gelten. Wiederholte Bestätigungen, negative Antworten, unerwartete Frames, Verbindungsverlust und Timeout stoppen den Ablauf. Unit-Tests für diese Reihenfolge liegen unter `app/src/test/java/de/pulselibre/CirqaSyncDiagnosticSessionTest.kt`.

**Integration noch offen:** Weder diese Zustandsmaschine noch `CirqaSyncExperimentGate` sind in den Android-GATT-Callback verdrahtet. Die Ereignisse `AUTHORIZE_WITH_RECORDER_STOPPED` und `CRC_VALID_5037` müssen vor Gerätefreigabe an echte, unabhängig überprüfte Bedingungen gebunden werden. Der vorhandene `CirqaMlHandshakeProbe` verwendet weiter nur die Nullmaske. CI für die jüngsten Commits muss separat geprüft werden. Kein aktiver SYNC-Test und kein Historien-Download nachgewiesen.

## Bestehende Nullmasken-Diagnose abgesichert (2026-11-17)

Der aktive `CirqaMlHandshakeProbe` wurde **ohne neue BLE-Befehle** angepasst: Bei einer negativen GFDI-Protokollantwort (`fileListProbe.responseErrors > 0`) während `configurationTestMode` wird die Diagnose jetzt sofort über `finish(...)` beendet. Im Abschlussbericht wird außerdem die Anzahl passiv erkannter `gfdi-5037`-Frames ausgewiesen. Diese Zahl ist eine **Frame-Typ-Zählung**, keine vollständig decodierte 5037-Dateitypmaske und kein Nachweis eines erfolgreichen Downloads. Die 15-Byte-Nullmaske bleibt unverändert.

**Noch offen:** Die neue Offline-Zustandsmaschine ist weiterhin nicht an GATT angebunden; ein separater SYNC-Bit-3-Modus existiert nicht. Die vorhandene Nullmasken-Diagnose hat keine neue Nutzerfreigabe erhalten und wurde nicht am Gerät ausgeführt. Vor einer neuen Gerätevariante müssen die noch fehlenden Stop-Gates und die Freigabe geprüft werden.

## Dritter Implementierungsnachtrag: 5050-Retry-Abbruch

Im bestehenden **Nullmasken**-`configurationTestMode` wird jetzt unmittelbar beim Einplanen der 5050-Antwort die Anzahl bereits empfangener `gfdi-5050`-Frames festgehalten. Kommt danach eine weitere 5050-Anfrage, beendet `CirqaMlHandshakeProbe` die Sitzung über `finish` statt erneut zu verhandeln. Die Vergleichslogik ist als `Cirqa5050RetryGuard` isoliert und mit Unit-Tests abgedeckt. Die vorhandene 5050-Nullmasken-Antwort und der normale Recorder werden nicht verändert.

**Grenze:** Das Zählverfahren wertet vollständige Frames nach dem Queue-Zeitpunkt aus und unterscheidet keine Gerätesequenznummern. Der aktive SYNC-Bit-3-Versuch bleibt gesperrt. CI-Erfolg für diesen Commit muss geprüft werden.

## Vierter Implementierungsnachtrag: 5050-ACK-Reihenfolge

Der aktive **Nullmasken**-Diagnosepfad erkennt ein positives GFDI-5000-ACK für 5050 nun nur dann als `configurationAckReceived`, wenn die gesamte 5050-GATT-Schreibfolge bestätigt ist. Die reine Regel `Cirqa5050AckGate` wird sowohl beim Eintreffen des ACK als auch beim letzten GATT-Schreibcallback ausgewertet, damit ein früh eingetroffenes ACK nicht verloren geht. Unit-Tests prüfen alle acht booleschen Kombinationen und die frühe ACK-Reihenfolge. **Wichtig:** Das Gate wertet den bisherigen kumulierten Status des passiven Decoders aus; es beweist nicht die Sequenznummern-Zuordnung oder die Herkunft eines ACKs aus einer bestimmten Sitzung. Ein SYNC-Bit-3-Gerätetest ist weiterhin nicht freigegeben oder implementiert.

## Fünfter Implementierungsnachtrag: Frische ACK-Evidenz

Die Nullmasken-Diagnose speichert beim Einplanen ihrer 5050-Antwort die bisherige Zahl CRC-validierter `GFDI-5000 ACK(request=5050)` aus `GarminFileListProbe.responseStatuses`. Erst wenn diese Zahl anschließend steigt **und** die komplette GATT-Schreibfolge bestätigt ist, wird `configurationAckReceived` gesetzt. `Cirqa5050AckEvidence` und zugehörige Tests sichern die Zählung gegen alte und fremde Statusmeldungen ab. Damit wird eine vor dem Queue-Zeitpunkt beobachtete ACK-Nachricht nicht mehr als Bestätigung der aktuellen Antwort fehlinterpretiert. Einschränkung: Eine nach dem Queue-Zeitpunkt empfangene, verspätete ACK-Nachricht lässt sich ohne geprüfte Sequenzkorrelation nicht eindeutig einem einzelnen Sendeversuch zuordnen. Es gibt weiterhin keinen SYNC-Bit-3-BLE-Test.

## Sechster Implementierungsnachtrag: passiver 5037-Strukturdecoder

Der bestehende `GarminFileListProbe` prüft jetzt nach erfolgreicher COBS-Dekodierung, GFDI-Längenprüfung und CRC-Validierung zusätzlich eingehende GFDI-5037-Bodies mit `Cirqa5037OfflineSynchronization.inspect`. Er zählt gültige und ungültige 5037-Strukturen getrennt und erfasst nur die **Anzahl** gesetzter Maskenbits. Die 5050-Diagnose zeigt diese Zähler ebenfalls an. Synthetische Unit-Tests prüfen einen gültigen 5037-Body, einen strukturell ungültigen Body und einen CRC-Fehler. Keine automatischen 5037-Antworten, Filter oder Dateianfragen wurden hinzugefügt.

**Evidenzgrenze:** Ein strukturell gültiger 5037-Body ist noch kein Nachweis eines historischen Downloads oder der semantischen Richtigkeit der Dateitypbits. Die zuvor dokumentierten Frame-Typ-Zähler bleiben getrennt. Der SYNC-Bit-3-Test ist weiterhin nicht aktivierbar.

## Siebter Implementierungsnachtrag: 5037-Evidenzfenster

Im bestehenden **Nullmasken**-Diagnosepfad startet das passive 5037-Evidenzfenster erst dann, wenn sowohl die gesamte 5050-GATT-Schreibfolge als auch ein frisches positives 5050-ACK bestätigt wurden. Genau zu diesem Zeitpunkt wird die bisherige Anzahl strukturell gültiger, CRC-geprüfter 5037-Nachrichten als Baseline gespeichert. Erst eine danach neu eintreffende gültige 5037-Struktur kann im Abschlussbericht als `frisches 5037-Signal nach bestätigtem 5050` erscheinen. Der Offline-Guard `Cirqa5037SyncEvidenceGate` und Unit-Tests prüfen die Bedingungen. Eine vorzeitige 5037-Nachricht wird bewusst **nicht** nachträglich als Nachweis gewertet.

Das ist **kein** historischer Datenabruf und kein Beleg, dass CIRQA die SYNC-Bit-3-Hypothese unterstützt. Die eigentliche `CirqaSyncDiagnosticSession` ist noch nicht mit GATT verbunden; es wurden keine neuen BLE-Befehle eingeführt.

## Achter Implementierungsnachtrag: Grenze für ungültige GFDI-Frames

Die vorhandene aktive **5050-Nullmasken-Diagnose** speichert beim Einplanen ihrer Antwort die bisherige Zahl von COBS-, Längen- und CRC-Fehlern aus `GarminFileListProbe.rejectedFrames`. Ab **drei neuen** abgewiesenen GFDI-Frames wird die Sitzung über `finish` beendet und GATT anschließend geschlossen. Frühere Fehler werden nicht nachträglich dem aktuellen 5050-Versuch zugerechnet. Die Regel `Cirqa5050MalformedFrameGuard` ist offline mit Grenzwert- und ungültigen Zählerfällen getestet. Der SYNC-Bit-3-Versuch bleibt deaktiviert; es wurden keine neuen GFDI-Kommandos ergänzt.

## Neunter Implementierungsnachtrag: negative Statusantworten zeitlich abgrenzen

Die aktive Nullmasken-Diagnose bricht weiterhin bei **jeder negativen GFDI-Antwort vor** dem Einplanen der 5050-Antwort ab. Ab dem Queue-Zeitpunkt zählt sie negative Antworten relativ zum dort gespeicherten `responseErrors`-Ausgangswert und stoppt bereits bei der ersten neu hinzugekommenen negativen Antwort. Der Offline-Guard `Cirqa5050NegativeResponseGuard` und Unit-Tests prüfen diesen Zählervergleich. Diese Abgrenzung vermeidet falsche Zuordnung früherer Fehler, ist jedoch **keine** echte Sequenznummernkorrelation. Keine Änderung an 5050-Nullmaske oder BLE-Sendefolge; SYNC-Bit-3 bleibt gesperrt.

## Zehnter Implementierungsnachtrag: SYNC-Signal beendet keinen Sicherheitsabbruch

In der **offline-only** `CirqaSyncDiagnosticSession` bleibt `signalObserved` nach einer im Zustand `OBSERVING` beobachteten `CRC_VALID_5037`-Meldung als reine Evidenz erhalten. Jede nachfolgende Zustandsmaschinen-Nachricht (insbesondere `TIMEOUT` und `CONNECTION_LOST`) führt nun auch aus `SYNC_SIGNAL` nach `STOPPED`, statt den Abbruch zu ignorieren. Tests decken beide Fälle ab. Die Zustandsmaschine ist weiterhin **nicht** an GATT angebunden und schließt selbst keine Bluetooth-Verbindung. Die Ereignisbezeichnung `AUTHORIZE_WITH_RECORDER_STOPPED` ist keine unabhängige Prüfung des Recorder-Zustands: Vor einer Gerätefreigabe muss die aufrufende Schicht sowohl die ausdrückliche Zustimmung als auch den Recorder-Stopp nachweislich erzwingen. Ein empfangenes 5037 ist kein Nachweis eines abgeschlossenen Downloads.

## Elfter Implementierungsnachtrag: BLE-Notification-Flutgrenze

Die bestehende aktive **5050-Nullmasken-Diagnose** zählt jetzt alle eingehenden BLE-Notifications relativ zu dem Zeitpunkt, an dem die 5050-Antwort eingeplant wurde. Ab **120 neuen Notifications** beendet `CirqaMlHandshakeProbe` die Diagnose über `finish(...)`; das verhindert auch bei nicht als GFDI erkannten Paketen eine unbegrenzte Beobachtung bis zum Timeout. `Cirqa5050NotificationFloodGuard` prüft die Grenze offline, behandelt ungültige Zähler als Abbruch und verwendet `Long` für die Differenz, um Integer-Überlauf zu vermeiden. Die vorhandene Zeitschranke bleibt bestehen. Es werden **keine zusätzlichen BLE-Nachrichten gesendet**; die Hostmaske bleibt 15 Bytes mit allen Bits 0. Der aktive SYNC-Bit-3-Versuch bleibt gesperrt.

**Prüfstatus:** Code und Unit-Tests committed; CI und Gerätetest müssen für den aktuellen Commit separat bestätigt werden. Die Schwelle 120 ist ein konservatives Sicherheitslimit für die Diagnose, kein aus CIRQA-Firmware abgeleiteter Protokollwert.

## Zwölfter Implementierungsnachtrag: fehlerhafte 5037-Struktur sofort stoppen

Die aktive **5050-Nullmasken-Diagnose** übernimmt jetzt beim Einplanen der Hostantwort einen Baseline-Zähler für strukturell ungültige GFDI-5037-Synchronisationsnachrichten. Jede **neu** beobachtete fehlerhafte 5037-Struktur führt zu `finish(...)` und Verbindungsabbau; der Baseline-Vergleich verhindert, dass ältere Decoder-Beobachtungen als neue Fehler zählen. Die Offline-Policy `Cirqa5050Malformed5037Guard` ist durch Unit-Tests abgedeckt und behandelt ungültige Zähler als Abbruch. Dies ist eine konservative Diagnose-Sicherheitsentscheidung, keine Bestätigung eines CIRQA-5037-Protokollformats. Es werden keine zusätzlichen BLE-Befehle gesendet, die 15-Byte-Nullmaske bleibt unverändert und SYNC-Bit 3 bleibt deaktiviert. **CI-Erfolg und Gerätetest für den neuen Commit sind noch separat zu bestätigen.**

## Dreizehnter Implementierungsnachtrag: eindeutige 5050-Evidenzklassifikation

Der CIRQA-Historien-Diagnosebericht zeigt jetzt zusätzlich einen konservativen `5050-Evidenzstatus` aus der separat getesteten Offline-Policy `Cirqa5050Outcome`. Die fünf Zustände unterscheiden: keine eingeplante Antwort, unvollständige GATT-Schreibfolge, kein frisches 5050-ACK, ACK ohne neues gültiges 5037-Signal und gültiges 5037-Signal nach bestätigtem Handshake. **Auch ein gültiges 5037-Signal ist weder ein Dateilistenbeweis noch ein Historien-Download.** Die Klassifikation verändert keine BLE-Schreibvorgänge. Die aktive SYNC-Bit-3-Hypothese bleibt deaktiviert. CI für diese Änderungen ist separat abzuwarten.

## Vierzehnter Implementierungsnachtrag: sofortige Abbruchverriegelung

Ein Sicherheitsabbruch wurde bisher in `finish(...)` ausschließlich zur späteren Bearbeitung auf den Android-Main-Handler gestellt. Bis zur Ausführung dieses Tasks konnten weitere BLE-Callbacks oder bereits eingeplante Sendeaufträge eintreffen, während `active` noch `true` war. `CirqaMlHandshakeProbe` setzt deshalb jetzt **vor** dem Einplanen des Cleanup-Tasks die volatile Verriegelung `terminationRequested`. Eingehende Notifications, GATT-Schreibbestätigungen, GATT-Verbindungs-/Discovery-Callbacks und weitere ML-Sendeversuche werden ab diesem Zeitpunkt verworfen bzw. nicht mehr ausgeführt. Der bestehende Cleanup schließt GATT und gibt die Session-Gate frei. `close()` verriegelt ebenfalls sofort; `start()` setzt die Verriegelung für eine neue Sitzung zurück. Die 5050-Nullmaske bleibt unverändert; SYNC-Bit 3 bleibt deaktiviert. Diese Änderung benötigt weiterhin CI-Bestätigung und einen gesonderten Gerätetest.

## Fünfzehnter Implementierungsnachtrag: passive 5050-Zustandsverfolgung in Android-BLE

`Cirqa5050PassiveSession` ist eine neue, separat unit-getestete Zustandsverfolgung für den **bestehenden** 15-Byte-Nullmasken-Test. `CirqaMlHandshakeProbe` verbindet sie mit dem realen Ablauf: Antwort einplanen → vollständige GATT-Schreibbestätigung → frisches 5050-ACK → gültiges 5037-Signal. Früh beobachtete 5037-Strukturen zählen nicht als nachgelagertes SYNC-Signal; ein ungültiger Zustandsübergang verriegelt die passive Session. Beim Diagnoseabbruch wird die Session gestoppt, während `signalObserved` als reine Evidenz erhalten bleibt. Der Abschlussbericht weist den Passivzustand und die beobachtete Evidenz separat aus. **Dies ist nicht die Freigabe der experimentellen `CirqaSyncDiagnosticSession` und kein Historien-Download.** Es werden weder SYNC-Bit 3 noch weitere Dateibefehle gesendet. Der zugehörige Android-CI-Lauf muss für den neuen Stand erfolgreich sein, bevor ein Testbuild empfohlen wird.
