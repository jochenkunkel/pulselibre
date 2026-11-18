# 5030-only: Evidenz und Freigabevorlage (Jour fixe 18.11.2026)

**Status: technisch vorbereitet, nicht freigegeben, nicht am CIRQA ausgeführt.**
Bezug: Issues #2, #7, #20, #22. Keine neuen BLE-Kommandos im Jour-fixe-Auftrag.

> **Nachtrag 18.11.2026 – gesonderte Nutzerfreigabe:** Nach Vorlage dieses
> Plans bestätigte der Nutzer ausdrücklich „ok wird freigegeben“. Freigegeben
> ist ausschließlich **ein** Bootstrap→5030-only-Lauf mit den hier beschriebenen
> Grenzen und Stop vor 5007/5031/5002. Die Releasekonstante wird auf true
> gesetzt; der persistente Einmal-Gate und App-Dialog bleiben erforderlich.
> Das ist keine Freigabe eines kombinierten Downloadtests oder eines Retries.
> Bei der anschließenden Vorprüfung war ADB leer, daher bislang keine
> Installation/Verbindung, kein Gate-Verbrauch und kein neues Geräteergebnis.
> Die folgenden „gesperrt“-Angaben beschreiben den ursprünglichen
> Vorbereitungssnapshot; dieser datierte Nachtrag hat Vorrang.

Nach erneuter Nutzermeldung war das S25 wieder erreichbar (installiert Build
949). Vorprüfung: alter JSONL-Pfad und neues 5030-Gatefile fehlen, kein
Recorder-Service und kein laufender Garmin-Connect-Prozess gefunden. Lokale
Unit-Suite, Build, Lint und Instrumentation-APK-Build des Freigabecodes
erfolgreich. Vor Geräteausführung bleibt der signierte CI-Update-Build
einschließlich Zertifikats-/Versionsprüfung erforderlich.

## Integrationsprüfung

Zu Beginn `origin/main=7e78376`; `7d0af92` und `39d15f5` waren nur auf
`fix/cirqa-5031-probe`, nicht in `main`. PR #65 integrierte zuvor nur bis
`1590393`. Neuer Arbeitsbranch `fix/cirqa-5030-result-gate` enthält diese
Vorarbeiten und den aktuellen main. PR #66 (`codex/cirqa-status-ui`) betrifft
Codex' Dashboard-/Statusarbeit; DB-Schema und Messwertmodelle bleiben hier
unverändert. CI startet ausschließlich manuell.

## Quellen und Reproduzierbarkeit

Garmin Connect 5.31, versionCode 11761. Proprietäre Dateien und Dekompilate
bleiben außerhalb des Repositorys; nachfolgend eigene Beschreibung, kein
kopierter Implementierungscode.

| DEX | SHA-256 |
|---|---|
| classes.dex | `ccbfa18c9edf15754fbf0d92faf20215f0fc6f0ff88ae85a2ab36782e55e5408` |
| classes3.dex | `eaa725be5782b10a1cd301715d66c53f2d9ab94aacbe0acc7f883f1ed3a5c485` |
| classes9.dex | `eb9fd30f5f3cff7f2b93d717c32c839c0b66ef7e87af56bb968ecefeacee68d5` |
| classes10.dex | `e4d3e974e34b3c4f85ab266a9592c84623080aa1585e4ef4c11d92e6f479a4cf` |

`classes.dex` SHA-1 und Adler32 gegen DEX-Header erneut erfolgreich geprüft.
Mit JADX 1.5.6 zusätzlich Einzelklassen aus classes3/9/10 ausgewertet.

### Kontrollflussbelege

- **`classes.dex:yb60.a`**, DEX-Codeadresse `0x5151a4`:
  Code-Unit-Offsets `0x0074–0x0084` bauen `[eventByte,00]` und rufen
  `xns.b(5030, body, continuation)` auf. `0x008d` ruft `zi2.F(byte[])`;
  `0x0093` entnimmt `Byte.byteValue()`, `0x009c` liefert bei fehlendem Byte `−1`.
  Das Byte wird vorzeichenbehaftet zu `Int` erweitert.
- **`classes3.dex:zi2.F`**: bei leerem Array `null`, sonst erstes Byte.
- **`classes.dex:vid.b`**: eigener Dispatcher pro Verbindung; Ablehnung bei
  Zustand ENDED, Übergabe an aktive OutgoingStrategy.
- **`classes.dex:sez`**: Legacy-Queue serialisiert pro Nachrichtentyp und
  ordnet Antworten nach `responseTo` dem ausstehenden SendMessageTask zu.
  **`gg80`** ist eine alternative transaktions-ID-basierte Strategie (0..31);
  diese darf nicht mit einer allgemeinen 7-bit-Sequenz verwechselt werden.
- **`classes10.dex:h6p.run`, case 15**: prüft Länge und CRC, erkennt
  Response-Typ 5000, liest `u16LE responseTo` ab Offset 4, äußeren Status an
  Offset 6; `wi2.k(frame,7,length−2)` liefert den Restpayload. Dann
  `strategy.b(transactionId,responseTo,status,payload)`. Nach ENDED werden
  Antworten nicht mehr verteilt.
