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
