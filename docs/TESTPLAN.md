# PulsLibre – manueller Testplan für Galaxy S25

Stand: 2026-11-17. Grundlage: Android-CI-Build `23c2cfd9`, Run `44101529585` (success), signiertes Artifact `pulselibre-signed-update-apk`. Nachfolgende reine Branding-/Dokumentationsänderungen sind **nicht Bestandteil** dieser APK. Ziel: sichere Abnahme der **vorhandenen** App-Funktionen, **kein** CIRQA-FileSync-Freigabetest.

## Vorbereitungen und Sicherheitsregeln

- Galaxy S25, Android-Version und PulsLibre-Versionsnummer notieren; CIRQA-Akkustand und Nähe zum Smartphone prüfen.
- Vor einem Update bei Bedarf lokale Messdaten und Diagnosen exportieren. **Nicht deinstallieren**, falls die Signatur nicht passt: App-private Daten können verloren gehen.
- Garmin Connect nicht entkoppeln; CIRQA nicht zurücksetzen.
- Bei einer Sicherheitswarnung, unerwarteten BLE-Aktion oder instabiler Verbindung abbrechen.
- Keine experimentellen Buttons für `5050`, synthetisches `SYNC`, `Bootstrap`, `Dateiliste`, `Garmin-ML-Handshake`, `MousePods` oder `CLOSE_ALL` auslösen. Diese sind **nicht** Teil des sicheren manuellen Tests.
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
| T07 | P0 | Bluetooth ausschalten und App erneut öffnen | Verständlicher Status, kein Absturz |
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
- Die `yw40`-CRC-Vergleichsvariable muss noch im DEX bestätigt werden. Connects Protokollbelege beweisen keine CIRQA-Kompatibilität.
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

## Rückmeldung zu T07 – Verbindungsstatus am Oberarmbruch-Icon (2026-11-17)

**Featurewunsch / noch nicht implementiert:** Das Oberarmbruch-Einstellungsicon im oberen App-Header soll einen gut erkennbaren farbigen Rand als Statusanzeige erhalten:

- **Grüner Rand:** aktive, bestätigte BLE-Verbindung zur CIRQA-Armbanduhr.
- **Schwarzer Rand:** offline / keine aktive CIRQA-Verbindung, einschließlich Bluetooth ausgeschaltet oder Verbindung verloren.

Wichtig: Ein sichtbares BLE-Advertisement, eine gespeicherte Kopplung oder ein laufender Scan zählen **nicht** als aktive Verbindung. Der Rand muss bei Connect, Disconnect, Bluetooth-Off und App-Wiederaufnahme korrekt aktualisiert werden. Grün darf nicht nur wegen alter/gespeicherter Statusdaten angezeigt werden. Bei dunklem Design muss der schwarze Rand weiterhin klar sichtbar sein (z. B. mit zusätzlicher heller Kontur). Das Icon bleibt als Einstellungsbutton bedienbar.

**Erweiterter T07-Abnahmetest:** Bluetooth aus → schwarzer Rand; Bluetooth an ohne CIRQA-Verbindung → schwarz; CIRQA tatsächlich verbunden → grün; Verbindung trennen oder außer Reichweite → wieder schwarz. Den Befund als OK/FEHLER/NICHT GETESTET dokumentieren. **Im derzeitigen Testbuild nicht vorhanden**, daher zunächst als Featureanforderung und nicht als Testfehler behandeln.
