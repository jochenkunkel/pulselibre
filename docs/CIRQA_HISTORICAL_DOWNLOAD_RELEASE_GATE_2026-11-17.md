# Historischer CIRQA-Download – Implementierungs- und Freigabegate

Stand: 2026-11-17. Ziel: lesender historischer FIT-Abruf, keine geräteseitige Änderung.

## 1. CRC / DEX-Verifikation – ABGESCHLOSSEN (statischer Connect-Befund)

`classes.dex` aus dem außerhalb des Repositories liegenden Garmin-Connect-5.31-`base.apk` war lokal verfügbar. Die extrahierte Datei ist bytegleich mit `/tmp/opencode/garmin-connect-5.31/dex/classes.dex` (SHA-256 `ccbfa18c9edf15754fbf0d92faf20215f0fc6f0ff88ae85a2ab36782e55e5408`); `dexdump -c` meldet „Checksum verified“. Die proprietäre APK/DEX bleibt außerhalb des Repositorys.

### Tatsächlicher Kontrollfluss in `yw40.a([B)Ljow;`

`dexdump -d classes.dex` zeigt die Methode bei DEX-Codeadresse `0x4f82d8`; der jeweilige Offset in Code-Units steht nachfolgend in Klammern:

| Code-Unit-Offset | DEX-Instruktion | Bedeutung |
|---:|---|---|
| `0x0021`–`0x0029` | `a60.E(1,v12)` → `move-result v4`; `a60.F(3,v12)` → `move-result-wide v6`; `long-to-int v6` | erwartete Chunk-CRC in `v4`; Datenoffset in `v6` |
| `0x002f` | `if-ne v6,v7,0x0074` | CRC-Pfad nur bei erwartetem Offset (`this.g`) |
| `0x0032`–`0x0033` | `move v1,v3` (v3=7); `iget v7,this.e` | Payloadcursor 7; laufender CRC in `v7` |
| `0x0035` | `if-ge v1,v0,0x0042` | bei leerer Payload direkt zum CRC-Vergleich |
| `0x0037`–`0x003d` | `aget-byte v10,payload[v1]`; `invoke-static {v10,v7},u45.a`; `move-result v7`; `iput v7,this.e` | jeder Payloadbyte-Update-Rückgabewert ersetzt sowohl `v7` als auch den Feldakkumulator |
| `0x003f`–`0x0041` | Cursor erhöhen; `goto 0x0033` | Schleife aktualisiert `v7` für alle Payloadbytes |
| **`0x0042`** | **`if-eq v7,v4,0x005a`** | **Verglichen wird der aktualisierte Endwert in `v7`, nicht der Wert vor dem letzten Byte** |

Die relevanten DEX-Byteadressen sind `0x4f833e` (Akkumulator laden), `0x4f8346` (Payloadbyte), `0x4f834a` (CRC-Update call), `0x4f8350` (Rückgabewert nach `v7`), `0x4f8352` (in Objektfeld speichern) und `0x4f835c` (Vergleich `v7` mit `v4`). Damit ist die scheinbare Vorletzter-Wert-Besonderheit der JADX-Java-Rekonstruktion **widerlegt**: dort war die Variable falsch/irreführend rekonstruiert; der DEX-Registerfluss ist eindeutig.

### CRC-Vektoren, die dem DEX-Kontrollfluss entsprechen

`u45.a` ist CRC-16/ARC: reflected polynomial `0xA001`, init `0`, kein final XOR; CRC-Wirefeld u16 LE (`a60.E`). Der CRC-Akkumulator `this.e` ist laufender Zustand des Parserobjekts. Für einen frischen Parser:

| Payload | erwarteter CRC | Wirebytes |
|---|---:|---|
| leer | `0x0000` | `00 00` |
| `41` (`A`) | `0x30C0` | `C0 30` |
| `31 32 33 34 35 36 37 38 39` (`123456789`) | `0xBB3D` | `3D BB` |

Bei leerem Payload springt der DEX von `0x0035` direkt zu `0x0042`, sodass der vorhandene laufende Akkumulator verglichen wird (bei frischem Parser 0). Bei einem Byte vergleicht er nach einem `u45.a`-Update; bei mehreren Bytes nach dem letzten Update. Ein Vektor über Chunkgrenzen muss den akkumulierten CRC des gesamten übertragenen Datenstroms berücksichtigen, nicht jeden Chunk unabhängig neu initialisieren.

Die vorhandenen JVM-Tests prüfen CRC-Referenzwerte, den Einbytevektor und inkrementelle Fortsetzung. Sie testen den Algorithmus, nicht die proprietäre DEX-Methode selbst; der DEX-Registerbeleg oben ist der Kontrollflussnachweis.

## 2. CIRQA-Unterstützung – NO-GO

Die vorhandenen CIRQA-Traces zeigen 5050-ACK, aber **keine** belegten 5007-/5031-/5002-/5004-Dateitransfers. Der kompakte Kandidat `0x8A39` wurde mit `UNSUPPORTED` abgelehnt. Connect-Implementierung ist keine CIRQA-Gerätebestätigung.

