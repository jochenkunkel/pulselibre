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

### Follow-up-Codeprüfung

Commit `a27e59b` ergänzt begrenzte Auswahlgründe und den separaten Follow-up-Gate. Lokale Verifikation mit SDK `/tmp/pulselibre-build-tools/sdk` und Gradle 8.13: `:app:testDebugUnitTest` sowie `:app:assembleDebug` erfolgreich; Debug-APK erzeugt. Der CI-Dispatch `44199029802` für diesen Commit wurde vor Jobstart wegen GitHub-Billing-/Ausgabenlimit abgebrochen. ADB zeigte bei der erneuten Prüfung keine angeschlossenen Geräte; der Follow-up wurde daher nicht ausgeführt und es wird kein neues Geräteergebnis behauptet.

### In-Place-Updateversuch und Datenbankschemaabweichung

Am 2026-11-18 wurde nach erfolgreichem CI-Run `44254317525` Build 946 installiert. Zertifikatsfingerabdruck entsprach dem installierten Build 942 (`36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`), `firstInstallTime` blieb unverändert; es war ein In-Place-Update ohne Deinstallation. Beim App-Start crashte `MainActivity`, bevor das Testlabor erreichbar war: `SQLiteException: Can't downgrade database from version 2 to 1`. Der CIRQA-Follow-up wurde **nicht** gestartet und es gingen keine BLE-Requests ab. Es wurde keine Datenbankdatei gelöscht oder verändert; der Fehler entsteht beim Öffnen des DB-Helpers vor `onOpen`.

Ursache: Der Historien-Branch basierte auf einem älteren `main` mit `CirqaDatabase`-Schema-Version 1, während die installierte App-Version 942 Schema-Version 2 besitzt. `origin/main` enthält die additive 1→2-Migration und die aktuellen App-Dateien. Diese Basis wurde lokal in den Historien-Branch gemergt (Merge-Commit `1ad3e3e`); danach liefen `:app:testDebugUnitTest` und `:app:assembleDebug` lokal erfolgreich. Vor einem weiteren Gerätezugriff ist ein neuer kompatibel signierter CI-Build von dieser aktualisierten Basis erforderlich. Keine Deinstallation, kein DB-Rollback und kein manuelles Schema-Umschreiben.

### Einmaliger Metadaten-Folgelauf (2026-11-18)

Nach erfolgreichem CI-Run `44255224280` wurde Build 949 (versionCode 949) installiert. Die Zertifikat-SHA-256 stimmte mit dem installierten Signer überein; In-Place-Update erfolgreich und `firstInstallTime` blieb erhalten. Die App startete mit Datenbankschema 2. Der separate Follow-up-Gate war vor dem Lauf unbenutzt; der erste Transfer-Gate war bereits verbraucht; der gespeicherte Status-0-5031-Typcache war vorhanden.

Der Nutzer bestätigte den Einmallauf in der App. Genau ein 5002-Request für Directory-Index 0 wurde verarbeitet; Retry=0. Ergebnis: **ein Eintrag**, `idx=0,type=0,subtype=0,read=false,flags=0x00,size=0`, Auswahlgrund `not-fit-type`. Es gab keine passenden FIT-Kandidaten, keine FIT-Requests und keine gespeicherten FIT-Dateien (`0` Dateien, `0` Bytes). Die Auswahlgründe sind im App-Bericht festgehalten; Zeitstempel, Identifierbytes und Rohdaten wurden nicht ausgegeben. Der Follow-up-Gate ist nun verbraucht. 5007/5031 wurden nicht wiederholt; kein Archivieren, Flag-Ändern, Löschen, CLOSE_ALL oder SQLite-Import.

**Hinweis zur bestehenden Startlogik:** `CirqaDatabase.onOpen()` ruft weiterhin `LegacyNightRecording.remove()` für `files/cirqa_overnight.jsonl` auf. Vor dem Start von Build 949 wurde die Existenz dieser Datei nicht geprüft; nach dem Start ist der Pfad nicht vorhanden. Daher lässt sich nicht feststellen, ob die Datei bereits vorher fehlte oder durch die bestehende App-Startlogik entfernt wurde. Diese Aktion war nicht Teil des CIRQA-Tests und nicht beabsichtigt; es wurde kein Wiederherstellungsversuch unternommen.
