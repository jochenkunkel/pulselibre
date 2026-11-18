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

## Nächster Schritt
Directory-Index 0 (8192 Bytes) per 5004 lesen, FIT-Header/CRC validieren und
Einträge (Index/Typ/Subtyp/READ/Größe) datensparsam protokollieren. Erst nach
identifiziertem gültigen FIT-Eintrag-download. Kein Löschen/Archivieren/Flags.
