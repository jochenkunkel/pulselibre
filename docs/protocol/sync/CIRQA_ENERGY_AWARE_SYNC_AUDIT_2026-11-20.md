# Akku- und Historien-Sync: statische Ist-Aufnahme

Stand: 2026-11-20 · Issue: [#171](https://github.com/jochenkunkel/pulselibre/issues/171)

Dies ist ein **statischer Quellcodebefund**, keine Akku-Messung und kein
Hardware-/BLE-Test. Geprüft wurde der aktuelle `main`-Stand
`642ecac5c9bdb47a2a110f638fd6d4a3324aa66d`.

## Tatsächliches Verhalten im Quelltext

| Pfad | Befund |
| --- | --- |
| Historischer Download | Einziger Produkt-Callsite ist der Button „Historie herunterladen“ mit Bestätigungsdialog in `MainActivity.kt` → `CirqaMlHandshakeProbe.startHistoricalDownload(userConfirmed = true)`. Kein automatischer Aufruf in `onResume`, beim Tabwechsel oder über einen Timer gefunden. |
| Laufzeit des Produkt-Syncs | `startSyncRequest` begrenzt die Produkt-Sitzung über `CirqaSyntheticDownloadLimits.MAX_SESSION_MILLIS` auf 120 s; parallele Probe-/Sync-Sitzungen werden abgelehnt. Der UI-Text warnt, dass die Sync-Anfrage das Band veranlassen kann, Daten als übertragen zu markieren. |
| App-Resume / UI-Polling | `MainActivity.onResume()` startet `dashboardPoll`; `onPause()` stoppt ihn. Der Handler läuft im Vordergrund alle 1 s und aktualisiert Verbindungs-/Recorderstatus. DB-`latestEventId` wird nur auf Übersicht/Verlauf gelesen; Rendern erfolgt bei geänderter Event-ID oder spätestens nach 30 s. Das ist UI-Polling, kein BLE-Scan und kein historischer Download. |
| Automatischer Recorderstart | Separate Funktion: bei frischem App-Start nur, wenn die Opt-in-Einstellung aktiviert ist und Bluetooth/Berechtigungen/Zustand passen. Sie startet den kontinuierlichen Vordergrunddienst, nicht den historischen Download. |
| Kontinuierliche Aufzeichnung | Explizit gestarteter Foreground-Service mit offener GATT-Notification-Session. Reconnects bei Ende/Fehler nach 2, 4, 8, 16, 32, dann 60 s; weitere Versuche bleiben auf 60 s gedeckelt, solange der Dienst läuft. Der Zähler steigt über erfolgreiche Sitzungen hinweg weiter und wird bei Erfolg nicht zurückgesetzt. Das ist ein zu messender Akku-Kandidat, keine bestätigte Ursache. |
| GATT-Sitzungsbesitz | `CirqaBleSessionGate` bietet bereits process-lokale exklusive Probe-Besitznahme; `CirqaMlHandshakeProbe.start()` erwirbt sie vor dem Verbindungsstart und gibt sie beim Ende frei. Ein zukünftiger Coordinator sollte diese vorhandene Sperre integrieren, nicht eine konkurrierende zweite Lock-Quelle schaffen. |
| BLE-Scan in `MainActivity` | Es existiert eine 60-s-Scanmethode mit Stop bei Activity-Zerstörung; im geprüften `MainActivity`-Quelltext wurde kein Aufrufer dieser Methode gefunden. Daher ist daraus kein laufender automatischer Scan abzuleiten. |
| Hintergrundplanung | In den geprüften App-Quellen wurde kein WorkManager-, `PeriodicWorkRequest`-, AlarmManager- oder JobScheduler-Auftrag für historischen Sync gefunden. |
| Lokaler FIT-Backfill | Beim Erstellen der Main-Activity werden bereits lokal verifizierte FIT-Dateien offline und idempotent nachverarbeitet. Der Code kommuniziert dabei nicht mit dem Band und ist kein Geräte-Download. |

## Messlage und offene Risiken

- Issue #235 ist geschlossen, aber die Messung wurde laut Issue-Kommentar nicht
  durchgeführt. Es gibt keine belastbare %/h- oder mAh/h-Baseline für Handy oder
  Band.
- `tools/battery_baseline.py` misst nur das Handy per read-only ADB. Das Band
  muss aus einer validierten Anzeige/Quelle separat von Hand erfasst werden.
- `docs/science/BATTERY_MEASUREMENT_PROTOCOL.md` fordert pro Betriebsart mindestens
  drei vergleichbare, jeweils mindestens 60-minütige Fenster, idealerweise
  mehrere Stunden. Snapshots bleiben außerhalb des Repositories.
- Noch zu untersuchen: genaue Dashboard-Verbindungsabfragen und ihr Aufwand,
  tatsächliche Recorder-Laufzeit/Retry-Verteilung, Verhalten bei Verbindungsende,
  tatsächliche Download-/Importdauer sowie BLE-Sitzungskonkurrenz.
- Der empirische Sync-Erfolg für einzelne begrenzte Dateilisten-/Downloadläufe
  beweist keine vollständige CIRQA-Historie oder serverseitige Zeit-/Cursor-
  Filter. Geräte-seitige Delta-Filter bleiben bis zu Evidenz aus #148 aus.

## Nächste sichere Arbeitsschritte

1. Ergänzender statischer Callgraph-/Test-Audit, insbesondere GATT-Abbruch,
   Recorder-Retry nach erfolgreichem Empfang, Tab-/Activity-Lifecycle und
   Download-Checkpoint-Semantik.
2. Eine redigierte Messmatrix für PulsLibre aus / an, Vordergrund, Hintergrund,
   Recorder, manuellen Sync und Screen-off erstellen. Keine Messung im
   Ladebetrieb oder mit Buildwechsel vermischen.
3. Nach Verfügbarkeit eines freigegebenen S25-Testfensters Baseline nach dem
   bestehenden Protokoll erheben; keine neue BLE-Aktion für die Messung.
4. Erst nach Baseline und Regressionstests kleine, einzeln messbare Änderungen
   priorisieren. UI-Polling mit dem GUI-Agenten abstimmen; Recorder-Backoff nicht
   gleichzeitig mit Sync-Änderungen verändern.
5. Historische Automatik erst aktivieren, wenn Session-Single-Owner,
   Frische-/Debounce-Regeln, Manifest-Dedupe, Wiederaufnahme nach Abbruch und
   Datenintegrität automatisiert getestet und Akku-Nachhermessungen belegt sind.

## Testnachweis dieses Audits

`PYTHONPATH=tools python3 -m unittest test_battery_baseline -v` – 6 Tests
bestanden. Es wurden keine ADB-Befehle, BLE-Kommandos oder Hardwaretests
ausgeführt.
