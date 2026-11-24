# Wissenschaftliches Dashboard (#45) und Schlaf/Aktivität (#329): Darstellungsregeln und Freigabe

Stand 2026-11-22 · GUI-Agent · Grundlage: Arbeitsauftrag GUI-/Wissenschafts-Agent. Dieses Dokument legt **Regeln fest, keine Auswertungen**. Datenabhängige Funktionen bleiben gesperrt, bis die jeweilige Freigabebedingung unten erfüllt ist.

## 1. Qualitätszustände (jede Anzeige trägt genau einen)
| Zustand | Bedeutung | Darstellung |
|---|---|---|
| gültig | dokumentierter Messwert | normal, in Min/Mittel/Max |
| fehlend | kein Wert im Fenster | Lücke, **keine Interpolation** |
| ungültig | Statuscode oder Sentinel, kein Messwert | zählt als „ungültig“, **nie** in Diagramm oder Kennzahl |
| eingeschränkt | Wert mit Qualitäts-/Konfidenzinformation unterhalb „voll“ | markiert, Bedeutung nur wenn vom Datenmodell geliefert |
| Gerätestatus | z. B. off wrist, große Bewegung | als Status, nicht als Wert |
| nicht unterstützt | Feld unbekannt (z. B. FIT-Nachricht 233) | **nicht** als Gesundheitsmetrik, nur Hinweis „nicht entschlüsselt“ |

## 2. Metrikregeln (im Code umgesetzt: `UnifiedMeasurements.VALID_SQL`, `DashboardMeasurement`)
- **Herzfrequenz:** 0 oder kleiner ist ungültig; gültig 1–240 bpm.
- **Atemfrequenz:** -100 (off wrist), -200 (große Bewegung), -300 (ungültig) sind Gerätestatus, keine Messung; keine negativen Werte in Diagrammen.
- **Stress:** negative Rohwerte sind ungeklärt und werden nicht als Stresswert gezeigt.
- **SpO₂:** ein numerisch gültiger Wert ist **nicht** automatisch qualitätsgesichert. Messmodus und Konfidenz werden nur angezeigt, wenn das Datenmodell sie liefert. **Keine eigenen Grenzwerte.**
- **FIT-Kandidaten (#500):** liegen getrennt (`fit_review_candidates`) und gelten als nicht vertrauenswürdig; sie fließen nicht in Kennzahlen ein. Die **Persistenz** dieser Werte ist nach der P0-Entscheidung vom 24.11.2026 verbindlich (`docs/protocol/DATA_ACQUISITION_PRIORITY.md`); die getrennte, als ungeprüft markierte Darstellung ist eine GUI-Aufgabe, kein Speicherverbot.

## 3. Pflichtangaben jeder Auswertung
Datenabdeckung (Anteil des Fensters mit gültigen Werten), Messlücken (nicht gefüllt), Stichprobengröße (n), Messintervall je Quelle (Band und Telefon haben **verschiedene Zeitbasen und Intervalle**, nie vermischen), Unsicherheit (Streuung statt Einzelzahl). Ohne diese Angaben wird keine Kennzahl gezeigt.

## 4. Freigabebedingungen (gesperrt bis erfüllt)
| Funktion | Voraussetzung (Besitzer) |
|---|---|
| Schlafband (#329/#45) | verifizierte Schlafintervalle und Zeitsemantik im Datenmodell (FIT-Agent, #43/#44/#46) |
| Aktivitätsreihen | belegte Zeitbasis der Aktivitätsfelder (FIT-Agent) |
| Korrelationen | methodische Beschreibung (Test, Mindest-n, Behandlung von Lücken) von ChatGPT/Datenmodell-Agent abgenommen |
| Akku/Ladezustand (#159) | Datenquelle belegt (#142); bis dahin „nicht verfügbar“ |

## 5. Definition of Done je Funktion
Datenquelle nachgewiesen, Qualitätsregeln angewandt, verständliche Darstellung, TalkBack/Kontrast geprüft, Tests grün, Dokumentation und Ticket aktualisiert.
