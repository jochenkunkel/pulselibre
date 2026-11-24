# START | Priorität 1: testbarer S10-Build innerhalb von 48 Stunden
- Zeit (UTC): 2026-11-23 01:30
- Von: GUI-Agent (Claude) · An: FIT-Agent (OpenCode), CI-Agent, ChatGPT
- Auftrag (Nutzer): S10-Testbuild mit aktueller Geräteansicht, wissenschaftlichem Dashboard und allen bereits implementierten Funktionen; Test mit produktiven Daten auf S10 + Armband; fehlende Messgrößen als verständliche Leerzustände; **keine zusätzlichen Forschungsaufgaben**.
- Abnahmekriterien: (1) APK installierbar, (2) App startet auf dem S10, (3) Geräteansicht funktioniert, (4) Dashboard sichtbar, (5) CI grün.

## Aufteilung
| Wer | Aufgabe |
|---|---|
| GUI (ich) | Dashboard sichtbar im Reiter „Analysen“ mit echten Daten (PR #537), Geräteansicht (bereits auf `main`), Leerzustände, Abnahme auf dem S10 |
| FIT-Agent | bereits verifizierte Messwerte bereitstellen, offene PRs (#522, #499) integrieren/Konflikte lösen; **bitte Datenvertrag-Entwurf kurz beantworten** (`docs/protocol/data-model/GUI_DATA_CONTRACT_DRAFT_2026-11-23.md`), aber nur soweit für diesen Build nötig |
| CI-Agent | grüner Lauf auf `main`, signierter APK-Artefakt (`pulselibre-signed-release-update-apk`), Gate-Prüfung |

## Stand `main` (für den Build)
Auf `main`: Geräteübersicht zuerst (#521), Live-Gerätemetadaten (#520), gemeinsame Tabelle `unified_measurements` und Verlauf/Export darauf (#514/#519), Stillstands-Wächter (#525), Modell/Analyse/Fixtures (#534), synthetische Vorschau im Testlabor (#535). Offen für den Build: Dashboard mit echten Daten (#537).

## Regeln für den Build
- Qualitätsregeln gelten: HF 0, negative Atemfrequenz-/Stress-Codes sind ungültig bzw. Gerätestatus, SpO₂ bleibt „Qualität ungeprüft“, keine Interpolation, Band- und Telefonzeit getrennt.
- Nicht verfügbare Messgrößen (Schlaf, Aktivitätsverlauf, HRV, Temperatur, Akku) zeigen erklärte Leerzustände.
- **DB-Version 8 ist auf `main`**: Wer ältere Builds auf dem S10 hat, installiert über den neuen (nicht umgekehrt).
- Gerätezugriff: S10 ist zum Testen freigegeben (Nutzer). Wer es nutzt, meldet im Chat START/ENDE; ich installiere den fertigen Build und prüfe die Abnahmekriterien.

## Nächste Schritte (GUI)
1. #537 nach grüner CI mergen. 2. Signierten Build aus `main` ziehen, Signatur prüfen, auf dem S10 installieren. 3. Kriterien 2–4 am Gerät prüfen und im Chat melden.
