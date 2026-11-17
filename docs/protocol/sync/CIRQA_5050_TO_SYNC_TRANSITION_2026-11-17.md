# CIRQA: 5050 → 5037 → FileSync – quellengeprüfte Übergangsanalyse (2026-11-17)

## Evidenz aus dem Gerät

Drei gezielte S25-Tests zeigen nach GATT-bestätigter aktiver 5050-Antwort (15 Host-Capability-Bytes, alle 0) ein positives GFDI-5000-ACK für `5050`. Im letzten verlängerten Beobachtungsfenster folgt **kein** `5037`, `5011`, `5050`-Retry, FileListResponse oder Protobuf. Das Gerät hat damit die 5050-Antwort bestätigt, aber **keinen Historien-Download freigegeben**. Siehe `CIRQA_5050_THIRD_TEST_AND_PHASE_FIX_2026-11-17.md`.

## Primärquellen erneut im Quelltext geprüft

1. [Amazfish: Configuration](https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garminconfigurationmessage.cpp): `parse` erzeugt eine Host-5050-Nachricht mit `[u8 Länge][Host-Capabilities]`; `generateCapabilities` setzt bei 15 Bytes nahezu alle bekannten Bits und nimmt ausdrücklich bestimmte unbekannte Bits aus. Das ist eine **breite Fremdimplementierung**, keine sichere CIRQA-Vorlage. `completePairing()` wird nach dem Senden aufgerufen, beweist aber keinen Dateizugriff.
2. [Amazfish: Synchronization](https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garminsynchronizationmessage.cpp): `parse` verarbeitet **eingehende** `5037`-Nachrichten mit `[u8 syncType][u8 maskLength][4 oder 8 Byte LE fileTypeBitmask]`. Es sendet darauf ein GFDI-5000-ACK für `5037`; bei gesetzten Workout-/Activity-/Summary-/Sleep-Dateitypen folgt in dieser Implementierung ein GFDI-Filter (`5007`) mit Filtertyp 3. Der Kommentar markiert Teile als TODO; dies ist **kein** Beweis für einen vollständigen Download.
3. [GarminHRM600 FileSync-Vergleich](CIRQA_FILESYNC_REFERENCE_COMPARISON_2026-11-16.md): Früherer CIRQA-Versuch mit kompakter FileListRequest wurde dreimal als `UNSUPPORTED` (GFDI-5000-Status 2 für `0x8A39`) abgewiesen. Der Fehler kann am falschen Nachrichtentyp, Bootstrap oder an Capabilities liegen; aus dem Status ist die Ursache **nicht** bestimmbar.

## Abgeleitete Protokollhypothesen – noch unbewiesen

- **H1 – Null-Hostmaske blockiert Sync:** plausibel, weil SYNC-Bit 3 deaktiviert ist. Es ist jedoch nicht belegt, dass CIRQA den Start einer 5037-Nachricht an dieses Bit bindet.
- **H2 – Gerät initiiert 5037:** Amazfish behandelt 5037 als *eingehende* Nachricht; damit ist geräteinitiierte Synchronisation ein sinnvoller Beobachtungskandidat, **nicht** ein nachgewiesener CIRQA-Ablauf.
- **H3 – zusätzlicher Bootstrap erforderlich:** GarminHRM600 zeigt weitere ACKs, EventSharing und Sequenzzähler; ob CIRQA diese benötigt, ist unbekannt.
- **H4 – FileSync-Variante inkompatibel:** Ein vorheriger kompakter FileListRequest wurde als unsupported abgewiesen. Nicht blind wiederholen.

## Nächster begrenzter Implementierungsschritt

