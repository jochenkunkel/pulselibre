# S10 Produktdownload – Build 1757 (22.11.2026)

## Freigabe und Vorbereitung

- Nutzer hat Installation, erforderliche Berechtigungen und einen
  Historien-Download auf dem S10 ausdrücklich freigegeben.
- Gerät: Samsung SM-G970F, Android 12 / API 31, eindeutig per ADB identifiziert.
  Kein S25 angesprochen; sämtliche Gerätezugriffe unter atomarer Host-Lease.
- PulsLibre war zunächst nicht installiert. Erstinstallation der signierten
  Release-APK `de.pulselibre`, Version `2.0.0_build1757`, VersionCode 1757.
  Keine Deinstallation, kein Datenlöschen, keine Instrumentationstests.
- Quelle: Main-Commit `4ae1c1025700f14e1389af668212f30eaa0dc22d`;
  FULL-CI [44997585775](https://github.com/jochenkunkel/pulselibre/actions/runs/44997585775)
  inklusive Build/Unit/Lint/APK und API-35-Emulator vollständig erfolgreich.
  Release-Artefakt `pulselibre-signed-release-update-apk` geladen;
  `apksigner verify` und Paket-/Versions-/minSdk-Prüfung erfolgreich.
- Nearby-Devices-Dialog sichtbar; anschließend BLUETOOTH_SCAN und
  BLUETOOTH_CONNECT als gewährt nachgewiesen. Keine Kopplung verändert.

## Tatsächlich beobachteter App-/Downloadlauf

Appstart und Geräteansicht funktionieren. Nach der Berechtigungsfreigabe lag
bereits ein abgeschlossener Produktlauf in der Geräteansicht vor. Es wurde
**keine zusätzliche Downloadbestätigung** durch den Agenten ausgelöst.
Der genaue Auslöser dieses ersten Laufs (App-Entry-Automatik oder Nutzeraktion)
wurde nicht separat mitgeschnitten. Es gab keinen zweiten Download oder Retry.

Die lokale UI-Statusmeldung weist aus:

| Stufe / Status | Ergebnis |
|---|---:|
| `accepted`, `itemListSent`, `pullSent` | jeweils true |
| ausgewählte Dateien / Skips | 24 / 0 |
| übertragene Bytes / MLR-Chunks | 485.654 / 3.338 |
| erfolgreich / nicht erfolgreich | 24 / 0 |
| übertragen / verifiziert / privat gespeichert | jeweils 24 |
| dekodierte FIT-Dateien / SQLite-Commit-Stufe | jeweils 23 |
| Dateien mit Importstatus / ohne importierbare Daten | 17 / 6 |
| unsupported / failed | 0 / 0 |
| Listenseiten / Transaktions-Folgeanfragen | 4 / 3 |
| letzte Folgeantwort | 0 Einträge |
| `runDrained` | false |
| `incompleteReason` | `next-transaction-followup-empty` |
| `coverage` | `device-history-unverified` |

**17 ist eine Dateianzahl, keine Anzahl neuer Messwerte.** Die Stufenmeldung
belegt nicht separat die Anzahl neuer SQLite-Beobachtungen oder Idempotenz.
Die FIT-/Datei-/SQLite-Ergebnisse stammen aus dem Produktreport, nicht aus einer
zusätzlichen unabhängigen Extraktion der privaten App-Datenbank.

## Grenzen und Abschluss

- **Folgediagnose auf Nutzeranweisung, read-only (22.11.2026):** ADB bestätigte
  erneut SM-G970F / Android 12 / API 31, Build 1757 sowie gewährte
  `BLUETOOTH_SCAN`- und `BLUETOOTH_CONNECT`-Berechtigungen. Zum Diagnosezeitpunkt
  wurde kein `CirqaContinuousRecordingService` als aktiv gemeldet. Das ist
  lediglich der aktuelle Zustand, kein Beleg für den Ausgang eines früheren
  Startversuchs. Die gefilterte aktuelle Logcat-Rückschau enthielt keine
  einschlägigen App-/GATT-Zeilen.
- Eine private SQLite-Prüfung war mit dieser nicht-debuggbaren Release-App nicht
  möglich (`run-as` verweigert den Zugriff); `allowBackup=false`. Es wurde
  weder die App neu gestartet noch ein Export/Backup, Sync, Scan oder GATT-
  Verbindungsaufbau ausgelöst, um keinen unbeabsichtigten automatischen Sync
  oder zusätzliche Datenoperation zu verursachen. Die tatsächliche Zahl und
  Metrik der importierten Beobachtungen bleibt daher ungeprüft.
- Codeabgleich: In `MainActivity` und `CirqaMlHandshakeProbe.start()` ist keine
  fest codierte S25-MAC vorhanden. Beide Geräteauswahlpfade verwenden gekoppelte
  Geräte mit Namen, die „CIRQA“ enthalten; die ML-Probe nimmt den ersten
  passenden Eintrag. Mehrere gleichnamige Kopplungen wären ein möglicher
  Auswahlkonflikt, aber nicht als Ursache belegt. Live-Aufzeichnung erfordert
  zusätzlich eine erfolgreiche GATT-Dienstsuche, Notify-/Write-Characteristics,
  CCCD-Aktivierung, ML-Registrierung und anschließende Messwert-Notifications.
- Der Verlauf projiziert FIT-Herzfrequenz (Metrik `heart_rate`, Einheit `bpm`,
  plausible Werte 1–240) separat von Live-Ereignissen. Standardmäßig ist nur
  das letzte 24-Stunden-Fenster ausgewählt; rückwärts navigierbar ist höchstens
  der lokale 30-Tage-Horizont. `imported=17` belegt weder, dass darin
  Herzfrequenz enthalten ist, noch dass deren Messzeit in diesem Fenster liegt.
  Build 1757 enthält zudem den nachfolgenden Decoder-Fix #416 nicht; ob genau
  das die fehlenden Verlaufspunkte erklärt, wurde auf diesem Gerät nicht
  nachgewiesen.

- Dies ist ein realer S10-Produktdownload mit gemeldeter Integritätsprüfung und
  Importstufen, aber **kein Nachweis vollständiger Gerätehistorie**.
- Build 1757 enthält den neuen Offline-zlib-Decoder aus PR #416 nicht. Dieser
  Test belegt deshalb weder dessen Live-Integration noch komprimierte
  Live-Übertragung.
- Keine experimentellen BLE-Sequenzen, Reset-, Delete-, Archive- oder
  SetFlags-Kommandos. Der bestehende Sync-Request kann geräteseitig Daten als
  übertragen markieren; dies war vorab offengelegt.
- Nach Sicherung der privaten UI-Evidenz App per `am force-stop` beendet, um
  weitere automatische App-Entry-Läufe zu vermeiden. Installation und Daten
  bleiben erhalten; Host-Lease danach als FREI geprüft.
- UI-/Screenshot-/Rohstatusdateien ausschließlich privat lokal; keine
  Gerätekennungen oder Messwerte veröffentlicht.

Nächster technischer Schritt: vorhandene private FIT-/SQLite-Daten bei Bedarf
über einen regulären Export unabhängig abgleichen und die noch offene
Historienabdeckung klären. Kein zusätzlicher Gerätelauf durch diesen Bericht
freigegeben.
