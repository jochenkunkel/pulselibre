# PulseLibre – Anforderungen, Motivation und Umsetzung

> **Temporäre Entwicklungsregel (21.11.2026):** Rohdaten des aktuellen CIRQA-Testarmbands dürfen agentenintern verarbeitet werden; vor öffentlicher Weitergabe Identifizierbarkeit prüfen. Diese Sonderfreigabe gilt nicht automatisch für spätere personenbezogene Daten. Siehe [Forschungsdatenregel](collaboration/CURRENT_BAND_RAW_DATA_POLICY.md).

Stand: 2026-11-15 · Lebendes Projektdokument · Priorität: nachvollziehbare Fakten statt scheinbarer Genauigkeit.

## 1. Warum wir PulseLibre entwickeln

PulseLibre soll ein datensparsamer, lokal funktionierender Gesundheits- und Aktivitätsbegleiter für Android werden. Ausgangspunkt ist der Wunsch, Daten des Garmin-CIRQA-Armbands möglichst direkt und ohne verpflichtende Hersteller-Cloud auszuwerten. Das Projekt entstand im Kontext einer persönlichen Verletzungs- und Genesungserfahrung; das stilisierte Schulter-/Oberarmmotiv erinnert an diesen Ursprung, ohne persönliche Röntgenoriginale zu veröffentlichen.

**Grundsätze:** Selbstbestimmung über Gesundheitsdaten; Offline-first; Transparenz; nachvollziehbare Wissenschaft; keine erfundenen Messwerte, Diagnosen oder Therapieempfehlungen; Sicherheit vor Funktionsumfang.

## 2. Was die App leisten soll (Zielbild)

### 2.1 Startseite / Übersicht
- Ruhiges violett-weißes Design, angelehnt an Mein Weg 2; große, gut lesbare Karten und klare Statusanzeigen.
- Karten für Herzfrequenz, Aktivität/Schritte, Schlaf und Sauerstoffsättigung (SpO₂) inklusive zeitgestempelter Verlaufskurve.
- Sichtbarer Offline-, Geräte-, Datenherkunfts- und Verbindungsstatus.
- Deutliche Unterscheidung zwischen **echten Messdaten**, **Simulation/Demo** und **fehlenden Daten**. Keine scheinbar echten Demo-Messwerte.
- Navigationsbereiche: Übersicht, Geräte, Verlauf, Einstellungen; zusätzlich Ground Rules sowie Ernährung & Energie.

### 2.2 Persönliches Profil / Einstellungen
- Optional und lokal: Alter/Geburtsdatum, Körpergröße, Körpergewicht; Geschlecht nur, wenn eine konkret implementierte, belegte Formel dies tatsächlich benötigt; weitere Angaben nur bei klarer fachlicher Notwendigkeit.
- Einheiten, Datenschutzoptionen, Datenquellen und individuelle Ziele; keine Pflicht zur Angabe unnötiger sensibler Daten.
- Sonderfälle (z. B. Schwangerschaft, relevante Erkrankungen, Rehabilitation) dürfen nicht automatisch mit allgemeinen Bevölkerungsreferenzen bewertet werden.
- Keine ungesicherte Aussage „in deinem Alter musst du Wert X erreichen“.

### 2.3 Ground Rules – wissenschaftliche Orientierung
- Referenzen primär WHO, DGE/D-A-CH und geeignete deutsche Fachorganisationen; bei Messgeräten zusätzlich seriöse Regulierungs- und Validierungsquellen.
- Jede Regel mit **Originalquelle/URL, Publikationsstand, Zielgruppe, Eingaben, mathematischer Formel, Einheit, Unsicherheit, Grenzen und Testfällen** dokumentieren.
- BMI für Erwachsene: Gewicht(kg)/Größe(m)², WHO-Kategorien als Populationsorientierung, keine Diagnose.
- WHO-Aktivitätsempfehlungen für Erwachsene: 150–300 min moderate oder 75–150 min intensive Aktivität/Woche oder äquivalente Kombination; Krafttraining an mindestens 2 Tagen. Bei Älteren zusätzlich funktionelles Gleichgewichts-/Krafttraining. Schritte sind kein verlässlicher Ersatz für Intensitätsminuten.
- **Kein angebliches WHO-10.000-Schritte-Ziel** und keine erfundenen altersabhängigen Herzfrequenz- oder SpO₂-Grenzen.
- SpO₂-Trend darstellen, Messbedingungen und Grenzen der Handgelenkmessung erklären; keine automatische Diagnose oder Alarmgrenze ohne gerätespezifische Validierung.

