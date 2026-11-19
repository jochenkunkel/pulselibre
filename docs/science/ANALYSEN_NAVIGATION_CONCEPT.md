# PulsLibre – Hauptreiter „Analysen“: Informationsarchitektur und wissenschaftlicher Navigationsvertrag

**Status:** freigegebene Planungsrichtung (2026-11-19); **nicht im Android-Quellcode implementiert**. Zentrales Ticket: [#227](https://github.com/jochenkunkel/pulselibre/issues/227).

## Zielbild
Die **Übersicht** zeigt aktuelle Geräte-/Messdaten und wenige Hinweise. **Analysen** ist der einzige Haupt-Navigationsort für umfangreiche wissenschaftliche und explorative Auswertungen. Kurzlinks in der Übersicht führen zu denselben Analysen-Routen, niemals zu duplizierten Berechnungen.

## Sitemap

```text
Bottom Navigation
├── Übersicht (aktuelle Werte, Gerätestatus, kurze Teaser)
├── Analysen (NEU, Planung #227)
│   ├── Herz & Kreislauf
│   │   ├── Ruhepuls / Fitnessentwicklung (#41)
│   │   ├── HRV Intelligence (Forschung; nur verifizierte RR)
│   │   ├── Heart Rate Recovery (Forschung; belastbarer Aktivitätskontext)
│   │   └── Oxygen Stability (Forschung; SpO₂-Qualitätsgate)
│   ├── Schlaf & Erholung
│   │   ├── Night Flow / Regeneration (#43)
│   │   ├── Bewegung → nächste Nacht (#44)
│   │   └── Recovery Lag (Forschung)
│   ├── Bewegung & Entwicklung
│   │   ├── Matched Effort / Fitnessfortschritt (#46)
│   │   ├── 90-Day Horizon (#47)
│   │   └── Activity Signature (Forschung)
│   ├── Zusammenhänge & Muster
│   │   ├── Insight Constellation / Korrelationen (#45)
│   │   ├── Circadian Rhythm Lab (Forschung)
│   │   ├── Change Point Explorer (Forschung)
│   │   └── Physiological Pattern Explorer (Forschung)
│   └── Datenqualität & Methoden
│       ├── Data Confidence Observatory (Forschung)
│       ├── Personal Baseline Engine (Forschung, gemeinsame Infrastruktur)
│       ├── Datenherkunft / Historienabdeckung (#144, #148)
│       └── wissenschaftliche Methodik und Grenzen
└── Bestehende weitere Hauptreiter bleiben bis zur UI-Entscheidung unverändert
```

**#219:** Verlauf-Heatmap bleibt primär im historischen Verlauf und kann aus Analysen verlinkt werden, ohne als eigenständige Gesundheitsanalyse umetikettiert zu werden.

## Karten- und Routenschema (fachliches Konzept, kein Kotlin-API-Vertrag)

| Feld | Bedeutung |
|---|---|
| `moduleId` | stabile ID, unabhängig vom angezeigten Titel |
| `category` | eine der fünf Gruppen |
| `availability` | `AVAILABLE`, `INSUFFICIENT_DATA`, `UNVERIFIED_SOURCE`, `NOT_IMPLEMENTED` |
| `dataRequirements` | konkrete Messquellen, Mindestabdeckung, Messzeitsemantik |
| `summary` | maximal 1–2 verständliche Sätze ohne Diagnosen |
| `methodRoute` | Erklärung zu Datenqualität, Methode, Unsicherheit |
| `deepLink` | eindeutiges Ziel für Teaser und Navigation |

**Ablauf:** Analysen-Start → Gruppe → Modul → Zeitraum/Detail → Datenqualität & Methode. Ein fehlender Datenvertrag führt zu einem erklärten Leerzustand, nicht zu einem ausgegrauten Fantasie-Score. Gruppen und Modulstatus sind unabhängig von aktuellen BLE-Verbindungen navigierbar.

## Visuelle Richtung
Bestehende PulsLibre-Dark-/Light-Tokens, Navy-Flächen, violette Gruppenkarten, Pink für Herzkurven, Türkis für neutrale Datenqualität. **Keine medizinischen Ampeln**. Bestehende SVGs aus `design/icons/fitness-v1/` und `design/icons/research-v1/` wiederverwenden. Die großen Designreferenzen stehen unter `design/mockups/research-v1/`. Alle dynamischen Kurven/Heatmaps aus echten verifizierten Daten rendern; Mockups enthalten ausschließlich synthetische Illustrationswerte.

## Wissenschaftliche Prüfregeln
- Ruhepuls nicht aus Tagesminimum ableiten; HRV nur aus validierten RR-Intervallen.
- SpO₂ keine Diagnose; Schritte kein VO₂max/Fitnessalter; Korrelation ≠ Kausalität.
- Jeder Wert: Einheit, Zeitraum, gültige n/Tage, Datenabdeckung, Quellenstatus und Methode.
- Keine 90-Tage-Trends bei fehlender 90-Tage-Historie behaupten.
- Mehrfachtests, Autokorrelation und Confounder bei explorativen Analysen berücksichtigen.
- Datenqualität als Voraussetzung jeder Auswertung, nicht als nachträgliche Dekoration.

## Implementierungsreihenfolge – später durch Codex
1. Bestandsnavigation auf `main` auditieren und IA/Deep-Link-ADR schreiben.
2. Leere Analysen-Startseite mit fünf Gruppen und echten Availability-States; Emulator-Screenshot und TalkBack.
3. Bereits vorhandene/validierte Analysen aus #41/#43/#44/#45/#46/#47 sukzessive einhängen, ohne Datenmodell zu duplizieren.
4. Forschungs-Ideen erst nach eigenem Datenverfügbarkeits-/Methoden-Ticket aufnehmen.
5. Tests: 320 dp, 200 % Schrift, Dark/Light, leere Datenbank, Rücknavigation, Offlinezustand, Deep Links, kein erfundener Messwert.

## Dokumentationsabgleich
Planungsentscheidung in `docs/DASHBOARD_IMPLEMENTATION.md`, `docs/science/SCIENTIFIC_DASHBOARD_TILE_BLUEPRINT.md` und `docs/protocol/data-model/CIRQA_DASHBOARD_GROUND_RULES.md` nachgetragen; verlinkte Issues #41 #43 #44 #45 #46 #47 #219 #144 #148 erhalten Hinweise auf #227. **Dieser Commit ändert nur Dokumentation.**
