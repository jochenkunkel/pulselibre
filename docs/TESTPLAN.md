# PulsLibre – manueller Testplan für Galaxy S25

Stand: 2026-11-17. Grundlage: Android-CI-Build `23c2cfd9`, Run `44101529585` (success), signiertes Artifact `pulselibre-signed-update-apk`. Nachfolgende reine Branding-/Dokumentationsänderungen sind **nicht Bestandteil** dieser APK. Ziel: sichere Abnahme vorhandener App-Funktionen; der separat freigegebene einmalige GFDI-5007-Kompatibilitätstest bleibt ein eigener, eng begrenzter Versuch.

## Vorbereitungen und Sicherheitsregeln

- Galaxy S25, Android-Version und PulsLibre-Versionsnummer notieren; CIRQA-Akkustand und Nähe zum Smartphone prüfen.
- Vor einem Update bei Bedarf lokale Messdaten und Diagnosen exportieren. **Nicht deinstallieren**, falls die Signatur nicht passt: App-private Daten können verloren gehen.
- Garmin Connect nicht entkoppeln; CIRQA nicht zurücksetzen.
- Bei einer Sicherheitswarnung, unerwarteten BLE-Aktion oder instabiler Verbindung abbrechen.
- Keine experimentellen Buttons für `5050`, synthetisches `SYNC`, `Bootstrap`, `Dateiliste`, `Garmin-ML-Handshake`, `MousePods` oder `CLOSE_ALL` auslösen. Diese sind **nicht** Teil des freigegebenen 5007-Einzeltests.
- Keine Gesundheitsdaten, MAC-Adressen oder ungeschwärzten Diagnoseprotokolle öffentlich posten.

## Testfälle (Status jeweils: OK / FEHLER / NICHT GETESTET)

| ID | Priorität | Durchführung | Erwartetes Ergebnis |
|---|---|---|---|
| T01 | P0 | APK über vorhandene Installation installieren | Installation klappt ohne Datenverlust; bei Signaturkonflikt abbrechen, nicht deinstallieren |
| T02 | P0 | App starten, Übersicht anzeigen | Kein Absturz, Dashboard und Navigationsflächen sichtbar |
| T03 | P1 | Hoch-/Querformat, große Schrift und Dark Mode prüfen | Texte und Schaltflächen bleiben bedienbar; Abweichungen dokumentieren |
| T04 | P1 | Oberarmbruch-Icon oben antippen | Einstellungen/Profil öffnen; Icon hat eine verständliche Accessibility-Beschreibung |
| T05 | P0 | Körperprofil ändern, App schließen und neu öffnen | Eingaben bleiben gespeichert |
| T06 | P0 | Dashboard ohne aktuelle Messungen ansehen | Keine erfundenen Messwerte; Empfangszeit wird nicht als historische Messzeit dargestellt |
| T07 | P0 | Bluetooth ausschalten und App erneut öffnen | Verständlicher Status, kein Absturz; Oberarmbruch-Icon schwarz offline, grün nur bei aktuell bestätigter CIRQA-GATT-Verbindung |
| T08 | P0 | Bluetooth-/Nearby-Devices-Berechtigung verweigern und anschließend gewähren | Keine Abstürze, kontrollierter Wiederholungsversuch |
| T09 | P0 | Unter Bluetooth/Geräte die Gerätesuche starten | CIRQA wird bei Sichtbarkeit erkannt; kein Zwang zum Entkoppeln |
| T10 | P1 | CIRQA-Eintrag und RSSI während des Scans beobachten | Plausible Anzeige, keine unnötigen Duplikate |
| T11 | P0 | Lokale Aufzeichnung starten und anschließend stoppen (vorhandene Standard-Steuerung) | Sichtbarer Zustandswechsel, kein Absturz |
| T12 | P1 | Dashboard nach der Aufzeichnung prüfen | Nur tatsächlich empfangene Daten erscheinen; kein Versprechen historischer Bewegungssensordaten |
| T13 | P0 | Datenexport öffnen, Zeitraum wählen und CSV speichern | CSV entsteht oder verständliche Fehlermeldung; Export enthält nur lokale Daten |
| T14 | P1 | App in Hintergrund/Vordergrund wechseln und neu starten | Keine Abstürze; Profil und gespeicherte Daten bleiben erhalten |
| T15 | P1 | Diagnose-Teilen nur bei Bedarf prüfen | Android-Teildialog erscheint; Protokoll vor Weitergabe schwärzen |
| T16 | P1 | Anzeige mit TalkBack/Screenreader prüfen | Navigation und Einstellungen sind verständlich benannt |

