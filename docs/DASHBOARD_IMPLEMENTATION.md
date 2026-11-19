# Dashboard: Bestandsaufnahme und erster Konzeptschritt

**Historischer Bestands-Snapshot:** 2026-11-17, Basis `f17f977`, Arbeitsbranch `codex/dashboard-history`. **Spätere Entwicklung:** #41 (Aktivitätsatlas, Ruhepuls-Hero, getrennte Abdeckung) und #45 (Sensitivitätsanalyse) wurden anschließend auf `main` weiterentwickelt; den aktuellen Implementierungsstand stets am Code/CI prüfen. Neuer Hauptreiter „Analysen“ ist **nur geplant**: [#227](https://github.com/jochenkunkel/pulselibre/issues/227), [Sitemap](science/ANALYSEN_NAVIGATION_CONCEPT.md).

## Bestand

Die Übersicht liest reale, lokal vorhandene Ereignisse über `CirqaDashboardReader`
aus der kanonischen `cirqa_measurements.db`. `MainActivity` aktualisiert die Anzeige
bei geänderter Ereignisanzahl. Profilangaben sind separat in SharedPreferences.
Die ältere Beschreibung von sechs statischen Platzhalterkarten ist überholt.
Das reine `HealthSnapshot`-Modell besteht für Offline-Funktionen und Tests fort.

Vor diesem Schritt wurden Herzfrequenz, Ruhepuls, Schritte und Schrittziel in vier
primären Kacheln gezeigt; SpO₂ und Schlaf standen in der nachgeordneten Liste.
Das entspricht noch nicht der freigegebenen Hierarchie aus
[APPROVED_UI_CONCEPT_2026-11-15.md](ui/APPROVED_UI_CONCEPT_2026-11-15.md).

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
und [CIRQA_DASHBOARD_GROUND_RULES.md](protocol/data-model/CIRQA_DASHBOARD_GROUND_RULES.md).

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

Dichte Messreihen werden für die Darstellung nach Zeitfenstern zusammengefasst:
24-h-Ansicht in 1-h-Fenstern, 7-Tage-Ansicht in 6-h-Fenstern, 30-Tage-Ansicht in
Tagesfenstern. Je belegtem Fenster zeigt die Grafik den Median sowie Minimum und
Maximum der tatsächlich empfangenen Werte; leere Fenster bleiben leer. Es gibt keine
Verbindungslinien, Interpolation oder medizinische Schwellen. Die feineren vertikalen
Zeitstriche markieren die Fenster; Beschriftungen erscheinen in gröberen Abständen,
damit sie auf dem Telefon lesbar bleiben. Die Rohmesspunkte bleiben in der Datenbank
unverändert; die Abfrage ist read-only und löst keinen Legacy-Import aus.

JVM-Tests prüfen Auflösung, Aggregation, Median und beobachtete Spanne mit synthetischen
Fixtures. Android-Tests prüfen weiterhin Zeitreihenauswahl und leere Zustände.

## Tachometer-Geometrie und Referenzskalen (2026-11-17)

Der Puls-Tacho hat eine 200°-Öffnung (170° bis 370°). Die Nadel dreht um ihren
Drehpunkt auf der Sehne zwischen den beiden Bogenenden. Große Skalenstriche markieren
die Anzeigeskala 40–180 bpm; grün-gelb-orange-rot illustriert ausschließlich die
Skalenposition, nicht die Gesundheit oder eine Trainingszone.

Kleine Messkarten zeichnen Messwert und Skala als Balken. SpO₂ nutzt 0–100 %, mit einem
neutral markierten Kontextbereich 95–100 % aus einer NHS-Patienteninformation für
Finger-Pulsoximeter an Erwachsenen. Ruhepuls nutzt 60–100 bpm (AHA, Erwachsene); die
Atemfrequenz 12–20/min (MedlinePlus, Erwachsene in Ruhe). Die Quellen, Kurzbezeichnung
und Einschränkung stehen hinter dem Info-Punkt. CIRQA-SpO₂ am Handgelenk ist durch diese
Fremdquellen nicht validiert; Ruhe- und Atemfrequenzbereiche gelten nur im jeweiligen
Messkontext.

