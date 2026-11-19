# Code-Review „von oben bis unten“ – 2026-11-19 (Tagesabschluss)

**Umfang:** gesamter Quellbaum `app/src/main` (≈ 20 750 Zeilen Kotlin), mit Schwerpunkt auf den von Claude Code verantworteten GUI-Dateien. Statisch geprüft (Suche nach Mustern, Lesen der betroffenen Stellen), plus Build/Unit-Tests/Lint lokal grün. **Nicht** geprüft: BLE/GFDI/FIT-Protokollcode (Zuständigkeit OpenCode), Laufzeitverhalten auf dem S25 mit Band.
Bewertung: **hoch** = Datenverlust/Fehlverhalten möglich, **mittel** = Inkonsistenz mit Nutzerwirkung, **niedrig** = Wartbarkeit.

## Gefunden

| # | Stufe | Fund | Wo | Empfehlung |
|---|---|---|---|---|
| 1 | mittel | **Icon-Umstellung (#157) nicht vollständig:** in der normalen UI stehen noch Emojis/Glyphen: Profilkarte (`👤`, `↕ ⚖ ◷ ▣`), Überschrift „Gesundheit · CIRQA“ (`📡`), Verlauf-Datumszeile (`📅`), Daten-Tab (`🔒`), Info-Dialogzeilen (`▬ ▰ ↗ ◷ ⚠`), Nacht-Indikatoren (`◉ ↧`), Aufzeichnungsstatus (`○ ◌`). | `MainActivity.kt` 321/413–418/450/523–566, `HistoryOverviewView.kt` 99, `ExportActivity.kt` 254, `DashboardOverviewView.kt` 210–221/652–704 | Folgeticket: auf `PlIcon` umstellen (Profilkarte zuerst). |
| 2 | mittel | **„Profil löschen“ leert den ganzen Prefs-Store** `local_personal_profile`, in dem auch SpO₂-Schwellen und das Validierungs-Gate (`spo2_device_validation_recorded`) liegen. Das Gate soll laut Code „nicht über normale Einstellungen erreichbar“ sein. | `PersonalSettings.kt` (`p.edit().clear()`), `SpO2DisplaySettings.kt` | Profil-Schlüssel gezielt entfernen oder Gate/Schwellen in eigenen Store (wie `recording_settings`). |
| 3 | mittel | **Typ-Inkonsistenz im selben Store:** Schwellen als `Int`, restliche Profilfelder als `String`. Das hat heute den Konfig-Absturz verursacht (behoben, PR #161). Gleiche Gefahr für andere Schlüssel. | `PersonalSettings.newField`, `SpO2DisplaySettings`, `ActivityDetectionPreference` | Ein typisierter Zugriff (Profil-Repository) statt freier Schlüssel; Regressionstest existiert für SpO₂. |
| 4 | niedrig | Der Store-Name `"local_personal_profile"` steht 6× als Literal im Code und in Tests; Schlüssel sind Freitext. | `MainActivity`, `PersonalSettings`, `SpO2DisplaySettings`, Tests | Konstante / Repository. |
| 5 | niedrig | **Duplizierter Code:** Glyph→`DashboardInfoIconView.Kind`-Mapping und Info-Zeilenaufbau doppelt in Dashboard und Verlauf. | `DashboardOverviewView.kt` 652, `HistoryOverviewView.kt` 273 | Eine gemeinsame Hilfsfunktion. |
| 6 | niedrig | **Toter Code:** `PersonalSettings.avatar()` und `bodyAvatar()` werden nirgends benutzt (Emoji-Avatare). | `PersonalSettings.kt` 15–27 | entfernen. |
| 7 | niedrig | **46× `Thread { … runOnUiThread }`** ohne zentrale Abbruch-/Ersatzlogik; nur teilweise `destroyed`-Prüfung. | verteilt, v. a. `MainActivity` | Ein kleiner Executor + „latest request wins“-Token (Muster existiert bereits im Verlauf). |
| 8 | niedrig | **Sehr große Dateien:** `MainActivity` 1346, `CirqaTestLaboratoryActivity` 1622, `CirqaMlHandshakeProbe` 2647 Zeilen. | – | `MainActivity` schrittweise in Tab-Builder zerlegen (GUI, meine Zuständigkeit). Labor/Probe: OpenCode. |
| 9 | mittel | **Autostart nur teilweise getestet:** Entscheidungslogik per Unit-Test, Schalter per Instrumentationstest; der echte Start des Dienstes bei App-Start ist nicht automatisiert prüfbar (BLE) und am Band **nicht** getestet. Das Gate verlangt Bluetooth+Rechte schon vorhanden; schlägt `startForegroundService` fehl, passiert still nichts (kein Hinweis in der UI). | `MainActivity.autoStartRecordingIfEnabled` | Bei Fehlschlag Statuszeile setzen; Test am S25 nach Freigabe. |
| 10 | mittel | **CI-Abhängigkeit/Prozess:** Läufe scheitern ohne Schritte („job was not started because recent account payments have failed“), heute zweimal. Dadurch ließen sich PR #166 und der Untertitel-Fix nicht per grüner CI mergen. Zusätzlich Hinweis auf `ubuntu-latest`→Ubuntu 26 ab 19.10.2026. | `.github/workflows/android.yml` | Billing klären; `ubuntu-24.04` pinnen (→ #156). |
| 11 | mittel | Release-/Debug: weiterhin debuggbare Update-APK (#154); hilfreich für Tests (`run-as`), aber für Gesundheitsdaten ein Risiko. | `build.gradle.kts` | #154 |
| 12 | niedrig | Zwei harte Farbcodes-Gruppen (`Color.parseColor`/`rgb`) in `MainActivity` (5), `DashboardOverviewView` (5), `DeviceOverviewView` (3) umgehen `AppPalette`; im Hellmodus nicht alle geprüft. | – | Hellmodus-Durchgang der Übersicht. |
| 13 | niedrig | Konfig-Dialog: Autostart-Schalter sitzt zwischen „Sonstiges“ und „Darstellung“; Reihenfolge ist Geschmackssache und noch nicht vom Nutzer bestätigt. | `PersonalSettings.kt` | Nutzerentscheidung. |

## Heute behoben (zur Einordnung)
Konfig-Absturz (#161), Profilkarte nach Speichern über Header-Button (#162), Config-Icons/-Reihenfolge/Autostart (#163), Header-Berge (#164, PR #166, CI wegen Billing offen), Untertitel-Umbruch (eigener PR).

## Zusätzliche Beobachtung (Vorfall, nicht Code)
Auf dem S25 stand nach einem App-Absturz „Datenbank aus Backup wiederhergestellt“ (DB danach 6,7 MB). Geprüft: `ExportActivity.confirmRestore` zeigt vor dem Überschreiben einen Dialog mit Backup-Datum und Zählern und verlangt „Ersetzen“; die Wiederherstellung musste also ausdrücklich bestätigt werden (nicht durch einen unbeabsichtigten Einzel-Tap möglich). Code ist hier in Ordnung; wer bestätigt hat, ist aus den Logs nicht ersichtlich.

## Nachtrag: Review-Punkte behoben (Branch claude/review-2026-11-19)
- **#2 / Ticket #169:** „Profil löschen“ entfernt nur noch Profilschlüssel; SpO₂-Schwellen und Validierungs-Gate bleiben (`SpO2DisplaySettings.PRESERVED_KEYS`). Instrumentationstest `deletingTheProfileKeepsSpo2SettingsInTheSameStore` (lokal auf Emulator grün).
- **#4:** Store-Name als Konstante `SpO2DisplaySettings.STORE`, in `MainActivity`/`PersonalSettings` verwendet.
- **#6:** ungenutzte `avatar()`/`bodyAvatar()` entfernt.
- **#9:** Schlägt der automatische Start fehl, erscheint ein Hinweis (Toast) statt stillem Scheitern. Test am Band weiter offen.
- Offen bewusst: #1/#168 (Emoji→Icon, größerer Umbau), #3, #5, #7, #8, #12, #13, CI-Billing.
- **#1 teilweise (zu #168):** „Gesundheit · CIRQA“ (CIRQA_BAND, weiß auf Violett), Verlauf-Datumsknopf (CALENDAR) und Datenschutzzeile im Daten-Tab (STORAGE) nutzen jetzt `PlIcon`. Emulator-Sichtprüfung der Übersicht ok. **Offen:** Profilkarte (Person/Größe/Gewicht/Alter/BMI) – dafür fehlen passende Icons im Set v1, es braucht neue Entwürfe; Info-Dialogzeilen und Statusglyphen.