**Benötigter Nachweis:** ein autorisierter, passiver, datenschutzbereinigter CIRQA-Mitschnitt mit GFDI-Nachrichtentypen, korrekter äußerer Framing-/CRC-Prüfung, Capabilitybitmap und nachweisbar erfolgreicher Read-Response; oder gleichwertige CIRQA-spezifische Firmware-/Profil-Evidenz. Protokollversion, Kompression und Dateitypen müssen zur tatsächlichen CIRQA passen. Keine Wiederholung abgelehnter Kommandos. Keine erfundenen Gerätedaten.

## 3. Implementierung – offline vorbereitet, BLE-Pfad gesperrt

Vorhanden: `CirqaLegacy5002Codec` (14-Byte-Request, u32LE-Response), `CirqaLegacyCrc16`, `CirqaLegacyTransferDecoder`, `CirqaLegacyOfflineFileSession`, `CirqaLegacyPlainFileAssembler`, `CirqaLegacyDirectoryParser`, `CirqaLegacyFitFileSelector`, `CirqaVerifiedFitStore`. FIT-CRC ist eine **separate** Prüfung; die Dateiablage ist app-privat und offline.

Noch nötig: eine CIRQA-spezifische Bestätigung des 5004-CRC-/Transfervertrags; der Connect-DEX-Nachweis allein reicht nicht. Erst nach CIRQA-Evidenz darf ein separater read-only BLE-Orchestrator mit hartem Request-Allowlist-Gate, begrenzter Dateigröße, Zeit-/Chunk-/Retry-Limits, eindeutiger Cancel-/Disconnect-Behandlung und Replay-/Offset-Schutz entworfen werden. Keine Archivierung/Löschung/Schreib-/Flag-Kommandos. Danach End-to-End-Tests mit synthetischen Frames und erst anschließend ein explizit genehmigter Gerätetest. FIT-Historie darf im Dashboard erst behauptet werden, wenn FIT-Datensätze, Zeitstempel, Duplikate und lokale Persistenz validiert sind.

## 4. Test-APK – derzeit nur Bestandsfunktionen testbar

Der erfolgreiche signierte Basisbuild `23c2cfd9` ist unter GitHub Actions Run `44101529585` verfügbar. Die Offline-Module haben keinen UI-Downloadbutton. Ein neuer normaler Android-CI-Lauf nach Dokumentations-/Kommentaränderungen **ist keine** historische Download-Testfreigabe. Ein echter Download-Testbuild wird erst nach Gate 1+2 und einem sicherheitsgeprüften BLE-Lesepfad erstellt.

## Konkrete Übergabe an OpenCode

1. Erledigt: DEX-Registerprüfung von `yw40.a([B)` und nachprüfbare Connect-CRC-Vektoren dokumentiert.
2. `v5d`-Registrierung und CIRQA-spezifische FileSync-Route mit belegtem Callgraph untersuchen; Connect-Code nicht als CIRQA-Beweis ausgeben.
3. Belegbare CIRQA-Capability-/Subtype-Information und passive Trace-Auswertung ergänzen; keine aktiven BLE-Kommandos.
4. Ergebnisse in `docs/GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md` und `docs/CIRQA_LEGACY_FILESYNC_PROTOCOL.md` mit exakten Referenzen aktualisieren.

**Freigabeentscheidung:** CRC-DEX-Frage für Connect ist geklärt; **NO-GO** für aktive historische CIRQA-Downloads bleibt bestehen, bis CIRQA-spezifische Protokollunterstützung und der sichere Transport-/ACK-Pfad nachgewiesen sind. Das schützt die Uhr und verhindert, dass ein grüner APK-Build fälschlich als erfolgreicher historischer Download interpretiert wird.

## Nachtrag: DEX-Verifikation und begrenzter Prüfauftrag (2026-11-17)

Die DEX-Registerbefunde, CRC-Vektoren und 5002-Connect-Layouts sind oben anhand der lokal vorhandenen DEX-Datei konkret dokumentiert. Die Original-APK/DEX bleibt außerhalb des Repositorys. Diese Evidenz gilt ausschließlich für Garmin Connect 5.31 und beweist keine CIRQA-Kompatibilität.

### Auftrag: keine neuen BLE-Kommandos

1. `v5d`-Registrierung und CIRQA-spezifische FileSync-Route statisch mit Callgraph untersuchen; Connect-Code nicht als CIRQA-Nachweis ausgeben.
2. Nur vorhandene, autorisierte **passive** CIRQA-Traces auf Capability-, GFDI- und Dateitransfer-Evidenz prüfen; Rohgesundheitsdaten und Gerätekennungen nicht öffentlich hochladen.
3. Erst bei CIRQA-spezifischer Evidenz einen **separaten Sicherheitsreview** für einen einzelnen begrenzten, rein lesenden Gerätetest vorlegen und ausdrückliche Benutzerfreigabe einholen.

**Verboten:** neue aktive BLE-Kommandos; erneutes `0x8A39`; Archivieren, Löschen, Schreiben, Ändern von Geräteflags; Download-UI oder als Historien-Download beworbene Test-APK vor belegtem sicherem CIRQA-Pfad. **Freigabe bleibt NO-GO.**

### Teststatus

Ein vorheriger lokaler Lauf von `:app:testDebugUnitTest` war erfolgreich (24 Tasks). Nach Rebase auf den inzwischen aktualisierten `origin/main` läuft derselbe Task erneut. Ein Push erfolgt erst nach erfolgreichem Abschluss und erneutem Remote-Stand-Check.