Die Balken ändern ihre Messwertfarbe nicht in eine medizinische Ampel. Die Referenzbereiche
werden neutral hinterlegt. Proprietäre Stress-/Body-Battery-Werte zeigen nur 0–100 der
Geräteskala. Schritte zeigen, falls vorhanden, den Anteil am geräteseitigen Tagesziel.
Einzelne RR-Intervalle bleiben als Millisekunden-Skala gekennzeichnet und sind keine HRV.
WHO-Intensitätsziele (150–300 moderate oder 75–150 intensive Minuten/Woche) sind als
Quellenhinweis verfügbar, werden aber wegen des unbestätigten CIRQA-Zeitraums nicht als
Fortschritt oder Wochenzielbalken interpretiert. Kalorien-/Schlafwerte ohne bestätigte
Einheiten bzw. Daten erhalten keine erfundene Normskala.

Quellen und Geltungsbereiche sind in [SCIENTIFIC_REFERENCES.md](SCIENTIFIC_REFERENCES.md)
festgehalten. Skalenprofile und Tacho-Geometrie sind durch JVM-Tests abgedeckt.

Die Palette umfasst Übersicht, Gerätekarten, Navigation, Export und Profilüberschriften.
Systemdialoge und Eingaben verwenden das bestehende AppCompat-DayNight-Theme.
Theme- und Gauge-Modelle haben JVM-Tests; Android-Tests prüfen Light/Dark in beiden
Activities, Rückkehr zum Systemmodus, leere Gauge, manuelle Auswahl und Zeichnung in
beiden Paletten. Tatsächliche Build-/CI-Ergebnisse stehen im Übergabestatus bzw. GitHub.


## Wissenschaftlicher Kachelbauplan (2026-11-17)

Der ausführliche, quellenbasierte Forschungs- und Implementierungsplan für Messwerte, abgeleitete Kennzahlen, Referenzbereiche, wissenschaftliche Grenzen, Kachelverträge und Tests liegt unter [SCIENTIFIC_DASHBOARD_TILE_BLUEPRINT.md](science/SCIENTIFIC_DASHBOARD_TILE_BLUEPRINT.md). Er ist ein **Plan**, keine Freigabe zusätzlicher Gesundheitsberechnungen oder CIRQA-Datenquellen. Für neue Kacheln gelten weiterhin die Evidenz- und KDoc-Regeln aus [ENGINEERING_SCIENTIFIC_STANDARDS.md](ENGINEERING_SCIENTIFIC_STANDARDS.md).

## Datenbank sichern und wiederherstellen

Der Export-Reiter heißt „Daten“. Neben CSV kann der Nutzer beide lokalen SQLite-Datenspeicher in ein versioniertes ZIP-Backup sichern: CIRQA-Ereignisse sowie Legacy-Messwerte und Sync-Cursor. Das Backup enthält keine SQLite-Schema-/App-Einstellungen, ist unverschlüsselt und wird ausschließlich über den Android-Dateiauswahldialog geschrieben. Die UI weist auf sensible Inhalte hin.

Vor Restore werden Dateieinträge, Größenlimits, Formatversion, SHA-256-Prüfsummen und Datensatzinhalte validiert. Erst nach expliziter Bestätigung ersetzt eine SQLite-Transaktion die Einträge in beiden Stores; bei Fehlern rollt SQLite die Änderung zurück. Reset verwendet dieselbe Transaktion und löscht nur Datenbankzeilen, nicht Profil- oder Darstellungseinstellungen. Bei laufender Nachtaufzeichnung muss der Nutzer deren Stopp bestätigen. Es werden keine BLE-/GFDI-/FileSync-Aktionen ausgelöst.

## Auflösung und Symbole im Verlauf (2026-11-17)

Die Verlaufskarten fassen echte Empfangssnapshots abhängig vom gewählten Zeitraum in Zeitfenstern zusammen: 15 Minuten bei 24 Stunden, 2 Stunden bei 7 Tagen und 6 Stunden bei 30 Tagen. Pro belegtem Fenster zeigt die Grafik beobachtetes Minimum, Median und Maximum sowie die Anzahl der Originalwerte. Leere Zeitfenster bleiben leer. Es gibt keine Interpolation, künstliche Messpunkte oder Verbindungslinien. Die Karten nennen Auflösung, belegte Fenster und ursprüngliche Punktzahl.

Das Dashboard versieht Messwerttitel mit kleinen thematischen Symbolen (Puls, Sauerstoff, Schlaf, Schritte und weitere Messwertgruppen). Die Symbole sind dekorative Orientierung und tragen keine Mess- oder Gesundheitsinformation. Android-UI-Tests sichern die Symbolpräsenz an Messkarten und die Verlaufstests die Zeitfensteraggregation ab. Die Gauge-Karte ist für ein kompakteres Kachelverhältnis neu angeordnet; Skalenbeschriftungen liegen innerhalb der Anzeige und Modus, Wert und Einheit stehen direkt unter dem Bogen.


