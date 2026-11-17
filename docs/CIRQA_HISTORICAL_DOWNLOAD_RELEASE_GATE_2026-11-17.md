# Historischer CIRQA-Download – Implementierungs- und Freigabegate

Stand: 2026-11-17. Ziel: lesender historischer FIT-Abruf, keine geräteseitige Änderung.

## 1. CRC / DEX-Verifikation – BLOCKIERT

**Belegt:** `u45.a(byte,int)` implementiert CRC-16/ARC, initial 0, reflected polynomial 0xA001. `yw40.a(byte[])` verarbeitet unkomprimierte 5004-Payloadbytes ab Offset 7. **Nicht belegt:** Ob der tatsächliche DEX-Kontrollfluss den CRC-Akkumulator **vor** oder **nach** dem letzten Payloadbyte vergleicht. Die JADX-Darstellung vergleicht scheinbar den Vorletzten-Zustand. Der Quelltext des Garmin-DEX liegt nicht im Repository; kein Zugriff auf die erforderlichen Bytecode-Registerspuren in diesem Arbeitsschritt.

**Reproduzierbare Prüfung für den Reverse-Engineering-Agenten:** Original-APK-Hash gegen `a7cbd2bdde76d4fb804c2a07cee908adb0f9e4846a05f082e92b01f8dea1e4a5` prüfen; `classes.dex` / `defpackage.yw40.a([B)` als smali/baksmali oder dex disassemblieren; die Registerzuweisungen innerhalb der Schleife, die `u45.a`-Rückgabe und das Vergleichsregister der anschließenden `if-*`-Instruktion zeilen-/offsetgenau dokumentieren. Vektoren: Payload `31 32 33 34 35 36 37 38 39` -> normaler CRC `0xBB3D`; den tatsächlichen Vergleichswert aus der Registerspur berechnen. Auch leere und einbyteige Payload prüfen. Ergebnis mit disassembly offsets und einem eindeutigen Pass/Fail-Vektor im Bericht festhalten. **Bis dahin keine produktive 5004-CRC-Akzeptanz.**

## 2. CIRQA-Unterstützung – NO-GO

Die vorhandenen CIRQA-Traces zeigen 5050-ACK, aber **keine** belegten 5007-/5031-/5002-/5004-Dateitransfers. Der kompakte Kandidat `0x8A39` wurde mit `UNSUPPORTED` abgelehnt. Connect-Implementierung ist keine CIRQA-Gerätebestätigung.

**Benötigter Nachweis:** ein autorisierter, passiver, datenschutzbereinigter CIRQA-Mitschnitt mit GFDI-Nachrichtentypen, korrekter äußerer Framing-/CRC-Prüfung, Capabilitybitmap und nachweisbar erfolgreicher Read-Response; oder gleichwertige CIRQA-spezifische Firmware-/Profil-Evidenz. Protokollversion, Kompression und Dateitypen müssen zur tatsächlichen CIRQA passen. Keine Wiederholung abgelehnter Kommandos. Keine erfundenen Gerätedaten.

## 3. Implementierung – offline vorbereitet, BLE-Pfad gesperrt

Vorhanden: `CirqaLegacy5002Codec` (14-Byte-Request, u32LE-Response), `CirqaLegacyCrc16`, `CirqaLegacyTransferDecoder`, `CirqaLegacyOfflineFileSession`, `CirqaLegacyPlainFileAssembler`, `CirqaLegacyDirectoryParser`, `CirqaLegacyFitFileSelector`, `CirqaVerifiedFitStore`. FIT-CRC ist eine **separate** Prüfung; die Dateiablage ist app-privat und offline.

Noch nötig: verifizierter 5004-CRC-Vertrag; nach CIRQA-Evidenz ein separater read-only BLE-Orchestrator mit hartem Request-Allowlist-Gate, begrenzter Dateigröße, Zeit-/Chunk-/Retry-Limits, eindeutiger Cancel-/Disconnect-Behandlung, Replay-/Offset-Schutz, ohne Archivieren/Löschen/Schreiben/Flagändern; End-to-End-Tests mit synthetischen Frames, danach ein einziger explizit genehmigter Gerätetest. Die App darf keine FIT-Historie im Dashboard behaupten, bevor FIT-Datensätze, Zeitstempel, Duplikate und lokale Persistenz tatsächlich validiert sind.

## 4. Test-APK – derzeit nur Bestandsfunktionen testbar

Der erfolgreiche signierte Basisbuild `23c2cfd9` ist unter GitHub Actions Run `44101529585` verfügbar. Die Offline-Module haben keinen UI-Downloadbutton. Ein neuer normaler Android-CI-Lauf nach Dokumentations-/Kommentaränderungen **ist keine** historische Download-Testfreigabe. Ein echter Download-Testbuild wird erst nach Gate 1+2 und einem sicherheitsgeprüften BLE-Lesepfad erstellt.

## Konkrete Übergabe an OpenCode

1. DEX/smali-Registerprüfung von `yw40.a([B)` abschließen und nachprüfbare CRC-Vektoren liefern.
2. `v5d`-Registrierung und CIRQA-spezifische FileSync-Route mit belegtem Callgraph untersuchen; Connect-Code nicht als CIRQA-Beweis ausgeben.
3. Belegbare CIRQA-Capability-/Subtype-Information und passive Trace-Auswertung ergänzen; keine aktiven BLE-Kommandos.
4. Ergebnisse in `docs/GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md` und `docs/CIRQA_LEGACY_FILESYNC_PROTOCOL.md` mit exakten Referenzen aktualisieren.

**Freigabeentscheidung:** **NO-GO** für aktive historische CIRQA-Downloads bis die oben genannten Beweise vorliegen. Das schützt die Uhr und verhindert, dass ein grüner APK-Build fälschlich als erfolgreicher historischer Download interpretiert wird.
