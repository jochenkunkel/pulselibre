# CIRQA Historical Sync – Test V1 (18.11.2026)

## Ziel
Erstmals die vollständige Sequenz `Bootstrap → 5030 → 5007 → 5031 → Directory`
in **einer einzigen BLE-Verbindung** validieren – statt isolierter Einzelschritte.

## Design
Eine Testfunktion im Testlabor mit drei Phasen, jede mit eigenem persistentem Einmal-Gate:
- **Phase A:** Bootstrap → 5030 → 5007, Stopp vor 5031.
- **Phase B:** A + 5031, Stopp vor Directory.
- **Phase C:** B + Directory (Index 0), Stopp vor FIT-Download.

Zustandsmaschine `CirqaHistoricalSyncV1MultiPhaseSession`: strikte
Write/Response-Korrelation, Token/Callback-Generation, harte Timeouts (45 s
Gesamt, 5 s je Antwort), kein Retry, keine schreibenden Gerätekommandos,
kein FIT-Download in V1.

## Gerätebefunde (S25 / CIRQA)
### Phase B – ERFOLGREICH (Build 1030+)
```
phase=PHASE_B_COMPLETED; reason=phase-b-completed-stop;
5030(outer=0,res=0); 5007=0; 5031(status=0,types=12); dirSize=null;
diag(notifs=5,frames=4,lastType=5000,lastTo=5031,outer=0,payloadLen=173,
     cobsErr=0,crcErr=0,discarded=0,phaseAtRx=WAITING_5031_RESPONSE,
     protobuf=1,writeCb=true,writeOk=true,timeout=null)
```
Damit erstmals bestätigt: 5024/5050-Bootstrap, 5030 HANDSHAKE_COMPLETE,
5007 PENDING_UPLOADS_ONLY und 5031 Supported File Types funktionieren in
einer Verbindung. CIRQA sendet zusätzlich einen 5043-Protobuf-Frame
(GDI-GFDI-Protobuf-Kanal) – als Evidenz protokolliert, nicht beantwortet.

### Wichtige während der Entwicklung behobene Fehler
- 5050-ACK setzte State Machine nicht fort (Trigger ergänzt).
- `mlWritePending` blockierte den 5030-Write (zurückgesetzt).
- WRITE_TYPE_NO_RESPONSE liefert keinen GATT-Write-Callback (direkt bestätigt).
- Response-Phase wurde statt Request-Phase an `sendHsv1Frame` übergeben.
- Fehlende `claim*()` Calls für 5007/5031/Directory (zentralisiert).
- Doppelter Sendeweg über `finishHsv1IfTerminal` und receive-Block.

### Phase C – ERFOLGREICH (Build 1030+)
```
phase=PHASE_C_COMPLETED; reason=phase-c-completed-directory-size-obtained;
5030(outer=0,res=0); 5007=0; 5031(status=0,types=12); dirSize=8192;
diag(notifs=6,frames=5,lastType=5000,lastTo=5002,outer=0,payloadLen=5,
     cobsErr=0,crcErr=0,discarded=0,protobuf=1,writeCb=true,writeOk=true)
```
Erstmals volle Sequenz 5024/5050→5030→5007→5031→5002 in EINER Verbindung.
Directory-Index 0 meldet **8192 Bytes** (statt des früheren leeren 1-Eintrags).
Die 8192 Bytes wurden in V1 noch nicht gelesen/gespeichert (nur Size ausgelesen).

## Directory-Body nach voller Kette (V2–V6)

Der 5004-Body-Read startet zuverlässig mit `size=8192/32; chunks=1`, stoppt aber
nach dem ersten 32-Byte-Chunk. Frühere Ursachen und Fixes:
- Stale 5-s-Response-Timer des 5002-Requests beendete den Lauf nach Chunk 1 →
  beim Body-Start abgebrochen (Gate V2).
- Nicht-5004-Kontrollframes brachen den Stream ab (`unexpected-type`) →
  zählen/ignorieren (Gate V3).
- Nur das **erste** COBS-Frame pro Notification wurde dekodiert; der bewährte
  Legacy-Pfad `CirqaLegacy5004ResponseObserver` verarbeitet dagegen **alle**
  Frames und über Notification-Grenzen hinweg → V2 übernimmt das (Gate V4).

