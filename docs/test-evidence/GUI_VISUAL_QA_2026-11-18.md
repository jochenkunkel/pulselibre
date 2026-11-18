# GUI-Qualitätstest 2026-11-18 – Übersicht, Verlauf, Config

Geprüft von Claude Code am echten Gerät. Reiner Test-/Review-Auftrag: **am Anwendungscode wurde dabei nichts geändert**; Korrekturen erfolgten danach in einem getrennten PR (siehe 7).

## 1. Getesteter Stand
- Commit `2db3131` (main), Build **2.0.0_build1038** (CI-Lauf 44320687911, schneller Lauf, signierte Debug-APK), installiert per `adb install -r` (Update, Daten erhalten).
- Gerät: **Samsung Galaxy S25 (SM-S931B)**, 1080×2340 px, 480 dpi (3 px = 1 dp, 360×780 dp), Android 17 (laut `ro.build.version.release`).
- Kein Emulator lokal verfügbar (SDK ohne Emulator-Paket); Tests ausschließlich am echten Gerät. Zusatzbedingungen: Schrift 1,3 bei voller Breite und Schrift 1,3 bei 900 px Breite (≈ 300 dp) per `settings put system font_scale` / `wm size`; **beides danach zurückgesetzt und geprüft** (Schrift 1,0, Originalauflösung).
- Messmethode: Screenshots (`screencap`), UI-Hierarchie (`uiautomator dump`, Bounds in px → dp), Reaktionszeit per Screenshot-Polling (Messfloor ≈ 0,9 s).

## 2. Tatsächlich ausgeführt
- Übersicht: Scroll von oben bis unten (3 Positionen), Hierarchie-Auswertung aller Kacheln (Bounds, Zielgrößen), Antippen der Kacheln Herzfrequenz und SpO₂ (Tagesdialog, Zurück), Info-Symbole (Profilkarte ⓘ, Empfangspunkte Herzfrequenz/SpO₂).
- Verlauf: Zeiträume 24 h/7 d/30 d umgeschaltet (5 Wechsel vermessen), schnelles Mehrfachtippen (5 Taps ohne Pause), Scroll bis zur Trendkarte, Info-Symbol Herzfrequenz, Trendkarte betrachtet.
- Config (Avatar oben rechts → Einstellungen): Scroll in 4 Positionen, ⓘ-Dialoge Aktivität/SpO₂/Profil, Rückweg per Schließen/Zurück. **Keine Werte geändert, nichts gespeichert, „Profil löschen“ nicht angetippt.**
- Navigation zwischen den drei Bereichen, Android-Zurück-Taste in Dialogen.
- Zwischenfall: Handy kurzzeitig anderweitig benutzt (Kamera im Vordergrund); danach nur noch Eingaben bei nachweislich fokussierter PulsLibre-App (Fokusprüfung vor jedem Tipp).

## 3. Messwerte (dp, aus UI-Hierarchie, Build 1038)
| Element | Messung |
|---|---|
| Seitenränder Übersicht/Verlauf | 16 dp, konsistent |
| Kachelbreite / Spaltenabstand | 160 dp / 8 dp |
| Kachelhöhen in einer Zeile | Herzfrequenz 194 vs. SpO₂ 168; Ruhepuls 120 vs. RR 128; Atemfrequenz 120 vs. Kalorien gesamt 80; Intensität moderat 80 vs. Kalorien aktiv 72 |
| Profilkarte ⓘ | 32×32 dp (Soll ≥ 48) |
| Config Eingabefelder | 45 dp hoch (Spinner 48) |
| Config Seiteninnenabstand/Spalten | 45 dp Rand, Feldspalten 8 dp Abstand, konsistent |
| Verlauf Zeitraum-Schalter | 103×48 dp je Taste |
| Verlauf Legendenchips | Text ≈ 11 dp hoch (≈ 9 sp) |
| Navigationsleiste | Labels 10 sp |
| Verlauf-Umschaltung | 0,33 s bis erste Änderung, ≈ 1,0 s bis stabil (5 Messungen: 0,99–1,08 s) |

Screenshots (15 Dateien) liegen **lokal** außerhalb des Repositories (`qa-screenshots-2026-11-18/` neben dem Projektordner), weil sie reale Profil- und Gesundheitsdaten zeigen (AGENTS.md: keine personenbezogenen Gesundheitsdaten committen).