## Nicht Gegenstand dieses Builds

- Ein echter historischer CIRQA-FIT-Download ist **nicht** implementiert oder freigegeben.
- `CirqaLegacy5002Codec`, `CirqaLegacyCrc16`, `CirqaLegacyOfflineFileSession` und `CirqaVerifiedFitStore` sind reine Offline-Bausteine. Es gibt keinen UI-Test für sie.
- Die `yw40`-CRC-Vergleichsvariable wurde anhand von `classes.dex`/`dexdump` als aktualisierter Endakkumulator bestätigt (DEX-Code-Unit-Offset `0x0042`, `if-eq v7,v4`). Connects Protokollbelege beweisen weiterhin keine CIRQA-Kompatibilität.
- Der normale CI-Build führt JVM-Unit-Tests und APK-Assembly aus, **nicht** automatisch Emulator-/Instrumentierungs- und vollständige Lint-Tests.

## Ergebnisprotokoll (bitte zurückmelden)

```text
Gerät: Galaxy S25
Android-Version:
PulsLibre-Version / versionCode:
Installation als Update: OK / FEHLER
T01:
T02:
T03:
T04:
T05:
T06:
T07:
T08:
T09:
T10:
T11:
T12:
T13:
T14:
T15:
T16:
Abstürze / sichtbare Fehlermeldungen:
Screenshots (ohne private Daten):
Weitere Beobachtungen:
```

## Namens- und Codeabgleich (2026-11-17)

- **Produktname:** `PulsLibre` ist die verbindliche Projektschreibweise. In `MainActivity.kt` stand `PulseLibre` als sichtbarer Header; auf `main` korrigiert. Der ältere signierte Testbuild `23c2cfd9` zeigt noch `PulseLibre`.
- **Paketname:** `de.pulselibre` und `applicationId` `de.pulselibre` sind konsistent. Nicht ändern: Android-Updates hängen an der stabilen ID.
- **Dateinamen:** `CirqaLegacy5002Codec`, `CirqaLegacyCrc16`, `CirqaVerifiedFitStore` bezeichnen ihre Funktionen nachvollziehbar. `Cirqa` bezeichnet den Zielgeräte-/Projektkontext, **nicht** nachgewiesene CIRQA-Wire-Kompatibilität.
- **Veraltete Kommentare:** `CirqaLegacyTransferDecoder` behauptete noch, die 5002-Feldbreiten und `u45`-CRC seien unbekannt. Auf `main` korrigiert. `readResponse().sizeBytes` ist ein historischer Name für vier rohe Größenbytes; der typisierte unsigned Wert liegt jetzt in `CirqaLegacy5002Codec.Response.unsignedValue`. Umbenennung nur mit abgestimmter API-Migration.
- **Produktversprechen:** Begriffe wie „Historien-Diagnose“, „Dateiliste lesen“, „Bewegungssensor“ sind **experimentelle Diagnosefunktionen**, kein Nachweis eines funktionierenden Historienimports.
- **Testdokumentation:** Der alte Testplan trug den Titel „PulseLibre 0.1“ und beschrieb einen älteren BLE-Scanner-Stand. Dieser Plan ersetzt ihn. Keine Behauptung einer vollständigen Prüfung aller Quellbezeichner: geprüft wurden Paketstruktur, relevante Kotlin-Klassen, UI-Labels und aktuelle Dokumentation.

## T07 – Verbindungsrand am Oberarmbruch-Icon

**Implementiert in `codex/t07-connection-indicator`:** Das Einstellungsicon zeigt einen schwarzen inneren Rand im Offline-Zustand und einen grünen Rand, wenn Android aktuell für ein gekoppeltes Gerät mit CIRQA-Namen `GATT/STATE_CONNECTED` meldet. Eine weiße Außenkontur hält den schwarzen Rand auch im dunklen Theme sichtbar. Das 48-dp-Icon bleibt derselbe Einstellungsbutton.

Der Status wird bei App-Wiederaufnahme und anschließend jede Sekunde neu aus dem Android-Bluetoothmanager abgefragt. Bluetooth aus, fehlende Berechtigung, kein gekoppeltes CIRQA oder jeder Zustand außer `STATE_CONNECTED` bleiben offline. Es werden weder Scan-Ergebnisse noch gespeicherte Verbindungsflags verwendet; die App startet keine Verbindung. Die Android-Systemabfrage belegt **nicht**, welche App die GATT-Verbindung hält. TalkBack nennt deshalb ausdrücklich „Android meldet …; verbundene App unbekannt“.

