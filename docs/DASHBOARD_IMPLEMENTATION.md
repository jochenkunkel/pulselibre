# Dashboard: Bestandsaufnahme und erster Konzeptschritt

Stand: 2026-11-17. Basis: `c02bbc1`; Arbeitsbranch `codex/dashboard-overview`.

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
- Lokaler Android-/JVM-Build bislang nicht ausgeführt: Gradle und Android SDK fehlen.
- Aktueller CI-Nachweis wird im Pull Request dokumentiert. PR-Schnellprüfung führt
  keine Instrumentierungstests aus; Full-CI und visuelle Geräteprüfung bleiben nötig.
- Keine Geräteaktionen, keine Installations- oder Produktionsfreigabe.

## Nächste Schritte

1. Karten im Emulator bei verschiedenen Breiten, Schriftgrößen und mit TalkBack prüfen.
2. Reader-Tests für Zeitzonen, Tageswechsel, ungültige Daten und Empfangsalter ergänzen.
3. Zeitreihenmodell mit expliziten Lücken und Herkunft entwickeln; vor wissenschaftlichen
   Aggregationen Quellen, Abdeckung, Fehlwertregeln und Einheiten prüfen.
4. Tag-/Woche-/Monat-Verlauf mit überprüften lokalen Zeitreihen umsetzen.

Noch offen: die bestehende Aktualisierung nur bei geänderter Ereignisanzahl lässt
Empfangsalter und Tageswechsel ohne neue Ereignisse unverändert. Der Reader lädt
vollständige Datenlisten und kann Legacy-Importe anstoßen. Speicher-/Threading-
Änderungen müssen mit dem anderen Agenten koordiniert werden.
