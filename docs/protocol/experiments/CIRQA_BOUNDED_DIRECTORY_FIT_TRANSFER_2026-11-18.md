# CIRQA bounded directory + FIT transfer

## Freigabe und Umfang

Am 2026-11-18 hat der Nutzer den nächsten Schritt ausdrücklich auf Directory-Index 0 **und** FIT-Dateien erweitert. Bestehende Evidenz bleibt Voraussetzung: bestätigter 5031-Befund Status 0 mit unterstützten Typen. Der geschlossene 5007-5/5-Gate wird nicht umgangen: dieser Lauf führt einen frischen 5024/5050-Bootstrap aus, sendet aber **weder 5007 noch 5031 erneut**.

## Feste Limits

- Eine Directory-Anforderung für Index 0; unkomprimierter Kandidat.
- Maximal 1 MiB Directory.
- Nur Directory-Einträge mit type 128, READ-Bit und einem zuvor mit Status 0 bestätigten 5031-Typ.
- Höchstens vier FIT-Dateien, je maximal 4 MiB, zusammen maximal 16 MiB.
- Maximal fünf Minuten gesamte Transferzeit.
- Unkomprimierte 5004-Daten nur nach GFDI-CRC, laufender Chunk-CRC und Offset validieren; positives 5004-ACK erst nach erfolgreicher Prüfung. Kein Retry oder automatisches Wiederaufnehmen.
- FITs erst nach eigenständiger FIT-Header-/File-CRC-Prüfung atomar in `files/cirqa_fit_history/session_*/` speichern. Kein SQLite-Import, keine Cloud-/SAF-Ausgabe und kein Löschen vorheriger Dateien.
- Keine SetFlags/Archive-, Erase-, CLOSE_ALL-, 5008- oder FileSync-V2-Kommandos.

Bei Statusfehlern, ungültiger Struktur/CRC/Offset, Größenüberschreitung, Datei-Index/Größenabweichung, Schreibfehler oder Timeout sofort stoppen. Bereits vollständig verifizierte FIT-Dateien bleiben lokal; unvollständige Transfers werden nicht automatisch fortgesetzt.

## Gerätelauf und Ergebnis (2026-11-18)

Implementierung und Offline-Tests sind in Commit `378a0f7`; CI-Workflow-Dispatch `44196330148` war erfolgreich, stabil signiertes Build 940 wurde mit Datenerhalt installiert.

Vor dem Lauf: S25 entsperrt; Build 940 installiert; Bluetooth an, keine bestehende CIRQA-GATT-Verbindung; Recorder-Dienst nicht aktiv; Garmin Connect war nicht installiert/aktiv. Der gespeicherte CRC-validierte 5031-Status-0-Befund lieferte unterstützte Typen.

Der freigegebene Einmaltransfer lief mit frischem Bootstrap und **einem** unkomprimierten 5002-Request für Directory-Index 0. Directory-Transfer wurde vollständig und CRC-/Offset-validiert. Ergebnis: **ein Directory-Eintrag, null passende lesbare FIT-Einträge** nach den bestätigten Typen/READ-Filtern. Daher wurden keine FIT-Dateiindizes angefordert und keine FIT-Dateien abgelegt. Retry=0; 5007 und 5031 nicht wiederholt; kein SQLite-Import und keine schreibenden Archiv-/Flag-/Lösch-/CLOSE_ALL-Befehle. Der Einmal-Gate `legacyHistoryDownloadAttempted` ist jetzt verbraucht. Lokale App-Kurzmeldung meldet: „Directory gelesen; keine passenden lesbaren, unterstützten FIT-Einträge innerhalb der Limits.“

### Quellenvergleich und nächste unterscheidende Schritte

- **Garmin Connect 5.31:** Der Legacy-Pending-Upload-Pfad ist `5030 HANDSHAKE_COMPLETE` → ggf. FULL_SYNC → `5007`-Filter → optional `5031` → `5002` Directory → `5004`-Dateidaten. Der heutige Lauf hat absichtlich keinen neuen 5007 gesendet; deshalb ist die einzelne Directory-Zeile nicht als vollständige Bandhistorie oder als „keine Historie vorhanden“ zu interpretieren. Im Code ist der offline rekonstruierte Connect-5030-Body `[08 00]` vorhanden, aber noch nicht an den Live-Pfad angeschlossen.
- **GarminHRM600:** Die öffentliche `bootstrap.py` enthält explizite fenix-8-Wachemulationsbytes (u. a. Geräteidentität) sowie Compact-/Core-Feature-Antworten; der Client verwendet initiale Compact-Counter `0x01EF` und Sequence `0x0A`. Dies ist ein HRM600/fenix-8-spezifischer Emulatorpfad, kein CIRQA-Trace. Diese Bytes, fremde Identität und Counter werden nicht kopiert oder gesendet.
- **Amazfish:** `garminsynchronizationmessage.cpp` behandelt 5037 eingehend, validiert 4-/8-Byte-Bitmasken und sendet danach ACK sowie ggf. Filter 5007; der Code markiert die Integration ausdrücklich als TODO. CIRQA hat bisher kein 5037 geliefert. Das belegt eine Implementierungsstrategie, nicht CIRQA-Semantik.
- **Gadgetbridge:** Der geprüfte V2-Communicator sendet beim Initialisieren `CLOSE_ALL`; dies ist für unseren Pfad ausgeschlossen. Zusätzlich ist die Lizenz vor Übernahme von Code zu prüfen. Es wird nichts daraus kopiert.
- **Offizielle Garmin-Alternative:** Garmin Support beschreibt individuellen FIT-Export über die Connect-Website sowie einen vollständigen Kontodatenexport, der Wellness-FIT-Dateien einschließen kann ([Garmin Support](https://support.garmin.com/en-US/?faq=W1TvTPW8JZ6LfJSfK512Q8)). Das ist ein Cloud-/Kontodatenexport, kein CIRQA-Direktabruf; PulseLibre hat keine Zugangsdaten erhalten und fordert keinen Export an.

Der implementierte Follow-up-Pfad ergänzt deshalb ausschließlich redigierte Directory-Felder (Index, Typ/Subtyp, READ, Größenwert, Auswahlgrund; ohne Zeitstempel, Identifierbytes oder Rohdaten). Er hat einen eigenen persistenten Einmal-Gate und wiederverwendet nicht den verbrauchten ersten Gate. Ein Folgelauf bleibt vollständig durch denselben Directory-/FIT-/Zeitlimitrahmen begrenzt. Beim nächsten verfügbaren S25 wäre der nächste **unterscheidende** Versuch der Connect-kompatible 5030-/5007-Übergang; vor einem aktiven Senden sind exakte Reihenfolge, Statuskorrelation, Stop-Regeln und ein separates Einmal-Gate zu implementieren und zu prüfen. Kein HRM600-Wachemulations- oder Gadgetbridge-CLOSE_ALL-Pfad.