**Automatisiert:** Policy-Unit-Tests decken Bluetooth aus, fehlende Berechtigung, fehlende Kopplung und getrennten GATT-Zustand ab. Activity-Test prüft die sichtbare Statusbeschreibung, Fokusierbarkeit und 48-dp-Touchfläche.

**Manueller S25-Abnahmetest – nach Installation des PR-Builds noch auszuführen:** Bluetooth aus → schwarz; Bluetooth an, CIRQA nicht verbunden → schwarz; CIRQA-GATT-Verbindung aktiv → grün; Verbindung trennen oder Band außer Reichweite → schwarz; T04 erneut antippen → Einstellungen öffnen. Dunkles Theme prüfen. Ergebnisse je Schritt als OK/FEHLER/NICHT GETESTET ergänzen. CI oder ein Emulator ersetzt diesen Hardwaretest nicht.

## Manuelle Rückmeldung Galaxy S25 – 2026-11-17

Der Tester meldet: **„bis auf 16 ist alles ok“**. Daher werden **T01–T15 als vom Benutzer bestanden gemeldet** erfasst (kein unabhängiger Hardware-Nachweis durch CI). **T16 ist offen:** Noch nicht klar, ob TalkBack/Barrierefreiheit fehlgeschlagen oder lediglich nicht getestet wurde. Diese Unterscheidung vor einer Freigabe nachfragen. Kein vollständiger Release-Gate; Historien-FileSync weiterhin unbewiesen.

**T07-Hardwareabnahme bleibt offen:** Die historische manuelle Meldung „bis auf 16 ist alles ok“ bezog sich auf den früheren Testbuild ohne Verbindungsrand und gilt nicht als Test dieses Features.

## CIRQA-Testlabor – Issue #20

Diese Schritte sind ein **noch auszuführender** S25-Testplan. Automatisierte JVM-/Emulatortests ersetzen keine CIRQA-Hardwareprüfung. Testlabor öffnen über den Tab **Testlabor**. Es gibt keinen Historien-Downloadbutton.

### Vorbedingungen und Stufen

- Sicherstellen, dass die installierte APK aus vertrauenswürdigem Build stammt. Bei Update-Signaturkonflikt abbrechen und **nicht deinstallieren**.
- Garmin Connect nicht entkoppeln; CIRQA nicht zurücksetzen. Vor dem bestehenden aktiven Live-Messwerttest Garmin Connect schließen.
- Stufe 1 umfasst passiven Scan, Android-Statusbeobachtung, vorhandene read-only GATT-Dienstsuche, vorhandene Puls-/Schritte-Liveprobe und lokale SAF-FIT-Analyse.
- Nur ausdrücklich freigegebene GFDI-5007-Einzelversuche dürfen über ihren jeweiligen Bestätigungsdialog ausgeführt werden. Der persistente App-Gate begrenzt die aktuell erteilte Gesamtfreigabe auf drei Testläufe; pro Lauf höchstens ein 5007-Write, keine automatischen Wiederholungen oder Folgekommandos. Stufe 3 (Archivieren/Löschen/Reset/Firmware/Flags) bleibt gesperrt.
- Bei unerwartetem BLE-Verhalten, Garmin-Connect-Abbruch oder sichtbaren Daten-/Berechtigungsproblemen sofort abbrechen. Keine Gerätekennungen oder Messwerte in öffentliche Tickets/Chats kopieren.

### Testfälle

