# Code-Review Gesamtquelltext (Stand `main`, 2026-11-18)

Reviewer: Claude Code. Umfang: gesamter Produktionscode `app/src/main` (169 Kotlin-Dateien, ≈ 16 800 Zeilen), Manifest, Gradle, Ressourcen, Lint-Ergebnis (lokal, 190 Hinweise), statische Auswertungen per Suche. **Kein vollständiges Zeile-für-Zeile-Lesen**: tief gelesen wurden Datenbank, Backup/Restore, Recorder-Dienst, Dashboard/Verlauf/Einstellungen, eigene neue Klassen; die BLE-Protokolldateien (≈ 100 Dateien, Zuständigkeit OpenCode) wurden nur über Größe, Muster und Lint geprüft. Reine Lektüre, keine Codeänderung, kein Gerätetest.

## Was gut ist
- **Offline-first belegt:** keine `INTERNET`-Berechtigung, `allowBackup=false`, nur `MainActivity` exportiert, Dienst `exported=false`.
- **Datenbank:** parametrisierte SQL-Abfragen (einzige String-Einbettung ist eine feste Konstante in `CirqaDashboardReader`), additive Migration 1→2 mit Test, Downgrade verliert keine Daten (Test).
- **Backup/Restore (`DatabaseBackup`)** ist defensiv: Whitelist der Dateinamen, Verbot von Verzeichnissen/Duplikaten (kein Zip-Slip), Größenlimits je Eintrag/Archiv/Zeile, Validierung jeder Zeile, SHA-256 im Manifest.
- **Trennung Logik/UI:** viele reine Klassen (Parser, Gates, `TrendAnalysis`, `FitnessOverview`, `CirqaLinkStatus`) mit Unit-Tests (≈ 500 Tests).
- Cursor werden per `.use` geschlossen; DB-Zugriffe der Oberfläche laufen im Hintergrund (seit #58/#79).

## Befunde
### P1
1. **`POST_NOTIFICATIONS` fehlt im Manifest** (`AndroidManifest.xml`; Prüfung nur im Code `CirqaContinuousRecordingService.updateNotification`). Ab Android 13 kann die Berechtigung nie erteilt werden → die Dauerbenachrichtigung des Vordergrunddienstes (inkl. „Sammlung beenden“) ist unsichtbar bzw. nicht bedienbar; der Dienst läuft weiter. Entspricht nicht der Beschreibung im Start-Dialog („Android zeigt eine dauerhafte Benachrichtigung mit Stopp-Funktion“). Zudem wird die Berechtigung nirgends angefragt (`requestPermissions` nur für Bluetooth).

### P2
2. **Veraltetes „Recorder läuft“-Flag:** `KEY_RUNNING` (SharedPreferences) wird nur bei geordnetem Stopp zurückgesetzt. Bei Prozess-Kill/Absturz bleibt es `true` (Dienst ist `START_NOT_STICKY`), die UI zeigt dann „Aufzeichnungsdienst aktiv“ bzw. „Wiederverbindung“, obwohl nichts läuft (`MainActivity` ~461/1013, `ExportActivity` 354/374). Ansatz: Herzschlag-Zeitstempel (nutzt bereits `last_reading_ms`) oder Prüfung, ob der Dienst tatsächlich läuft. Bezug #3, #63.
3. **`CirqaDatabase` wird bei jedem Zugriff neu instanziiert** (16 Aufrufstellen) und `onOpen` führt jedes Mal Dateisystem-Arbeit aus (`LegacyNightRecording.remove`). Das Dashboard fragt jede Sekunde `latestEventId` ab → wiederholtes Öffnen/Schließen und Dateizugriff. Empfehlung: gemeinsame Instanz/Helper, Legacy-Bereinigung einmalig statt in `onOpen`.
4. **Monolithische Klassen:** `CirqaMlHandshakeProbe` (2 089 Zeilen), `CirqaTestLaboratoryActivity` (1 578), `MainActivity` (1 135: Dashboard, Geräteliste, BLE-Scan, Diagnose- und Experiment-UI in einer Klasse). Erschwert Tests (13 Dateien ohne Testbezug, siehe `TEST_INVENTORY_2026-11-18.md`) und Parallelarbeit der Agenten (Merge-Konflikte). Empfehlung: Experiment-/Diagnose-UI aus `MainActivity` auslagern; Probe in Zustandsmaschine + Transport trennen (OpenCode).
5. **Alle UI-Texte hartcodiert, keine `strings.xml`:** 174 Lint-Hinweise `SetTextI18n`; kein Layout-XML, UI komplett programmatisch. Kein Ressourcen-Theming außer dem neuen `Theme.PulsLibre`. Erschwert Übersetzung, einheitliche Textpflege und Konsistenzprüfungen; Barrierefreiheit ist ausschließlich über `contentDescription` gelöst (gut gepflegt, aber nicht durch Lint prüfbar).

### P3
6. **Ungenutzte Abhängigkeiten/Features:** `com.google.android.material` (keine Verwendung im Code) und `viewBinding = true` (keine Layouts) → größere APK/Buildzeit ohne Nutzen; ungenutzte Ressource `drawable/ic_pulselibre_legacy.xml`.
7. **Branding uneinheitlich:** App-Label und Notification-Titel „PulseLibre“, UI „PulsLibre“ (`AndroidManifest.xml` `android:label`, `CirqaContinuousRecordingService`, Diagnosetexte).
8. **Lint:** `DataExtractionRules` (Android 12+ Datenextraktionsregeln fehlen, bei `allowBackup=false` formal), `ViewConstructor` (8 benutzerdefinierte Views ohne XML-Konstruktor – gewollt, da nur programmatisch), `DrawAllocation` in `BodyProfileAvatarView` (3 Allokationen in `onDraw`), `ObsoleteSdkInt` (`CirqaMlHandshakeProbe` Z. 2010).
9. **Fehlerbehandlung:** 8 stille `catch (_: Exception)` in `ExportActivity`, 6 in `MainActivity` – Nutzer erhält teils nur generische Meldungen, Ursachen gehen verloren (kein lokales Diagnoselog dafür). `!!` in `CirqaMlHandshakeProbe` (6×), `TrendCardView`, Lab.
10. **Profil löschen** (`PersonalSettings`) leert den gesamten Preferences-Store `local_personal_profile`, der auch SpO₂-/Aktivitäts-Einstellungen enthält (nicht nur persönliche Angaben). Verhalten ist so dokumentiert („Profilangaben“), sollte aber getrennt gespeichert werden.

## Nicht geprüft
BLE-Protokollkorrektheit (CRC, Zustandsmaschine, Download), Gradle-/CI-Sicherheitsaspekte außer Workflowtitel, tatsächliche Laufzeit auf dem Gerät (Speicher, Energie, Akku), OWASP-MASVS-Vollabgleich, Zeilenabdeckung (kein JaCoCo-Lauf).

## Empfohlene Reihenfolge
1 (Benachrichtigungsrecht) → 2 (Recorder-Flag) → 3 (DB-Zugriff) → 6–8 (Aufräumen) → 5 (Strings) → 4 (Zerlegen, mit OpenCode abgestimmt).