### Entscheidender Befund (Gate V5, Build 1028)
```
notifs=3; frames=3; dataFrames=1; control=5043x2;
protobuf=5043[id=2705,off=0,total=4,chunk=4];complete[len=4,shape=13:2];
         5043[id=2705,off=0,total=4,chunk=4];complete[len=4,shape=13:2];
```
Nach dem ersten 5004-Chunk sendet das Gerät **zwei identische, vollständige
4-Byte-Protobuf-Nachrichten** (Request-ID 2705, `Smart`-Feld 13, wire type 2)
und verstummt dann. Der 5004-ACK ist byte-korrekt (identisch mit dem
erfolgreichen Legacy-Lauf) – der Blocker ist das Ausbleiben der 5043-Quittung.

### Connect-Referenz (5.31, dekompiliert)
- `t3z.ProtobufHandler.onMessageReceived` (5043/5044) sendet **für jeden Chunk**
  ein GFDI-5000-ACK mit `ly00.ACK=0`, `responseTo=5043`, Payload
  `[requestId u16LE | receivedOffset u32LE | successFlag u8 | status u8]`,
  `c4z.NO_ERROR=0` – unabhängig vom Protobuf-Inhalt, sogar bei Parse-Fehlern.
- `v3z`/`t3z.h(5044, …)` ist die separat davon erfolgende Protobuf-Antwort;
  sie wird **nicht** gesendet (nur Transport-ACK, wie freigegeben).

Gate V6 (freigegeben) quittiert die 5043-Chunks mit genau diesem Format über
eine serialisierte ACK-Warteschlange; kein 5044, keine Geräteänderung.
Ergebnis V6/V7: ACK wird akzeptiert (Request-ID 2705 → 2706 → 2707, nur noch
**ein** 5043 pro Verbindung), der 5004-Stream bleibt aber bei 32 B stehen.
Verschachtelte Protobuf-Struktur (V7): `13:2[14:2]` = `Smart.core_service`
→ CoreService-Feld 14 = **`connection_ready_notification`**. Connect
(`vxk.java`, ConnectionReadyListener) behandelt das als reine Benachrichtigung
ohne 5044-Antwort. Damit ist die 5043-Hypothese widerlegt; der Blocker liegt
im 5004-Fortgang selbst.

Unabhängige Referenzen bestätigen unser 5004-ACK-Format byte-genau:
`tenover/gfdi.rs build_ack(5004, 0, [status,u32 nextOffset])` und
`wristbeam/GFDIFiles.swift GFDIFileReceiver.responsePayload`.

Gate V10 (Build 1033) lieferte den finalen Beweis:
```
events=5004[ts=0,crc=20481,off=0,len=32]@2ms;5043@4813ms;
firstChunk=entries=1; idx0=0; type0=0;
acks5004=1;acks5043=1;pendingWrites=0;writePending=false;
[Vollständige ACK-Hexbytes auf Nutzeranweisung vom 24.11.2026 entfernt.]
```
Der 32-Byte-Chunk ist bereits ein vollständiges Directory (Platzhalter
`idx=0,type=0`), identisch mit dem alten Lauf ohne Kette. Unser ACK ist
byte-perfect (dekodiert `[14][5000][5004][0][0][32][CRC]`) und wurde sofort
gesendet. Das Gerät sendet danach keine weiteren Chunks.

### Filter-Variante (Gate V11, freigegeben)
Connect-Enum `zgd` (`com.garmin.gfdi.file.a.applyDirectoryFilter`):
`NO_FILTER=0`, `DEFAULT=1`, `PENDING_UPLOADS_ONLY=3` (unser bisheriger Wert).
Gate V11 testet `NO_FILTER=0` in derselben gated Kette.

### Weiterer Connect-Befund (nicht umgesetzt)
Connect 5.31 enthält `BluetoothClassicAcceptor`/`ph4` mit der SPP-UUID
`DEAB91E4-670F-11E1-9ACE-30BC4824019B`. Ein öffentliches Gadgetbridge-Issue
(#6879) beschreibt genau dieses Bild: leeres Platzhalter-Directory über BLE,
echte Dateien nur über Classic-RFCOMM. Das ist ein möglicher Grund für die
8192/32-Diskrepanz, aber eine separate, deutlich größere Änderung und nicht
Teil der aktuellen Freigabe.

## Nächster Schritt
Ergebnis von Gate V6 auslesen. Bei vollständiger Directory: FIT-Einträge
klassifizieren und nächsten begrenzten FIT-Download vorbereiten. Kein
Löschen/Archivieren/Flags, kein CLOSE_ALL, kein Retry.