## 4. Bewertung (1 = schlecht, 10 = sehr gut)
| Kategorie | Übersicht | Verlauf | Config |
|---|---|---|---|
| Layout und Ausrichtung | 6 | 6 | 8 |
| Abstände und Proportionen | 6 | 7 | 7 |
| Typografie | 6 | 6 | 7 |
| Visuelle Konsistenz | 6 | 6 | 6 |
| Bedienbarkeit | 7 | 6 | 7 |
| Professioneller Gesamteindruck | 6 | 6 | 7 |

Begründung kurz: Übersicht – klare Struktur und gute Lesbarkeit der Kernwerte, aber ungleiche Kachelhöhen, Umbruchstellen in Titeln, gemischte Kachelränder. Verlauf – schnell und stabil, aber SpO₂-Skala 0–100 macht Daten unlesbar, keine Datumsnavigation, Caption-Waisen. Config – am saubersten (Gruppen, Symbole, feste Schaltfläche), aber graue Standarddialoge und Mindesthöhen.

## 5. Gefundene Probleme
| Prio | Problem | Issue |
|---|---|---|
| P1 | Verlauf: feste Skala 0–100 %/0–240 bpm, SpO₂-Daten im obersten 5 % | #99 |
| P1 | 300 dp + Schrift 1,3: Profilwerte zeichenweise umbrochen, Navigation „Übersich/t“, Speichern abgeschnitten | #100 |
| P2 | Übersicht: ungleiche Kachelhöhen, Titelumbrüche (Intensität · / moderat) | #101 |
| P2 | Tagesdialog: Text vor Diagramm, keine Y-Werte, Punktwolke, Button abgeschnitten, graue Standarddialoge, „SCHLIESSEN“ | #102 |
| P2 | Nacht-Insights: Ausrichtung, Leerraum, 9-sp-Schrift | #103 |
| P2 | Touch-Ziele/Schrift: ⓘ 32 dp, Eingabefelder 45 dp, Legende 9 sp | #104 |
| P2 | Verlauf: Caption-Waise, ungleiche X-Ticks, keine Datumsnavigation (Konzept) | #105 |
| P3 | Roter Empfangspunkt gegen violette CI | #106 |
| – | Verlauf-Performance (gemessen ≈ 1 s) | #79 (Kommentar mit Messung) |

Kein P0 gefunden (kein Absturz, alle Bereiche bedienbar; 0 FATAL im Log während des Tests).

## 6. Positiv, ohne Beschönigung
Umschaltung Verlauf stabil; Info-Dialoge funktionieren; Konfiguration gut gruppiert; Nacht-Insights und Trendkarte laden zuverlässig; violetter Validierungsrand und Legende klar erkennbar; Schrift 1,3 bei voller Breite ohne Layoutbrüche.

## 7. Verbesserungen (Priorität)
1. Verlauf-Auto-Skala (#99) · 2. Kompaktlayout bei schmaler Breite/großer Schrift (#100) · 3. gleiche Kachelhöhen/Titel (#101) · 4. Tagesdialog neu ordnen (#102) · 5. Nacht-Insights (#103) · 6. Touch-Ziele (#104) · 7. X-Ticks/Datumsnavigation (#105) · 8. Eingabefeldhöhe 48 dp · 9. Empfangspunkt-Farbe (#106) · 10. Test auf kleineren Geräten (320 dp) und Schrift 1,5.

**Stand der Umsetzung:** Punkte 1, 2, 3 und Teile von 4–7 wurden direkt nach dem Test in PR #107 umgesetzt (CI/Emulator grün, voller Lauf 44323252688). **Diese Korrekturen wurden noch nicht am Gerät betrachtet** (Gerät war danach gesperrt) und sind deshalb nicht als verifiziert zu werten.

## 8. Nicht getestet
Geräte-/Daten-/Testlabor-Reiter; Hover/Maus; Langdruck-Aktionen; Tablet/Faltmodus; Drehung des Displays; Schrift 1,5 und größer; Dunkel-/Hellmodus-Umschaltung; TalkBack (nur Beschreibungstexte aus der Hierarchie gelesen); Emulator-Auflösungen; Verlauf mit mehr als 3 Tagen Daten (Gerät hat nur 3 Tage); Änderungen/Speichern in der Config (absichtlich unterlassen); die zweistufige Löschbestätigung am Gerät (nur per Emulator-Test `PersonalSettingsDialogTest`).

## 9. Fazit
PulsLibre wirkt strukturiert und funktional, aber noch nicht „App-Store-reif“: Datenaufbereitung (Skalen), Responsivität bei engen Breiten und die Konsistenz der Raster sind die größten Lücken. Nach Umsetzung der P1/P2-Punkte und erneuter Gerätekontrolle ist ein deutlich professionellerer Eindruck realistisch.
