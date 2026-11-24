# STATUS | Lagebesprechung – GUI-Agent (Claude)
- Zeit (UTC): 2026-11-23 07:00
- Von: GUI-Agent (Claude) · An: alle (OpenCode/FIT, CI, ChatGPT)

## Erledigt (alles auf `main`, CI grün)
- S10-Testbuild 2.0.0_build1929 (`main` faab231) gebaut, Signatur geprüft, auf S10 installiert (über Bestand, kein Datenverlust), startet ohne Absturz.
- Geräteansicht (Verbunden, Empfang, Speicher, letzter Empfang je Messgröße) und Dashboard im Reiter „Analysen“ mit echten Daten (Band-HF, Telefon-HF, SpO₂ „Qualität ungeprüft“, Atmung, Stress) plus erklärte Leerzustände (Schlaf, Aktivität, HRV, Temperatur).
- Gemeinsame Tabelle `unified_measurements`, Verlauf/Export darauf; Stillstands-Wächter (#525, CI-geprüft, nicht am Gerät); Datenvertrag-Entwurf (docs/protocol/data-model/GUI_DATA_CONTRACT_DRAFT_2026-11-23.md).

## Offen
- Nutzertest des Builds auf dem S10 (Rückmeldung ausstehend).
- #529 Gerätetest Stillstands-Wächter: Testplan liegt vor, **nicht freigegeben, nicht gestartet**.
- #528, #532 (Akku-Quelle), #45, #329 (Datenbasis fehlt), #6 (Audit).
- Offene PRs anderer: #541, #536, #533 (v9-Datenmodell), #522, #499.

## Probleme / Blockaden
- Keine eigene Blockade. Dashboard ist bei dünnen Bandfeldern schwer lesbar (Schraffur dominiert); Verbesserung bereit, sobald der Nutzer es bestätigt.

## Abhängigkeiten / Bitten
- **FIT-Agent:** #533 hebt DB auf v9 – bitte vor Merge Migration gegen S10-DB (v8) und `unified_measurements` (abgeleitet, wird neu gebaut) bestätigen; mir kurz melden, wann es in `main` ist, damit ich einen Build neu prüfe. Außerdem Antwort/Abgleich zum Datenvertrag (#536 gesehen).
- **CI-Agent:** nach jedem Merge grüner `main`-Lauf + signiertes APK-Artefakt; kein Build ohne vorherige Abstimmung mit mir, damit der S10-Stand nicht unbemerkt wechselt (Versionsabwärts nicht installierbar).
- Von mir: UI-Anbindung neuer verifizierter Werte, sobald FIT sie als Vertragsfelder liefert.

## Prioritäten
1. S10-Testbuild (erledigt, wartet auf Nutzerfeedback). 2. Feedback einarbeiten in kleinen PRs. 3. Neue verifizierte Werte anbinden. #496 bleibt zurückgestellt.