1. **Offline-only** Decoder/Tests für den dokumentierten 5037-Body (`syncType`, Längenbyte, 4-/8-Byte-Maske, Little Endian), strikte Längen-/Trunkierungsprüfung, keinerlei BLE-Übertragung. Erfassung ausschließlich von Typen, Längen und redigierten Zuständen.
2. Danach einen **explizit opt-in**, auf wenige Sekunden begrenzten 5050-Capability-A/B-Gerätetest **entwerfen**, der nur die belegte 5024/5050-ACK-Folge nutzt. Variante A: bekannte 15-Byte-Nullmaske (Kontrolle), Variante B: 15-Byte-Maske mit ausschließlich SYNC-Bit 3 (Hypothese). **B noch nicht aktivieren**, bis Risiken, Voraussetzungen, Benutzerbestätigung und Stop-Gates überprüft sind.
3. Beobachtungskriterium: Kommt eine CRC-validierte `5037`-Nachricht oder ein anderer klar identifizierbarer Sync-Start? Positives 5050-ACK allein ist kein Erfolg.
4. Erst nach gerätebestätigter Nachrichtensequenz eine ausschließlich lesende, gesondert freigegebene Dateilistenstrategie erarbeiten; kein `CLOSE_ALL`, `FileSetFlags`, FileRequest oder automatisches Archivieren/Löschen.

**Nicht erlaubt:** Die breite Amazfish-Hostmaske kopieren, beliebige 18-Byte-Bits setzen, proaktiv `5037` oder `5007` senden oder den früher abgewiesenen FileListRequest ungeprüft wiederholen.

## Koordination

Dieser Bereich gehört dem ChatGPT-Agenten gemäß `AGENTS.md`. Codex arbeitet parallel ausschließlich an Dashboard und wissenschaftlichen Auswertungen; keine gemeinsamen Kotlin-Dateien ohne Absprache ändern.

## Abgleich mit alternativen Implementierungen nach CIRQA-SYNC-Bit-3-Gerätetest