### 2.4 Ernährung & Energie
- Übersicht über geschätzten Energiebedarf, dokumentierte Nahrungsenergie, Makronährstofforientierung und protokollierte Getränke.
- Energieverbrauch: Grundumsatzmodelle (z. B. Mifflin–St Jeor), PAL und gegebenenfalls Bewegungs-/Herzfrequenzdaten **nur als explizit gekennzeichnete Schätzungen mit validierter Quelle und geeigneten Eingaben**. Herzfrequenz allein ist keine exakte Kalorienmessung.
- Kalorienaufnahme nur aus tatsächlich erfassten Nahrungsmitteln und deren belegten Nährwertangaben; keine Rückrechnung aus Armbanddaten.
- DGE-Proteinorientierung für passende gesunde, normalgewichtige Erwachsene: 0,8 g/kg/Tag (19–64), ab 65 Jahren 1,0 g/kg/Tag als Schätzwert; bei abweichenden Voraussetzungen keine automatische Empfehlung.
- DGE-Getränkeorientierung rund 1,5 l/Tag als allgemeiner Richtwert, **keine individuell berechnete Sollmenge**. Flüssigkeitsbedarf hängt u. a. von Belastung, Umgebung und medizinischen Umständen ab.
- Lebensmittelgruppen, Ballaststoffe und andere Referenzen erst nach belegter Quellen- und Zielgruppenprüfung implementieren.
- Einheiten und Berechnungen transparent; „nicht verfügbar“ statt einer nicht begründbaren Zahl.

### 2.5 Garmin CIRQA / Bluetooth
- Android BLE-Scanning, Geräteauswahl, Verbindung, Dienst-/Characteristic-Inspektion und Fehlerszenarien in sauber getrennten Komponenten.
- Proprietäre Garmin-Protokolle sind **noch nicht entschlüsselt**; keine Zusage, dass Gesundheitsdaten schon gelesen werden können.
- Bestehende Garmin-Connect-Kopplung und vorhandene Daten nicht gefährden: kein Reset, kein Entkoppeln, kein aggressives Pairing ohne spätere ausdrückliche Freigabe.
- Simulationen ohne Hardware; Fehlerzustände (Timeout, Berechtigungen, Verbindungsabbruch, ungültige Pakete, Wiederholung) gezielt testen.

## 3. Wie wir entwickeln

1. **Offline-first und Privacy-by-design:** Keine Internetberechtigung, keine Cloud-Abhängigkeit, lokale Speicherung mit Datenminimierung; vor Einführung jeder neuen Datenquelle Datenschutz prüfen.
2. **Trennbare Architektur:** BLE-Transport, Zustandsmaschine, Protokollparser, lokale Datenhaltung, Referenzregeln und UI getrennt halten.
3. **Mathematische Nachvollziehbarkeit:** Nur quellengeprüfte Regeln mit Versionsangabe, Gültigkeitsbereich und deterministischen Unit-Tests; keine Diagnose.
4. **Emulator-first:** JVM-Tests, Lint, APK-Build und Android-Instrumentierung/Emulator in GitHub Actions. Kein Smartphone-Test vor ausdrücklicher Entscheidung des Nutzers.
5. **Saubere Git-Historie:** Jede relevante Anforderung, Designentscheidung, Quelle, Einschränkung, Änderung und Test im privaten GitHub-Repository versionieren.
6. **Klare Statusmeldungen:** Implementiert ≠ getestet ≠ medizinisch validiert ≠ auf CIRQA-Hardware verifiziert. Den jeweiligen Stand ehrlich kennzeichnen.

## 4. Aktueller Stand / Nicht implementiert

**Vorhanden:** BLE-Explorer-Prototyp, Statusmodell und erste Fehlersimulation, Android-Launcher-Icons, CI mit Emulator-Tests, erste WHO-/DGE-Berechnungsfunktionen und Unit-Tests, dokumentiertes App-Zielbild.

**Offen:** Echtes SpO₂-Diagramm in der App, Profileingabe und persistente lokale Datenhaltung, Ernährungs- und Trinkprotokoll, validierte Kalorienverbrauchsschätzung, Garmin-Protokolldecodierung, echte Gesundheitsdaten und vollständige UI-Umsetzung. Die zuletzt hinzugefügten Funktionen sind erst nach grünem CI-Lauf als buildgeprüft anzusehen.

## 5. Quellen und vertiefende Spezifikationen

- [App-Zielbild](APP_VISION.md)
- [Ground Rules und Quellen](GROUND_RULES.md)
- [Ernährung & Energie](NUTRITION_RULES.md)
- [Testplan](TESTPLAN.md)
- [Qualitätskriterien](QUALITY_GATE.md)
- [Projektgeschichte](PROJECT_HISTORY.md)
- [Branding](BRANDING.md)

**Grundsatz:** Wenn für eine Kennzahl keine belastbare, für die Person und das Gerät passende Grundlage existiert, zeigen wir keine automatische Empfehlung.
