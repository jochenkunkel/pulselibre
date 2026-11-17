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