Quelltext erneut geprüft (öffentlich, MIT-basierter Referenzpfad):
- [GarminHRM600 `bootstrap.py`](https://github.com/mskerwiderski/GarminHRM600/blob/main/hrm600/bootstrap.py) enthält einen **52-Byte-5024-RESPONSE** mit fenix-8-Geräteidentität und weitere aufgezeichnete kompakte Bootstrap-Frames: Status-ACK für kompakte Nachricht, Session/Core-Preamble, FeatureCapabilitiesRequest, FeatureCapabilitiesResponse-ACK und Ready-Trigger. Diese Bytes stammen aus einer **HRM600/fenix-8-Wachemulation**, nicht von CIRQA. **Nicht kopieren oder aktiv senden.**
- [GarminHRM600 `client.py`](https://github.com/mskerwiderski/GarminHRM600/blob/main/hrm600/client.py) initialisiert kompakte Counter/Sequence mit `0x01EF`/`0x0A` und erhöht sie je Nachricht. PulsLibre verwendet für die bisher abgewiesene Dateiliste feste `1`/`10`; das ist ein **konkreter Protokollunterschied**, aber noch kein Nachweis für die Ursache von `UNSUPPORTED`.
- [Amazfish `garminconfigurationmessage.cpp`](https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garminconfigurationmessage.cpp) sendet eine breite 15-Byte-Hostmaske und ruft danach `completePairing()` auf; `garminsynchronizationmessage.cpp` behandelt 5037 als **eingehend** und antwortet dann mit ACK und gegebenenfalls Filter. Beides sind **Fremdimplementierungen**, kein Beweis, dass CIRQA diese Schritte erwartet.

**Neue, testbare Unterscheidung:** PulsLibre bestätigt 5024 derzeit nur mit einem **9-Byte-Status-ACK**; GATT bestätigt das Schreiben, aber ein **5024-Anwendungs-ACK ist nicht nachgewiesen**. Die fremde HRM600-Implementierung sendet stattdessen eine identitätstragende 5024-Antwort. Daher sind fehlende 5024-Handshake-Daten oder ein späterer Core-Feature-Schritt plausible Blocker für 5037; sie sind **nicht** als Ursache belegt. Das 5050-ACK mit SYNC-Bit 3 zeigt lediglich, dass der Empfänger diese einzelne Nachricht quittiert hat.

**Engineering:** `CirqaBootstrapGapAnalysis` klassifiziert die vorhandene Evidenz offline, ohne fremde Watch-Identität oder neue BLE-Befehle zu senden. Drei Unit-Tests prüfen die Entscheidung. Priorität: zunächst CIRQA-spezifischen 5024-/Core-Handshake aus einer autorisierten Referenzaufzeichnung oder passiven Daten verifizieren, erst danach einen neuen begrenzten aktiven Test konstruieren. Keine Wiederholung der zuvor abgewiesenen `0x8A39`-Dateiliste ohne neuen Beleg.

### Offline-Encoder verbessert (17.11.2026)

`GarminFileListRequest.gfdi(sequence, counter)` und `mlChunks(handle, sequence, counter, mtuPayload)` erlauben nun explizite Session-IDs. Bisherige Signaturen bleiben kompatibel; neue Unit-Tests prüfen Legacy-Gleichheit, Little-Endian-Encoding und ungültige Werte. **Kein BLE-Sendepfad geändert.** Die HRM600-Startwerte `0x01EF`/`0x0A` sind keine CIRQA-Nachweise und dürfen nicht blind verwendet werden. Der alte FileList-Request blieb `UNSUPPORTED`; als nächste Evidenz benötigt die Untersuchung die CIRQA-spezifische Session-/Bootstrap-Sequenz oder einen belegten alternativen FileSync-Modus.


## Garmin Connect 5.31: neue Offline-Evidenz (17.11.2026)

OpenCode untersuchte die vom Nutzer bereitgestellte Garmin Connect 5.31 (versionCode 11761), primärer DEX, JADX 1.5.6; Arbeitsnotiz `GARMIN_CONNECT_531_HANDSHAKE_TEST_NOTES.md` wurde im Chat bereitgestellt. Der proprietäre APK-/JADX-Code bleibt außerhalb des Repositories.

- `HandshakeHandler` (JADX `defpackage.zxl`) behandelt 5024, 5050 und 5027. 5024 wird mit Hostmetadaten beantwortet; der exakte CIRQA-spezifische Payload ist noch nicht rekonstruiert.
- Bei 5050 wird die Capability-Bitmap anhand des ersten Längenbytes abgegrenzt. Bei gültiger Konfiguration ACKt Connect die empfangene Nachricht und sendet danach eine **separate Host-5050-Antwort**. Die Completion dieser Antwort bestimmt den Handshake-Abschluss; GATT-Write und GFDI-ACK sind keine hinreichenden Ersatzbelege.
- Ein 5030-Event ist im untersuchten Handler keine nachgewiesene Voraussetzung. Die nächste FileSync-Startnachricht bleibt unbekannt.

Implementiert: `Cirqa5050BodyParser` und `CirqaConnect531HandshakeModel` mit JVM-Tests, offline-only. Der neue Zustandsautomat ist **nicht** mit dem BLE-Laufzeitpfad verdrahtet und darf nicht als aktiver CIRQA-Handshake ausgegeben werden. Der bisherige Diagnosepfad bleibt unverändert. Offene Aufgaben: reale 5024-Antwortstruktur vergleichen; 5050-HostConfiguration aus echten Geräteeigenschaften herleiten; Completion-Callback auf Android getrennt nachweisen; FileSync-Start im Connect-Code zurückverfolgen.


## 5024-Parser: implementierte Offline-Charakterisierung

`Cirqa5024DeviceInformationParser` verarbeitet die aus Connect 5.31 berichteten festen LE-Felder (`u16 protocolVersion`, `u16 productNumber`, `u32 unitId`, `u16 softwareVersion`, `u16 maxPacketSize`) sowie drei längenpräfixierte UTF-8-Strings. Zu kurze Felder und ungültiges UTF-8 werden abgelehnt; verbleibende gerätespezifische Bytes werden **nur gezählt**, nicht interpretiert. Der Parser erzeugt weder eine ACK-Antwort noch Bluetooth-Verkehr. Fünf JVM-Tests prüfen Endianness, sämtliche abgeschnittenen Präfixe, ungültige Stringlängen, optionale Tailbytes und UTF-8. Noch offen: produktabhängige optionale Felder und die exakte von Connect erzeugte 5024-Hostantwort. Der Offline-Parser ist noch nicht in den Android-BLE-Pfad integriert.


## Passive 5024-Strukturevidenz im Android-Diagnosebericht

`Cirqa5024StructureObserver` wurde in `GarminFileListProbe` integriert. Er prüft nur CRC-validierte GFDI-5024-Frames gegen den Offline-Parser und meldet Anzahl, Bodylängen, Parser-Treffer und optionale Restlängen. Unit-ID, Produktidentität und Zeichenketten werden nicht ausgegeben. Ein Parser-Fehlschlag ist **keine** Aussage über die Gültigkeit der CIRQA-Geräteinformation: Garmin Connect interpretiert optionale Felder produktabhängig, und die Zuordnung der protokollierten Bodybytes zum 5.31-Layout muss am Gerät bestätigt werden. Drei JVM-Tests decken Metadaten, CRC-Ablehnung und verkürzte Frames ab. Es wurde kein aktiver Sendepfad geändert.


## Garmin Connect 5.31: rekonstruierte 5024-Hostantwort (Übergabe OpenCode)

OpenCode hat offline folgenden Aufbau der **Erweiterung nach dem 5000-ACK-Präfix für 5024** nachvollzogen: `u16(protocol = featureFlag && deviceProtocol >= 150 ? 150 : 113)`, `u16(0xFFFF)`, `u32(0xFFFFFFFF)`, `u16(hostConfig.appVersion)`, `u16(0xFFFF)`, dann drei jeweils mit `u8`-Bytezahl präfixierte UTF-8-Felder (`hostConfig.friendlyName`, `Build.MANUFACTURER`, `Build.MODEL`), gefolgt von `u8(0x01)`. Zahlen sind little-endian. Die Bedeutung des letzten Bytes ist **unbekannt**. Dies belegt den Connect-Codepfad, nicht die Akzeptanz durch CIRQA.

`CirqaConnect531Host5024Response` bildet diesen Aufbau **offline** mit explizit übergebenen Werten ab. Es erfindet keine App-Version, Geräteidentität, Android-Build-Werte oder Feature-Flags und wird nicht vom Bluetooth-Sender aufgerufen. Tests prüfen die Bytefolge, den 5000/5024-Frame inklusive CRC, die Protokollbedingung, UTF-8-Bytezahl und Feldgrenzen. Der bisherige minimale 5024-Kandidat bleibt als historischer Diagnoseversuch erhalten; er ist nicht mit der Connect-Hostantwort gleichzusetzen.

Die von OpenCode genannte Detailanalyse `docs/protocol/research/GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md` lag zum Zeitpunkt dieser Umsetzung nur lokal bei OpenCode und war **noch nicht ins Repository gepusht**. Die vorstehende Zusammenfassung basiert auf der ausdrücklich übergebenen Analyse, nicht auf einer unabhängig verifizierten APK.


## Vorbereitung eines aktiven 5024-Vergleichstests

Die Offline-Preflight-Komponente `CirqaConnect531HandshakePreflight` blockiert einen künftigen 5024-Versuch standardmäßig: erforderlich sind verifizierte eingehende 5024-Informationen, explizit geklärtes Feature-Flag, belegte App-Version, belegte Werte für Friendly Name/Hersteller/Modell sowie eine ausdrückliche Testfreigabe. Die Preflight-Komponente selbst ist **nicht** mit der BLE-Laufzeit verdrahtet. Zwei Unit-Tests sichern Default-Deny und die Vollständigkeit der Evidenz ab. Diese Vorarbeit ist keine Behauptung, CIRQA akzeptiere die Connect-Hostantwort; eine tatsächliche Geräteprüfung steht aus.


## Android-Hostevidenz ohne Sende-Freigabe

`Cirqa5024AndroidHostEvidence` prüft lesend, ob `Build.MANUFACTURER` und `Build.MODEL` verfügbar sind. Im opt-in gestarteten `CirqaMlHandshakeProbe` wird ausschließlich ein boolescher Verfügbarkeitsbericht ausgegeben, keine Hoststrings oder Identitäten. Garmin-spezifische Werte (`hostConfig.appVersion`, `friendlyName`, Feature-Flag und verifiziertes Geräteprotokoll) bleiben **ungeklärt**; insbesondere wird die PulsLibre-`versionCode` nicht als Garmin-Appversion ausgegeben. Der Preflight bleibt gesperrt und der bisherige BLE-Sendepfad unverändert. Zwei JVM-Tests prüfen Default-Deny und datensparsame Ausgabe. **Dies ist noch kein aktiver Connect-kompatibler 5024-Test.**
