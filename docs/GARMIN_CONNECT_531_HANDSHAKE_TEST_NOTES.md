# Garmin Connect 5.31 – Handshake-Testnotizen (Projektübernahme)

Stand 2026-11-17. Verdichtete Übernahme der vom Benutzer gelieferten 151-zeiligen OpenCode-Notiz. Quelle: Garmin Connect 5.31/versionCode 11761; lokales APK-Bundle SHA256 `a7cbd2bdde76d4fb804c2a07cee908adb0f9e4846a05f082e92b01f8dea1e4a5`; APK und Dekompilate bleiben außerhalb von GitHub.

**5024:** `zxl` registriert 5024/5050/5027. DeviceInformation LE: u16 protocolVersion, u16 productNumber, u32 unitId, u16 softwareVersion, u16 maxPacketSize, danach drei u8-längenpräfixierte UTF-8-Strings; optionale gerätespezifische Tailbytes. Unterlauf → LENGTH_ERROR. Erfolgreiche Antwort ACK plus Host-Metadaten. Wiederholtes 5024 wird erneut beantwortet; auf Android API >=34 führt mehr als zweimalige Wiederholung zu Fehler.

**5050:** Vor 5024/initialisierter Konfiguration kein ACK aus dem Handler. Body `u8 bitmapByteCount | bitmap | optionalTail`; wenn weniger als `1 + bitmapByteCount` Bytes: LENGTH_ERROR. Gültige Eingabe → Empfangs-ACK → **separate 5050-Hostantwort**. Handshake COMPLETE erst nach deren Completion bzw. definierten Status-Ausnahmen. Connect kann bei gesetztem Bit 3 und fehlenden Bits 4/90 intern Bit 4 ergänzen; daraus keine CIRQA-Hostmaske ableiten.

**5027:** ACK plus Nullbyte. **5030:** Nach dem 5050-Handshake wird durch anderen Connect-Pfad `HANDSHAKE_COMPLETE=8` gesendet; die ursprüngliche `zxl`-Klasse registriert selbst keinen 5030-Listener.

**Offline-Tests:** 5024 Endianness/Trunkierung/UTF-8/optionaler Tail; 5050 vor 5024, Nullbitmap, Längenfehler, zusätzliche Tailbytes, fehlgeschlagene Hostresponse; getrennte Zustandsübergänge für GATT-ACK, GFDI-ACK und 5050-Response-Completion; 5027 ACK; 5030 nicht als unbewiesene Voraussetzung für das Parsing missbrauchen.

**Sicherheitsgrenze:** Nur synthetische Offline-Tests, keine neuen GATT-/GFDI-Kommandos, keine fremden Geräteidentitäten oder ungeprüften Capability-Bits. Vollständige Originalnotiz bleibt im vom Benutzer bereitgestellten Dokument verfügbar.