## Issue #19: Messwertkacheln, Icons und Skalen (2026-11-17)

Die Prüfung gegen `docs/science/SCIENTIFIC_DASHBOARD_TILE_BLUEPRINT.md` ergibt drei Gruppen:

- **Direkt als Kachel darstellbar:** Herzfrequenz, kumulativer Tagesschrittstand und Geräte-Schrittziel; ihre Anzeige übernimmt gespeicherte Werte und Einheiten. Die zusätzlich sichtbare Kachel „Tagesziel Schritte“ berechnet `Schritte ÷ Geräte-Schrittziel × 100`. Das ist ausschließlich Produktfortschritt zum vom Band gelieferten Ziel, keine WHO-Empfehlung oder Gesundheitsbewertung. Fehlende/ungültige Eingaben ergeben „Keine Messdaten“; Werte über 100 % bleiben numerisch erhalten.
- **Nur mit Quellenhinweis und Einschränkung:** SpO₂, Ruhepuls und Atemfrequenz zeigen den Gerätewert samt neutraler, kontextgebundener Referenzmarke. Der NHS-Kontext für SpO₂ stammt vom Finger-Pulsoximeter und validiert den CIRQA-Handgelenksensor nicht. Eine Referenzmarke wird nicht als klinische Ampel eingefärbt.
- **Noch keine zusätzliche Gesundheitsauswertung:** Schrittfrequenz/Belastungstrends, Stress/Body Battery, RR→HRV, Aktivitätsminuten gegen WHO-Wochenziele, Schlafdauer/-stadien, Sitzzeit und Kalorieninterpretationen benötigen bestätigte Service-Semantik, ausreichend dichte Daten oder Historien-/Sensorzugriff. Die vorhandenen Roh-/Gerätewerte bleiben entsprechend gekennzeichnet; daraus wird kein weiterer Score abgeleitet.

Jede Messwertkachel erhält ein thematisches Icon. Skalenfüllungen verwenden die jeweilige Akzentfarbe; neutrale Referenzbereiche bleiben getrennt davon. Farbbänder mit medizinischer Bedeutung werden nur mit belastbarer, auf Messmethode und Kontext anwendbarer Evidenz ergänzt. Für die Schrittziel-Kachel testet die JVM-Suite gültige Prozentberechnung, Überschreitung, fehlende Eingaben und den nicht-klinischen Skalenvertrag.

## Hauptnavigation und Tagesstand

„Daten“ und „Testlabor“ hatten eigene Activities ohne Tab-Leiste. Die drei
Activities verwenden jetzt dieselbe Tab-Komponente. Die Leiste steht außerhalb
der Scrollfläche. Android zieht vorhandene Hauptseiten beim Tabwechsel nach vorne,
sodass beispielsweise das Exportformular erhalten bleibt. Android-Zurück folgt
der Reihenfolge der Activities. Die Übersicht speichert die Auswahl ihrer drei
internen Tabs bei Neuerstellung der Activity.

„Schritte heute“ zeigt in einer Kachel den kumulativen Tageszähler und das gültige Geräte-Schrittziel
des letzten Ereignisses. Der Prozentwert folgt
`100 × Tageszähler / Geräte-Schrittziel`, gerundet auf höchstens eine
Nachkommastelle. Der Balken endet bei 100 %, der Text zeigt auch höhere Werte.
Fehlende Schritte und fehlende Ziele bleiben getrennt erkennbar. Das
Empfangssymbol zeigt das Alter der gespeicherten Beobachtung.

## Messwertdetails und erste Qualitätsmerkmale

Die Kachelfläche öffnet eine Tagesansicht. Das Empfangssymbol bleibt ein eigener
Zugang zu Herkunft und Aktualität. Die Datumsauswahl liest gespeicherte
Echtzeitereignisse offline aus dem nach Dienst und Empfangszeit indizierten
SQLite-Fenster. Anfang und Ende sind zwei aufeinanderfolgende lokale
Mitternächte in der beim Öffnen geltenden Telefonzeitzone. An Tagen mit
Zeitumstellung beträgt die reale Achse 23 bzw. 25 Stunden. Die Wertauswahl
zeigt den UTC-Offset, um wiederholte Uhrzeiten zu unterscheiden.