- **`classes9.dex:gu2.invoke`, Response-Zweig** und **`classes.dex:vns`**:
  Response wird als Typ 5000 mit ursprünglichem Typ (u16LE), äußerem Status
  (u8), danach Payload serialisiert. Das bestätigt dieselben Feldgrenzen.
- **`classes.dex:aq20.a`**: nur äußeres `ACK` erfüllt den Deferred mit dem
  Payload; NAK/UNKNOWN_OR_NOT_SUPPORTED führen zu ResponseStatusException,
  andere Fehler können in Connect den Retry-Pfad erreichen. Ein nicht
  bereiter Task meldet „Received a response while not ready“.
- **`classes.dex:rxk`**: Default 2 Retries, 5000 ms Antworttimeout.
  **`aq20`/`zp20`** haben daneben einen 45000-ms-Protokoll-Gesamttimer.
  Konkrete Laufzeitkonfiguration des CIRQA ist damit nicht nachgewiesen.
  PulsLibre übernimmt keine Connect-Retries.

## Antwortlayout und Ergebniswerte

Für die unkompakte Legacy-Antwort:

| Offset | Bedeutung |
|---:|---|
| 0..1 | Gesamtlänge u16LE, einschließlich CRC |
| 2..3 | 5000 (`88 13`) |
| 4..5 | Antwort auf 5030 (`A6 13`) |
| 6 | äußerer GFDI-Status |
| 7..length−3 | event-spezifischer Payload, möglicherweise leer |
| length−2..length−1 | GFDI CRC-16/ARC LE |

Das zusätzliche Ergebnis ist **das erste Byte an Frameoffset 7**, nicht das
äußere Statusbyte an Offset 6 und keine separate zweite Nachricht.
GFDI-Antwortlänge bei leerem Payload: 9; bei einem Ergebnisbyte: 10.
Connect selbst verlangt hier **nicht** exakt ein Payloadbyte; es liest nur
das erste. Zusätzliche Bytes werden deshalb in der Diagnose über ihre Anzahl
gemeldet, aber nicht als semantischer Erfolg freigegeben.

**Belastbar aufgelöste Werte:**

- Außen (`ly00`): 0 ACK, 1 NAK, 2 UNKNOWN_OR_NOT_SUPPORTED,
  3 COBS_DECODER_ERROR, 4 CRC_ERROR, 5 LENGTH_ERROR.
- Innen: leer → Connect-Fallback −1. Rohbyte `FF` wird ebenfalls zu −1;
  daher Länge und unsigned Rohwert getrennt berichten.
- Die Kotlin-Metadaten nennen `SEND_EVENT_RESULT_SUCCESS`,
  `SEND_EVENT_RESULT_UNSUPPORTED`, `SEND_EVENT_RESULT_UNKNOWN`; im optimierten
  `yb60` und Companion existieren **keine entsprechenden statischen Felder**
  mehr. Deren numerische Werte lassen sich daraus nicht rekonstruieren.
- `classes12.dex:SystemEventCompat` prüft bei Software-Update und Disconnect
  auf 0, beim Factory-Reset hingegen auf 1. Das sind andere Events und keine
  belastbare Erfolgsabbildung für HANDSHAKE_COMPLETE. Kein solcher Event
  wird von PulsLibre gesendet.

**Offen:** konkrete Bedeutungszuordnung der inneren HANDSHAKE_COMPLETE-Werte,
insbesondere `0`/`1`; es existiert bisher **kein CIRQA-5030-Referenztrace**.
Synthetische Testframes sind keine Gerätebelege.

### Wann darf 5007 folgen?

Aus `yb60` ergibt sich **keine** direkte Erfolgsbedingung „Byte 0 → 5007“.
`yb60.a` kann den Sync-Listener bei Host-Bit 3 auch im finally-/Fehlerpfad
anstoßen. Die separat untersuchte FULL_SYNC-Strategie hat zusätzliche
Anwendungs-/Capability-Gates (siehe Connect-Analyse); die vollständige
Runtime-Kette zu CIRQA ist nicht nachgewiesen.

PulsLibre deshalb: **nach diesem Test nie 5007**. Auch der Offline-Übergang
verwendet standardmäßig eine leere Erfolgsabbildung und sperrt alle 256
inneren Bytewerte. Positive Modelltests verwenden ausdrücklich synthetische
injizierte Abbildungen. Die Null-Erfolgshypothese aus `39d15f5` ist damit
überholt, nicht stillschweigend als Evidenz übernommen.

## Exakte Requestbytes

Body: `08 00` (`HANDSHAKE_COMPLETE`, reserviertes Nullbyte).

- Unkompakter GFDI-Rahmen: `08 00 A6 13 08 00 D5 C5`
- COBS mit Null-Delimitern: `00 02 08 04 A6 13 08 03 D5 C5 00`
- ML-Characteristic-Wert: **Laufzeit-Handle** davor; kein fest kodierter Handle,
  keine Fremdgeräte-Counter. Default-MTU passt (12 Byte inkl. Handle).

