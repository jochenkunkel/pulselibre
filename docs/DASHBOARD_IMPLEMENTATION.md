# Dashboard: Bestandsaufnahme und erster Konzeptschritt

Stand: 2026-11-17. Basis: `f17f977`; Arbeitsbranch `codex/dashboard-history`.

## Bestand

Die Übersicht liest reale, lokal vorhandene Ereignisse über `CirqaDashboardReader`
aus der kanonischen `cirqa_measurements.db`. `MainActivity` aktualisiert die Anzeige
bei geänderter Ereignisanzahl. Profilangaben sind separat in SharedPreferences.
Die ältere Beschreibung von sechs statischen Platzhalterkarten ist überholt.
Das reine `HealthSnapshot`-Modell besteht für Offline-Funktionen und Tests fort.

Vor diesem Schritt wurden Herzfrequenz, Ruhepuls, Schritte und Schrittziel in vier
primären Kacheln gezeigt; SpO₂ und Schlaf standen in der nachgeordneten Liste.
Das entspricht noch nicht der freigegebenen Hierarchie aus
[APPROVED_UI_CONCEPT_2026-11-15.md](APPROVED_UI_CONCEPT_2026-11-15.md).

## Umsetzung im Review-Branch

- Puls und SpO₂ stehen nebeneinander, danach eine breite Schlafkarte.
- „Heute auf einen Blick“ enthält Tages-Schrittzähler und Geräte-Schrittziel.
  Kumulative Zähler werden nicht addiert; Intensitätsminuten werden wegen des
  unbestätigten Akkumulationsfensters nicht als heutige Aktivzeit bezeichnet.
- Ruhepuls, RR und alle weiteren vorhandenen Werte bleiben sichtbar.
- Empfangsnotizen und Werte werden unverändert übernommen. Fehlende Einträge
  bleiben „Keine Messdaten“; keine Schlafphasen oder Demonstrationswerte.
- Akzentfarben unterscheiden Kennzahlen, keine gesundheitlichen Risikoklassen.
- Unter 360 dp Breite oder ab Schriftfaktor 1,3 werden Kachelpaare gestapelt.
  Karten haben keine feste Texthöhe; Abschnittstitel sind Accessibility-Überschriften.
- Darstellung und Gruppierung liegen in `DashboardOverviewView` und dem reinen
  `DashboardOverview`-Modell; `MainActivity` bindet weiterhin den bestehenden Leser an.

Die Gruppierung nutzt die existierenden Titel des Readers als Präsentationsvertrag.
Bei künftigen Umbenennungen müssen Reader und Gruppierung gemeinsam angepasst werden.
Transport, Datenbankschema, Aufzeichnung und Export wurden nicht geändert.
Gemeinsame Status-/Build-Dateien bleiben wegen paralleler Protokollarbeit unangetastet;
Integration und Statusübernahme erfolgen nach Koordination im Pull Request.

## Datenherkunft und wissenschaftliche Grenzen

Geräteberichte gelten nach Nutzerklarstellung als vorläufige Beobachtungen.
Empfangszeit ist kein belegter Sensorzeitstempel. Es werden keine statistischen
Verfahren, medizinischen Schwellen oder neuen wissenschaftlichen Formeln eingeführt.
Daher entsteht in diesem Schritt keine neue wissenschaftliche Quellenbehauptung.
Maßgeblich bleiben [ENGINEERING_SCIENTIFIC_STANDARDS.md](ENGINEERING_SCIENTIFIC_STANDARDS.md)
und [CIRQA_DASHBOARD_GROUND_RULES.md](CIRQA_DASHBOARD_GROUND_RULES.md).

## Verifikation

- Neue JVM-Tests: leere Quelle, fehlendes SpO₂, umsortierte Eingabe,
  unveränderte Empfangsnotizen und Erhalt zusätzlicher Kennzahlen.
- Android-Tests: echte Activity-Sektionen, leere Karten, Reihenfolge, erhaltene
  Werte/Notizen, schmale Darstellung, Schriftfaktor 2 und Entfernen alter Werte.