Die Ansicht zeichnet Originalpunkte bis zum Abfragezeitpunkt. Pro Minute
bleiben Minimum, Maximum und letzter Punkt mit Zeitstempel erhalten.
Wiederholungen desselben Werts zum selben Zeitpunkt zählen einmal.
Die Detailansicht nennt eindeutige gültige Werte, Duplikate und verworfene
Feldwerte getrennt. Für die Empfangsabdeckung zählt jede Minute mit mindestens
einem gültigen Wert einmal. Der Nenner umfasst alle Minuten des gewählten Tags
bis zur laufenden Minute oder bis zum Tagesende, je nachdem, was früher liegt.
Die laufende Minute zählt als angefangen. Die 23- und 25-Stunden-Tage bei
Zeitumstellung enthalten 1380 bzw. 1500 Minuten. Die Anzeige beschreibt die
Verteilung empfangener Werte. Kontinuierliche Messung und Sensorqualität
lassen sich daraus nicht ableiten. Für null oder einen gültigen Wert erscheint ein Leerzustand
für den Verlauf, bei einem Wert ergänzt um den beobachteten Einzelwert.
Die Schrittansicht zeigt kumulative Zählerstände mit dem gültigen Tagesziel der letzten Beobachtung als Referenz. Proprietäre Indizes sind als Geräteskalen beschriftet.
Schlaf benötigt validierte Historie. Nachtgrenzen, Signalqualitätsflags und
wissenschaftliche Trendberechnungen sind weiterhin offen.


## Verbindungsanzeige (#63)

Kopfbereich und Gerätekarte werden im Vordergrund aus derselben Android-GATT-Abfrage
aktualisiert. Die Gerätekarte und TalkBack unterscheiden Aufbau, Trennung,
fehlende Berechtigung und unbekannten Status. Grün steht für eine von Android
gemeldete GATT-Verbindung. Android meldet dabei keine besitzende App. Der Recorder
zeigt separat den Dienststatus, der während eines Verbindungsabbruchs aktiv bleiben
kann. Die lokale Diagnose hält bis zu 32 beobachtete Statuswechsel fest.
Die Ursache der Verbindungsabbrüche ist am Gerät zu klären.

## Tagesverläufe und Grundlage für Trendanalysen

- Die Zeitachse folgt dem lokalen Kalendertag. Die Zeitmarken 06:00, 12:00
  und 18:00 entsprechen auch an Tagen mit 23 oder 25 Stunden der lokalen
  Uhrzeit. In der Punktauswahl kennzeichnet der UTC-Offset die wiederholte
  Stunde beim Wechsel zur Winterzeit.
- Tippen und horizontales Ziehen wählen einen gespeicherten Punkt. Die
  Schaltflächen für den vorherigen und nächsten Punkt bieten denselben
  Zugriff ohne Berührung der Grafik. Vertikale Bewegungen scrollen den Dialog.
- Für spätere Analysen nach #40 und #41 dient die lokale SQLite-Zeitreihe
  als Ausgangspunkt. Die Anzeige reduziert dichte Minuten auf Minimum,
  Maximum und letzten Empfang. Analysen müssen die geeigneten zeitlich
  zugeordneten Originalwerte lesen: Die Auswahl im Diagramm übergewichtet
  Extremwerte und eignet sich deshalb beispielsweise nicht als Stichprobe
  für einen Mittelwert.
- Für einen Ruhepulstrend ist das Gerätefeld erforderlich. Vor einem
  Schritttrend sind Rücksprünge des Tageszählers und die Abdeckung um den
  Tageswechsel zu prüfen. Empfangsminuten dokumentieren Telefonempfang.
  Sie liefern keinen Nachweis über die Tragedauer.



## Navigation wissenschaftlicher Auswertungen – Planung #227 (19.11.2026)

Die ausführlichen wissenschaftlichen Auswertungen werden künftig im **geplanten** Hauptreiter **„Analysen“** gruppiert; die **„Übersicht“** bleibt auf aktuelle Werte und kompakte Teaser begrenzt. **Kanonisch:** [Analysen-Sitemap und Modulvertrag](science/ANALYSEN_NAVIGATION_CONCEPT.md) sowie [Issue #227](https://github.com/jochenkunkel/pulselibre/issues/227). Die hier dokumentierten Messquellen-, Qualitäts- und Methodengrenzen bleiben verbindlich. **Keine Android-Navigation durch diese Dokumentationsänderung implementiert.**