| ID | Durchführung | Erwartetes Ergebnis / Einordnung |
|---|---|---|
| T17 | Testlabor öffnen, Dunkelmodus und große Schrift prüfen | Keine automatische Verbindung/Anfrage. Download-NO-GO ist sichtbar; Status nennt Ergebnis und Evidenzklasse getrennt. Buttons sind mit TalkBack bedienbar. |
| T18 | Bluetoothstatus einmal abfragen; optional den 30-s-Monitor starten | Nur Androids Bond-/GATT-Systemstatus und Statuswechsel; kein Verbindungsaufbau. Verantwortliche App bleibt unbekannt. |
| T19 | Passiven 15-s-Scan starten und optional stoppen | Keine GATT-Verbindung. Nur Zähler, Garmin-Hinweis, flüchtiger Bond-Adressabgleich und begrenzter RSSI-Überblick; kein Identitäts-/Entfernungsbeweis. |
| T20 | Read-only-GATT-Dienstsuche nach separater Dialogbestätigung | Vorhandene `CirqaGattProbe` verbindet temporär, sucht nur Services/Characteristics und trennt spätestens nach 15 s. Keine GATT-Lese-/Schreiboperation oder Notifications. Garmin Connect kann getrennt werden. |
| T21 | Bestehende Puls-/Schritte-Liveprobe nur nach eigenem Bestätigungsdialog | Bestehender unterstützter Livepfad; keine neue Protokolloperation. Echte empfangene Werte können lokal gespeichert werden. Das ist weder Historienimport noch Stufe-2-FileSync. |
| T22 | FIT-Datei über SAF auswählen (nur autorisierte Testdatei) | Lokal: Envelope-/CRC-Validierung und ausgewählte unterstützte Felder, Zeitstempel, Einheiten, Qualitätsflags, fehlende Werte und mögliche Duplikate prüfen. Max. 8 MiB/50.000 Beobachtungen, max. 80 Einzelzeilen in der Anzeige. Keine Kopie und kein SQLite-Import. |
| T23 | Standard-Testbericht speichern und Datei vor Weitergabe prüfen | Export enthält Status, Zeitpunkt und sichere Zähler/Ergebniszusammenfassungen, keine Gerätekennung, Dateinamen, Rohpayloads oder Messwerte. SAF kann Cloud-Ziele anbieten; nur vertrauenswürdigen Speicherort wählen. |
| T24 | Im Labor nach historischem Download / `0x8A39` suchen | Kein Download-Button und kein `0x8A39`; Historienstatus bleibt „Nicht getestet / Unbewiesen / gesperrt“. Keine 5031/5002/5004-Folgekommandos. |
| T25 | Bereich „Passiv beobachtete Protokolle und lokale Logs“ ansehen | Das Labor sagt ausdrücklich, dass keine passiven CIRQA-Rohframes gespeichert sind. SQLite-Zähler sind App-Ereignisse und kein passiver Protokollmitschnitt. |
| T26 | GFDI-5007-Karte im Testlabor prüfen | Bytepreview zeigt den Connect-5.31-Kandidaten. Jeder ausdrücklich freigegebene Versuch verlangt direkte Bestätigung; der persistente Gate lässt maximal drei freigegebene Testläufe zu und sendet pro Lauf höchstens einen 5007-Write. Keine CIRQA-Kompatibilität ohne passende CRC-validierte Antwort behaupten. |

**Stufe-2-Vorschlag:** siehe [CIRQA_TEST_LAB_STAGE2_PROPOSAL_2026-11-17.md](protocol/experiments/CIRQA_TEST_LAB_STAGE2_PROPOSAL_2026-11-17.md). Nur der gesondert freigegebene 5007-Test ist erlaubt; jede andere Stufe benötigt eine neue ausdrückliche Freigabe.

**Issue #22 / 5007:** [Byte-/Risiko-Review und kontrollierte Implementierung](protocol/experiments/CIRQA_5007_CONTROLLED_TEST_PREPARATION_2026-11-17.md). Der Request bleibt auf genau einen dynamisch gerouteten Write mit fünf Sekunden Beobachtungsfenster begrenzt.

```text
Testlabor APK/Version:
T17: NICHT GETESTET
T18: NICHT GETESTET
T19: NICHT GETESTET
T20: NICHT GETESTET
T21: NICHT GETESTET
T22: NICHT GETESTET
T23: NICHT GETESTET
T24: NICHT GETESTET
T25: NICHT GETESTET
T26: NICHT GETESTET
Abbruch / beobachtete Nebenwirkung:
```

## CI-Neustart nach Wiederherstellung der öffentlichen Repository-Sichtbarkeit (2026-11-17)

Der Repository-API-Status wurde als `private=false`, `visibility=public` geprüft. Die vorherigen Android-CI-Läufe (zuletzt `44126931755` für `cd050708`) endeten mit `failure`; die genaue Ursache ist damit **nicht** nachgewiesen. Dieser Dokumentationscommit löst einen neuen `push`-Build auf `main` aus. Erst ein erfolgreich abgeschlossener Lauf mit vorhandenem signiertem APK-Artefakt erlaubt die Ausgabe als installierbares Update. Eine geänderte Sichtbarkeit allein beweist keine Reparatur des Builds. Der CIRQA-Historiendownload bleibt NO-GO.
