# UEBERGABE | Koordination und Integration liegt ab jetzt beim OpenCode-FIT-Agenten
- Zeit (UTC): 2026-11-23 07:30
- Von: GUI-Agent (Claude) · An: OpenCode/FIT-Agent (Gesamtkoordination), CI-Agent
- Auftrag (Nutzer): FIT-Agent koordiniert und integriert; GUI liefert fertige Arbeiten als getestete, reviewfähige PRs, stimmt den Datenvertrag Dashboard/Geräteansicht direkt mit ihm ab und hilft bei Integrationskonflikten/GUI-Fehlern. Ziel: stabile, auf dem S10 testbare Oberfläche mit ausschließlich verifizierten Gesundheitsdaten.

## Fertige GUI-Arbeiten (alle auf `main`, CI grün)
Dashboard (#537), synthetische Vorschau nur im Testlabor (#535), Wissenschaftsmodell (#534), Geräteansicht zuerst (#521), unified_measurements + Verlauf/Export (#514/#519), Stillstands-Wächter (#525). Build 1929 ist auf dem S10.

## Offen von mir
- PR #542 (nur Doku, Status) – CI läuft.
- Datenvertrag: Entwurf `docs/protocol/data-model/GUI_DATA_CONTRACT_DRAFT_2026-11-23.md`; bitte verbindliche Antwort/Änderungen (Felder, Qualitätsflags, Herkunft, Zeitbasis). Ich übernehme deine Antwort (#536) in die GUI.
- Verifizierte Werte: Das Dashboard zeigt SpO₂ weiter als „Qualität ungeprüft“; sobald du Werte als verifiziert freigibst, schalte ich sie um. Bitte pro Messgröße nennen, was verifiziert ist.
- #529 Gerätetest Wächter: nicht freigegeben, nicht gestartet.

## Regeln, die ich weiter einhalte
Kein Ändern fremder Komponenten (FIT/DB-Migration), Band-/Telefonzeit getrennt, keine Interpolation, keine unverifizierten Werte als Gesundheitsdaten. Meldet GUI-Fehler oder Integrationskonflikte hier; ich antworte mit kleinem PR.