- [PR #9](https://github.com/jochenkunkel/pulselibre/pull/9), Commit `a99536e`:
  [schneller CI-Lauf #558](https://github.com/jochenkunkel/pulselibre/actions/runs/44053469919)
  erfolgreich; Python-Tests, JVM-Tests und Debug-Build.
- Lokale Zusatzprüfung desselben Code-Stands am 2026-11-17 mit Gradle 8.13,
  JDK 17 und Android SDK 35, isolierte Werkzeuge/Caches unter `/tmp`:
  `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`
  **BUILD SUCCESSFUL**. 208 JVM-Tests in 53 Suites, keine Fehler oder übersprungenen Tests.
- Lint: keine Fehler, 60 Warnungen (u. a. Übersetzbarkeit, Abhängigkeiten und
  bestehende Manifest-/Protokollstellen). Keine Änderungen an diesen fremden Bereichen.
- Android-Test-APK erfolgreich kompiliert; Instrumentierungstests **nicht ausgeführt**.
  `/dev/kvm` fehlt lokal; kein beschleunigter Emulator für UI-/TalkBack-Abnahme.
  Full-CI und visuelle Geräteprüfung bleiben offen.
- SSH-Schreibzugang zum separaten Dashboard-Branch per Push-Trockenlauf bestätigt.
  Keine Änderungen an globaler Git-Konfiguration oder gemeinsamem Build/Workflow.
- Keine Geräteaktionen, keine Installations- oder Produktionsfreigabe.

## Nächste Schritte

1. Karten im Emulator bei verschiedenen Breiten, Schriftgrößen und mit TalkBack prüfen.
2. Reader-Tests für Zeitzonen, Tageswechsel, ungültige Daten und Empfangsalter ergänzen.
3. Zeitreihenmodell mit expliziten Lücken und Herkunft entwickeln; vor wissenschaftlichen
   Aggregationen Quellen, Abdeckung, Fehlwertregeln und Einheiten prüfen.
4. Verlauf im Emulator bei verschiedenen Schriftgrößen und mit TalkBack prüfen.

Noch offen: die bestehende Aktualisierung nur bei geänderter Ereignisanzahl lässt
Empfangsalter und Tageswechsel ohne neue Ereignisse unverändert. Der Reader lädt
vollständige Datenlisten und kann Legacy-Importe anstoßen. Speicher-/Threading-
Änderungen müssen mit dem anderen Agenten koordiniert werden.


## Tacho und Erscheinungsbild (2026-11-17)

Auf Nutzerwunsch wird die freigegebene helle/dunkle Tacho-Gestaltung als native
Android-Ansicht umgesetzt. Der Puls bekommt eine breite Instrumentenkarte mit
radialen Teilstrichen, Zeiger, großem unverändertem Messwert und Modus in der Mitte.
SpO₂ bleibt eine eigene Messwertkarte. Der Zeiger wird bei geänderten gültigen
Messwerten kurz animiert; ausgeschaltete Systemanimationen werden respektiert.
Fehlende Werte zeigen einen Strich und keinen Zeiger. Empfangsnotizen bleiben sichtbar.

Die Skala 40–180 bpm ist ausschließlich eine feste Anzeigeskala, keine physiologische
Norm und kein geschätzter Maximalpuls. Außerhalb liegende gültige Werte bleiben
numerisch erhalten; der Zeiger steht am Rand und ein Hinweis benennt die Überschreitung.
Der türkise Bogen visualisiert den Messwert, **keine gesunde oder sichere Zielzone**.
Es werden keine alters-/geschlechts-/gewichtsabhängigen Grenzwerte und kein neuer
wissenschaftlicher Gesundheitsalgorithmus eingeführt.

Ruhe, leichte Bewegung und Sport werden ausdrücklich **manuell** gewählt, separat
lokal gespeichert und durch erneutes Tippen wieder aufgehoben. Ohne Auswahl heißt
der Modus „unbekannt“. Puls oder kumulative Schritte werden nicht als automatische
Erkennung von Sitzen, Sport oder Schlaf ausgegeben. Klinische Trainingszonen und
persönliche Ruhepulsvergleiche bleiben eine gesondert zu validierende Erweiterung.

## Kompakter Tacho, Empfangsdetails und automatische Schritt-Heuristik (2026-11-17)

Der Tacho und die SpO₂-Kachel stehen nebeneinander; der Tacho nutzt einen kleineren
Kreisbogen mit durchgehendem Zeiger und einem kontinuierlichen Grün-Gelb-Orange-Rot-
Verlauf. Farbe kodiert ausschließlich die Position auf der festen Anzeigeskala 40–180 bpm,
keine gesundheitliche Risikozone. Modusschalter sind entfernt.

Der Modus wird aus den jüngsten kumulativen CIRQA-Schrittständen abgeleitet. Standard:
unter 10 Schritte/min = Ruhe, 10 bis einschließlich 100 = leichte Bewegung, über 100 =
Sport. Berechnet wird die mittlere Rate im jüngsten zusammenhängenden Fünf-Minuten-Fenster.
Das Dashboard benötigt mindestens zwei frische Werte über 30 Sekunden; Datenlücken,
Zählerreset, Tagesgrenze, Zukunftszeitstempel oder zu alte Daten ergeben „unbekannt“.
Fünf-Minuten-Fenster, Ruhe-/Sportgrenze und Mindestspanne sind keine klinisch validierten
Schwellen. Ein Schrittzähler beweist weder Sitzen noch Trainingsart. Die Parameter sind in
den lokalen Einstellungen veränderbar. Forschung und Grenzen: [SCIENTIFIC_REFERENCES.md](SCIENTIFIC_REFERENCES.md).

Jede Messkachel zeigt rechts oben einen kleinen Empfangsindikator. Grün ≤3 Minuten,
Gelb ≤15 Minuten, Orange ≤60 Minuten und Rot darüber beschreiben ausschließlich das Alter
des letzten Telefonempfangs; ohne Zeitstempel bleibt der Punkt grau. Der Indikator öffnet
Wert, Empfangszeit, Datenhinweis und Farblegende. Zeitstempel werden nicht mehr direkt in den
Kacheln gezeigt; sie sind Telefon-Empfangszeiten, keine bestätigten Sensor-Messzeiten.

Die Dashboard-Aktualisierung läuft spätestens alle 30 Sekunden, damit sich Empfangsalter und
Schrittfenster auch ohne neue Ereignisse ändern. Bei Schriftvergrößerung ab 1,3 werden Kacheln
gestapelt; bei üblichen Schriftgrößen bleiben sie auch auf schmalen Displays paarweise.

### Theme-Konfiguration

In Einstellungen → Darstellung gibt es Systemeinstellung (Standard), Hell und Dunkel.
Die Auswahl liegt in `app_appearance`, getrennt vom löschbaren persönlichen Profil.
`PulseLibreApplication` setzt AppCompat beim Prozessstart auf die gespeicherte Wahl;
Systemmodus folgt Android, die beiden expliziten Modi überschreiben ihn. Speichern
aktualisiert die Activities über AppCompat. Messdaten und Recorder werden nicht migriert.

## Lokaler Messverlauf (2026-11-17)

Die Hauptnavigation enthält nun Übersicht, Verlauf, Geräte und Export. Der Verlauf
liest nur Ereignisse `kind=reading` der CIRQA-Dienste 6 (Puls) und 19 (SpO₂) aus
der kanonischen SQLite-Datenbank. Die drei wählbaren Fenster sind rollierende
24 Stunden, 7 Tage und 30 Tage bis zum aktuellen Zeitpunkt. Empfangszeit bleibt
Telefon-Empfangszeit. Ungültige JSON-Einträge, Zeitstempel außerhalb des Fensters,
Pulswerte außerhalb 1–240 bpm und SpO₂ außerhalb 1–100 % werden nicht geplottet.

Jeder Messwert wird als einzelner Punkt gezeichnet. Es gibt weder Verbindungslinien,
Interpolation, Mittelwerte, Datenlückenauffüllung noch medizinische Schwellen. Feste
Achsen dienen nur der Lesbarkeit. Leere Zeiträume bleiben ausdrücklich leer. Die
Abfrage ist read-only und begrenzt; sie löst keinen Legacy-Import aus.

JVM-Tests decken Fensterlängen und Zeitkoordinaten ab. Android-Tests mit isolierter,
synthetischer SQLite-Datenbank prüfen Filterung, Sortierung, UI-Auswahl und leere
Zeitreihen. Activity-Test prüft den Verlauf-Reiter. Lauf und Ergebnisse werden nach
Abschluss der Branch-Prüfungen ergänzt.

Die Palette umfasst Übersicht, Gerätekarten, Navigation, Export und Profilüberschriften.
Systemdialoge und Eingaben verwenden das bestehende AppCompat-DayNight-Theme.
Theme- und Gauge-Modelle haben JVM-Tests; Android-Tests prüfen Light/Dark in beiden
Activities, Rückkehr zum Systemmodus, leere Gauge, manuelle Auswahl und Zeichnung in
beiden Paletten. Tatsächliche Build-/CI-Ergebnisse stehen im Übergabestatus bzw. GitHub.