## Vorbereiteter Ablauf und Stop-Regeln

Der Testlabor-Eintrag ist **deaktiviert**. Zusätzlich prüft der BLE-Einstieg
`RELEASED_FOR_DEVICE_TEST=false`; aktuell keine Verbindung und kein
Gate-Verbrauch möglich. Die spätere Freigabe erfordert einen dokumentierten
Commit und neuen geprüften, kompatibel signierten Build.

Nach gesonderter Zustimmung:

1. Schema-/Signatur-/Version des Zielbuilds gegen installierte App prüfen.
   Nicht deinstallieren. Die bekannte JSONL-Entfernung bei DB-Öffnung zuvor
   berücksichtigen; keine Datenbank-/Messwertschnittstelle wird hier geändert.
2. S25 entsperrt, Bluetooth an, genau ein gekoppeltes CIRQA, Recorder beendet,
   Garmin Connect nicht aktiv, keine parallele GATT-Sitzung.
3. Nutzer öffnet Testlabor, bestätigt 5030-only-Dialog. Neues separates
   Preferencefile `cirqa_5030_only`, Schlüssel `legacy5030OnlyAttempted`.
   Gate synchron/atomar im Prozess geprüft, `commit()` vor connectGatt.
   Bei Persistenzfehler/Zielmehrdeutigkeit/Busy keine Verbindung.
   Bei Verbindungsfehler nach Gate-Verbrauch **kein** automatisches Rearming.
4. Neue Probeinstanz, GFDI-Registrierung, bewährter 5024/5050-Bootstrap mit
   unveränderter 15-Byte-Nullmaske. 5024- und 5050-Schreiben GATT-bestätigt
   plus frisches CRC-valides 5050-ACK, keine ausstehenden Schreibchunks.
5. Höchstens **ein** 5030-Request. 5 Sekunden ab Claim für Antwort **und**
   Write-Abschluss; gesamte Sitzung höchstens 30 Sekunden (frühere bestehende
   Bootstrap-Timeouts können ebenfalls abbrechen).
6. Nur ein korrelierter, CRC-valider, nichtkompakter 5000-Frame zu 5030;
   bis 64 decodierte / 66 codierte Framebytes, insgesamt 256 Antwortbytes.
   Fragmentierung erlaubt, mehrere Frames/Übergröße/fehlender Delimiter,
   falscher Typ, CRC-/COBS-Fehler, negativer äußerer Status: sofort stoppen.
7. Antwort und Write dürfen in beiden Reihenfolgen eintreffen; beide nötig
   für „beobachtet“, **nicht** für einen behaupteten semantischen Erfolg.
   Alte Sessiontokens/alte GATT-Callbackgenerationen werden ignoriert;
   Duplikate vor Abschluss stoppen. Android liefert keine eigene Operation-ID
   pro Write-Callback; das wird nicht als zusätzliche Wire-Korrelation ausgegeben.
8. Danach GATT trennen und schließen. Keine Folgekommandos, kein Retry,
   5007/5031/5002/5004-Download, 5037-Send, CLOSE_ALL, Archiv/Flags/Erase.

Gespeicherter Bericht: Phase/Stop-Grund, äußerer Status, Payloadlänge,
erstes Ergebnisbyte unsigned und Connect-signed; kein Rohframe,
Identifier, Zeitpunkt oder Gesundheitswert.

## Risikoentscheidung

5030 ist ein Systemevent, **kein nachweislich nebenwirkungsfreier Read**.
Geräteseitige Sync-Bereitschaft oder weitere Nachrichten sind möglich; deren
Funktion auf CIRQA ist unbekannt. Unerwartete Frames werden nicht automatisch
beantwortet. Keine vollständige Connect-Identität, Capabilitymaske oder FULL_SYNC
emulieren. Das Gate schützt vor Wiederholung, nicht vor jeder Gerätewirkung.

**Angefragte spätere Freigabe:** einmaliger Bootstrap→5030-only mit den obigen
Grenzen, ausdrücklich Stop vor 5007/5031/5002. Eine Zustimmung gilt nicht
für anschließende FileSync-Schritte. Heute wurde kein Gerätetest ausgeführt.

## Offline-Prüfung

Lokale Unit-Suite, Debug-APK, Lint und Instrumentation-APK-Build erfolgreich.
Die 5030-Tests prüfen Request-Golden-Bytes/COBS, alle 256 Rohwerte,
Fragmentierung, beide Callback-Reihenfolgen, fehlenden/mehrbyteigen Payload,
negative und unerwartete Antworten, CRC-/COBS-Fehler, Limits, Duplikate,
Sessiontoken, Clock-Regression und persistentes Gate (Erfolg/Fehler/gesperrt).
Es wurden keine Instrumentation-Tests auf dem S25 ausgeführt. GitHub-CI wird
separat manuell gestartet; lokale Ergebnisse sind kein CI-Nachweis.
