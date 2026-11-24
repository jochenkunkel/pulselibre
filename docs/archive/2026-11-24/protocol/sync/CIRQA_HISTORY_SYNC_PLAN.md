# CIRQA-Historien-Download: Vier-Schritte-Implementierungsplan

**Stand:** 2026-11-16 · **Status:** Schritt 1 begonnen (Offline-Framing), CIRQA-Historienzugriff noch nicht nachgewiesen · **Branch:** main

## Ziel und Ausgangslage

PulsLibre empfängt am Galaxy S25 bereits reale CIRQA-Echtzeitwerte über Garmin Multi-Link. Die kanonische lokale Datenbank ist `cirqa_measurements.db` (`CirqaDatabase`). Historische FIT-/FileSync-Daten sind **noch nicht** erfolgreich vom CIRQA übertragen worden. Die FIT-Hüllen- und Strukturprüfer sind Vorarbeiten, kein fertiger Historien-Decoder. Garmin Connect darf nicht unbemerkt gestört werden; weder Dateien auf dem Band löschen noch übertragen/archiviert markieren.

Referenzen für unabhängige Protokollrecherche: [Gadgetbridge Garmin-Protokoll](https://gadgetbridge.org/internals/specifics/garmin-protocol/), [gwerneckp/garmin-ble](https://github.com/gwerneckp/garmin-ble), [wh1le/garmin-bridge](https://github.com/wh1le/garmin-bridge), [GarminHRM600](https://github.com/mskerwiderski/garminhrm600). Geräte- und Firmware-Kompatibilität ist jeweils offen; AGPL-Lizenzpflichten vor Codeübernahme prüfen.

## Schritt 1 – Protokollweg nachweisen (P0)

- Bestehende Garmin-ML-Verbindung und GFDI-Framing mit Offline-Tests auf Länge, CRC, Fragmentierung, Zeitlimit und unerwartete Antworten erweitern.
- Klassische GFDI-Dateiliste/FIT-Nachrichten (u. a. 5011/5012) und Protobuf/FileSync als alternative Kandidaten vergleichen.
- Eine **explizit nutzerinitiierte**, zeitlich begrenzte, möglichst nur lesende Fähigkeitsabfrage implementieren; kein CLOSE_ALL als Automatismus, kein Geräte-Reset, keine Dateiänderung.
- **Abnahme:** S25-Diagnose belegt einen tatsächlich unterstützten Historien-/Dateilistenpfad oder dokumentiert reproduzierbar, dass dieser derzeit nicht nachgewiesen ist. Keine bloße Annahme aus Fremdprojekten.

### Implementierungsstand Schritt 1

- `GarminHistoryFrameStream.kt` ergänzt: begrenzte, fragmentierte und verkettete GFDI-Frames werden erst nach Längen- und CRC-Prüfung ausgegeben; Fehler führen zu einem expliziten Reset-Bedarf.
- `GarminHistoryFrameStreamTest.kt` ergänzt: Fragmentierung, mehrere Frames, beschädigte CRC, ungültige Länge, Größenbegrenzung.
- **S25-Diagnose implementiert:** Auf „Geräte“ → „CIRQA: Historien-Diagnose (Phase 1)“ wird nach ausdrücklicher Bestätigung eine maximal 20 Sekunden dauernde GFDI-ML-Registrierung ausgeführt. Ergebnis enthält Phasen, Registrierungsstatus, GATT-Schreibstatus, Anzahl Notifications und höchstens zwölf redigierte Paketform-Metadaten. Der Dialog bietet „Bericht teilen“, daneben „Letzte Historien-Diagnose teilen“. Keine Datei-Anfrage und kein automatisches CLOSE_ALL; Garmin Connect vorher schließen.
- **Noch offen:** tatsächliches GFDI-Transportframing (ggf. COBS), FileSync-Protobuf und kontrollierte CIRQA-Dateilistenabfrage. Keine BLE-Verbindung oder Historien-Downloads durch diese Änderung.
- **Verifikation ausstehend:** CI-/Android-Build und S25-Hardwaretest; die Tests sind geschrieben, nicht als erfolgreich ausgeführt behauptet. **Phase 1 ist erst abgeschlossen, wenn eine echte CIRQA-Dateilistenantwort nachgewiesen oder der nicht unterstützte Pfad nachvollziehbar diagnostiziert wurde.**


### S25-Hardwarebefund vom 16.11.2026

Die vom Nutzer ausgeführte 20-Sekunden-Diagnose bestätigt: GFDI-Dienst 1 registriert (Status 0, Handle 32), GATT-Schreiben bestätigt, 76 BLE-Notifications, Handshake-Zustand COMPLETE. Die ersten zwölf Paketformen umfassen kurze Datenpakete und eine Registrierungsantwort. **Keine Dateiliste angefordert; kein Historienzugriff oder Download nachgewiesen.**

**Nächste Implementierungsaufgabe:** ML-Datenpakete anhand des dynamischen Handles 32 von Steuerantworten trennen, Framing/Fragmentierung und CRC anhand des echten CIRQA-Transports verifizieren, erst dann einen dokumentierten, begrenzten und explizit bestätigten Dateilisten-Lesebefehl implementieren. Keine unbekannten Kommandos auf Verdacht senden; keine Dateiänderungen, kein CLOSE_ALL.


### Geräteoberfläche / Debug-Zugang (16.11.2026)

Die Geräteseite zeigt regulär den CIRQA-Recorder mit zwei nebeneinander angeordneten Aktionen (Start/Stop) und einem aus dem Foreground-Service-Lebenszyklus gespeisten Status. Der Recorder ist **periodische Echtzeit-Abfrage** (ca. 20 Sekunden alle 120 Sekunden), kein kontinuierlicher Rohdatenrecorder und kein Historien-Download. Die alten BLE- und Historien-Diagnoseaktionen bleiben als Debug-Funktionen im `MainActivity`-Quellcode erhalten und werden durch **langes Drücken auf „Bluetooth · Geräte“** in der laufenden Sitzung sichtbar. Die Diagnose ist nicht gelöscht. Service-Status: `CirqaOvernightService.PREFS` / `KEY_RUNNING`; nach Service-Start und -Stopp aktualisiert. Bei unerwartetem Prozessabbruch ist ein persistierter Status kein verlässlicher Beweis für eine aktive Serviceinstanz; für Produktionsreife zusätzlich Heartbeat/Liveness vorsehen.


### Zweiter S25-Diagnosebefund (16.11.2026)

Wiederholte GFDI-Registrierung erfolgreich: Service 1, Status 0, Handle 32, GATT-Schreiben bestätigt, 39 Notifications, Zustand COMPLETE. Von 38 Nicht-Registrierungs-Paketen wurden anhand des ersten Bytes vier dem Handle 32 zugeordnet, 34 nicht. Längenverteilung: 2 Byte × 2, 6 Byte × 26, 7 Byte × 6, 58 Byte × 4. Die Zuordnung nach dem ersten Byte ist eine **vorläufige Heuristik**, kein Beweis, dass die übrigen Pakete anderen Diensten gehören. Paketlängen beweisen weder GFDI-Framing noch FIT-Dateien. Keine Dateiliste angefordert.

**Implementierungsgate:** Als Nächstes ML-Paketstruktur und GFDI-Framing anhand der tatsächlich empfangenen Nachrichtentypen bzw. dokumentierter Referenzimplementierungen validieren; erst danach eine bestätigte, ausschließlich lesende Dateilistenabfrage implementieren. Die Phase 1 bleibt offen.


### Dritter S25-Befund: gleichzeitiger Echtzeit-Recorder (16.11.2026)

Nutzer meldet laufenden Echtzeit-Recorder während Historien-Diagnose: GFDI-Registrierung erneut bestätigt, Handle 32, 56 Notifications (1 Registrierung, 4 per First-Byte-Heuristik GFDI-zugeordnet, 51 andere), Protokollzustand COMPLETE. Längen: 2×2, 5×6, 6×24, 7×18, 9×1, 58×4. Vier GFDI-Pakete mit mindestens drei Bytes sind **nur unbestätigte MLR-Kandidaten**. Dateiliste weiterhin nicht angefordert.

**Risiko/Blocker:** Historien-Diagnose und `CirqaOvernightService` können konkurrierende GATT-Sitzungen starten. Erfolgreiche Dienstregistrierung beweist keine sichere parallele Nutzung. Vor aktivem Dateilistenabruf gemeinsame BLE-Sitzungskoordination/Mutual Exclusion mit Recorder-Pause und anschließendem Wiederanlauf implementieren; bestehende Messdaten nicht verändern. Erneute reine Metadaten-Diagnosen liefern keinen Nachweis für Historienzugriff.

## Schritt 2 – Eine Originaldatei vollständig herunterladen (P0)

- Metadaten/Dateiliste lesen; eine kleine Datei gezielt anfordern, Paketgrenzen, Wiederholungen und Abbruch sicher behandeln.
- Vollständigkeit über bekannte Dateigröße, Transport-/Dateiprüfsumme und FIT-CRC (falls FIT) verifizieren; begrenzte Dateigrößen und sichere Fehlerpfade.
- Originaldaten unverändert erhalten; keine Lösch-, Archivierungs- oder Transferbestätigungsbefehle mit unklarer Wirkung.
- **Abnahme:** Eine reale CIRQA-Historiendatei wird auf dem S25 reproduzierbar vollständig übertragen und lokal validiert; bei Abbruch keine fälschlich als vollständig markierte Datei.

## Schritt 3 – Dekodieren und in **dieselbe** SQLite-Datenbank importieren (P0)

- FIT-/Protobuf-Inhalte anhand validierter Felddefinitionen in getypte Messungen mit Gerätetimestamp, Empfangszeit, Einheit, Datenquelle und Granularität dekodieren.
- Originaldatei (als begrenztes BLOB oder kontrollierte interne Referenz) und Messungen transaktional in `cirqa_measurements.db` speichern; Schema-Migration **additiv**, bestehende Echtzeitdaten unverändert lassen.
- Stabile Datei-IDs/Hashes und Datensatzschlüssel für Idempotenz, keine doppelten Werte bei Wiederholung; fehlende Rohbewegung ausdrücklich als nicht verfügbar kennzeichnen.
- Dashboard und CSV ausschließlich aus der kanonischen Datenbank speisen.
- **Abnahme:** Erneuter Import derselben Datei erzeugt null Duplikate; Werte, Zeitstempel und Herkunft sind prüfbar; bisherige Echtzeitdaten und Exporte bleiben erhalten.

## Schritt 4 – Zuverlässige Synchronisation und S25-Abnahme (P1)

- Erst nach Schritt 1–3 einen einzelnen, gegenseitig ausgeschlossenen Sync-Job aktivieren: bei manuellem Start/App-Öffnen und **testweise alle 30 Minuten**, sofern Android-Hintergrundregeln es erlauben; kein 10-/20-Sekunden-Polling.
- Dateibasierte Fortschrittsmarke/Replay, begrenzte Wiederholungen mit Backoff, Bluetooth-aus-/Berechtigungs-/Abbruchbehandlung, keine gleichzeitige Live- und Historien-BLE-Sitzung.
- Android WorkManager oder gleichwertige OS-konforme Planung evaluieren: periodische Arbeit ist nicht minutengenau garantiert; auf dem Gerät Batterie- und Verbindungsverhalten messen.
- **Abnahme:** Nach Offline-Zeit werden nur fehlende Dateien nachgeladen; Neustart und Wiederholung sind verlustfrei; keine Duplikate, keine unbeabsichtigte Garmin-Connect-Störung; Build, Unit-Tests und S25-Feldtest dokumentiert.

## Reihenfolge und Stop-Gates

**1 → 2 → 3 → 4.** Ohne nachgewiesene Dateiliste kein Downloadversprechen. Ohne validierten Download keine Interpretation als historische Gesundheitsdaten. Ohne idempotenten Datenbankimport kein automatischer Job. Rohbeschleunigung, Schlafphasen und vollständige Historie sind **nicht zugesichert**: nur tatsächlich verfügbare, nachgewiesene Datensätze übernehmen.

**Verknüpfte Dokumente:** [Garmin-Protokollrecherche](../research/GARMIN_PROTOCOL_RESEARCH.md), [Roadmap](../../ROADMAP_TO_CIRQA_DATA.md), [einheitliche Speicherung](../../UNIFIED_LOCAL_STORAGE_2026-11-16.md), [Bewegung/Schlaf-Grenzen](../../../../protocol/data-model/CIRQA_SLEEP_MOTION_GROUND_RULES.md).

## Debugging-Freigabe vom 17.11.2026: synthetische CIRQA-Testdaten

Der Projektverantwortliche hat ausdrücklich erklärt, dass die Daten auf dem für das Debugging verwendeten CIRQA-Testgerät **synthetisch** sind, und den **vollständigen Download dieser Testdaten** zur Fehlersuche freigegeben. Der vorherige generelle Vorbehalt gegen den Download historischer Testdaten gilt damit für diesen Debugging-Kontext nicht mehr. Die Freigabe umfasst das gezielte Lesen und lokale Speichern aller vom Testgerät tatsächlich angebotenen Daten, soweit das Protokoll technisch verifiziert und sicher implementiert ist.

**Nicht mitfreigegeben** sind das Löschen oder Verändern von Daten auf dem Gerät, Geräte-Reset, CLOSE_ALL, Änderungen an Garmin-Connect-Kopplung, unkontrollierte Wiederholungsschleifen oder die Übertragung auf andere Geräte mit möglicherweise echten Gesundheitsdaten. Eine aktive 5050-SYNC-Capability-Aushandlung (einschließlich der bislang nur offline untersuchten Bit-3-Hypothese) darf für den ausdrücklich freigegebenen Debugging-Download vorbereitet und im kontrollierten Testpfad verwendet werden, sobald das konkrete Paketformat, die Abbruchbedingungen und die Sitzungsisolation validiert sind. Die bisherige Nullmasken-Diagnose darf dabei nicht stillschweigend verändert werden; ein eigener opt-in Debug-Modus ist erforderlich.

**Praktischer nächster Schritt:** den aktuellen erfolgreichen Build als Ausgangspunkt verwenden, Protokollsequenz und Decoder für die Dateiliste prüfen, dann einen begrenzten, protokollierten Debug-Download mit klaren Größen-/Zeitlimits und ausschließlich lokalen Artefakten implementieren. Der Download ist noch **nicht** als funktionierend nachgewiesen.

## Implementierungsstand: Budget für synthetische Debug-Downloads

`CirqaSyntheticDownloadLimits` definiert vor der Aktivierung eines vollständigen Transfers harte, offline getestete Grenzen: höchstens **256 Dateien**, **8 MiB je Datei**, **64 MiB insgesamt** und **120 Sekunden Sitzungsdauer**. Ungültige/negative Werte und Überläufe werden abgelehnt. Vier Unit-Tests prüfen diese Grenzfälle. Die Policy ist aktuell **noch nicht** an den BLE-Dateitransfer angeschlossen, weil der CIRQA-spezifische Dateilisten-/Dateianforderungsablauf zuerst belegt werden muss; diese Klasse allein lädt nichts herunter. Außerdem wurde der Zustandsübergang nach der letzten GATT-Schreibbestätigung idempotent gemacht, damit ein doppeltes Callback nicht die passive 5050-Session fehlerhaft stoppt. Die Freigabe synthetischer Daten ersetzt keine Protokollvalidierung und erlaubt keine geräteverändernden Befehle.

## Aktiver synthetischer SYNC-Debugpfad (Implementierungszwischenstand)

Im Debug-Menü gibt es jetzt den **separat bestätigungspflichtigen** Eintrag „CIRQA: synthetische SYNC-Aushandlung (experimentell)“. Dieser nutzt die vorhandene begrenzte 5024/5050-GATT-Sequenz, sendet jedoch **nur im neuen Debugmodus** eine 15-Byte-Hostmaske mit SYNC-Bit 3. Der normale 5050-Test bleibt bei 15 Nullbytes. Die bestehende Abbruch-/Timeout-/5037-Evidenzlogik bleibt aktiv. Der neue Test sendet **keinen FileRequest**, liest **noch keine Datei** und beweist keinen funktionierenden Download. Er soll zunächst am synthetischen Testgerät zeigen, ob CIRQA nach der Capability-Aushandlung ein gültiges 5037-Signal liefert. Erst danach kann die konkrete Dateiliste und der sequenzierte, begrenzte Dateiabruf implementiert werden. Ein Android-CI-Erfolg und ein manueller Gerätetest sind vor der Behauptung eines funktionierenden Downloads erforderlich.

## Gerätetest: synthetische SYNC-Bit-3-Konfiguration (17.11.2026)

Vom Projektverantwortlichen übermittelter vollständiger Diagnosebericht nach Installation des Debug-Builds: GFDI-Registrierung bestätigt (Handle 31); 5024 und 5050 empfangen; 5050-Antwort mit 15-Byte-Maske und ausschließlich SYNC-Bit 3 GATT-bestätigt; **frisches GFDI-5000-ACK für 5050**; keine negative Antwort, keine fehlerhaften Frames, keine 5050-Wiederholung. Im anschließenden Beobachtungsfenster **kein 5037**, kein 5011, keine Dateiliste und kein Dateiabruf. Decoder-Typen: 5024×1, 5050×1, 5000×1. Klassifikation: `ACK_WITHOUT_5037`. Das ACK bestätigt die Annahme der GFDI-Nachricht, **nicht** eine erfolgreiche Aktivierung des historischen Syncs.

**Folgerung:** Die Hypothese „allein SYNC-Bit 3 löst 5037 aus“ ist in dieser Testsequenz nicht bestätigt. Die frühere kompakte FileListRequest (0x8A39) wurde von CIRQA mit UNSUPPORTED abgelehnt; nicht unverändert erneut senden. Nächste Arbeit: gerätespezifische Bootstrap-Voraussetzungen (5024/5050, Sequenz-/Counterführung, eventuell weitere Host-Anwendungsnachrichten) anhand belegter Protokollquellen und passiver Evidenz prüfen. Kein Gerätetest ohne neue unterscheidungskräftige Hypothese. In `CirqaMlHandshakeProbe` wird ein 5037-Signal erst **nach** den Negativ-/Malformed-/Retry-Abbruchprüfungen in die passive Session übernommen. Historischer Download bleibt offen.
