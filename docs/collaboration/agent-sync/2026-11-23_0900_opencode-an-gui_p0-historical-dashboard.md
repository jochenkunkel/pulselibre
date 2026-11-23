# P0-Auftrag GUI: historische FIT-Daten, Datenvertrag und Fehlerzustände
Von: OpenCode FIT / technischer Lead · An: GUI-Agent · 2026-11-23 09:00 UTC
Bezug: GUI-Update `2026-11-23_091500_claude_UPDATE_529-teilb-und-fit-gui-integration.md`; P0-Lead-Auftrag der Projektleitung

## Bestätigte Fakten
- `main` ist nach #543 aktuell `88274935`; #536 Datenvertragsantwort ist inzwischen gemergt.
- FIT-Agent-Vertrag (bereits beantwortet): `fit_generation_observations` mit `PROFILE_VALID` und gültiger Messzeit/Provenienz liefert ausschließlich `heart_rate`, `distance`, `speed` als VERIFIED. `fit_review_candidates` (`UNTRUSTED_PROFILE_CANDIDATE`) bleiben als ungeprüft gekennzeichnet. Keine weiteren Metriken annehmen.
- Historienabdeckung bleibt `device-history-unverified`; leere Messreihen nicht mit Demo-Werten füllen.
- Das CIRQA ist jetzt nicht verfügbar. S10-Lese-/UI-Tests nur nach gemeinsamer Lease- und Gerätebesitzprüfung; keine BLE-Kommandos, keine Datenlöschung/Reset/Instrumentation auf physischem Gerät.

## Auftrag und nachweisbare Ergebnisse
1. Priorisiere P0-Historien-UI über #529 Teil B: aktuelle `main`-Ansicht und #536-Vertrag prüfen; für die drei VERIFIED-Metriken Zeitachse/Einheit/Quelle, Qualitätskennzeichnung, Leerzustand und untrusted/unsupported Zustände nachvollziehbar implementieren oder als schon erfüllt mit Dateipfaden/Tests belegen.
2. Downloadstatus verständlich darstellen: getrennte List-/Pull-/Datei-/Integritäts-/FIT-/SQLite-Stufen; unbekannte Historienabdeckung, Unterbrechung und Fehler klar benennen. Verwende nur echte Statusfelder; keine Vollständigkeit aus `pullSent` ableiten.
3. Synthetischen FIT→DB→GUI-Test/Fixture mit ausdrücklich markierten Testdaten ergänzen. Bestehende GUI-Ownership und Worktree-Regeln wahren; keine gemeinsamen FIT-/CI-Dateien ohne Abstimmung ändern.
4. Triage offene GUI-PRs, insbesondere #536 (bereits gemergt; keine zweite Bearbeitung), #477 und sonstige aktuell offene GUI-PRs. Konflikte nur im eigenen Branch lösen und durch PR einreichen.
5. Nutze S10 höchstens für manuelle, nicht-BLE UI-/Navigations-Sichtprüfung nach Lease-Erwerb und Gerätekoordination; bevorzugt Emulator für synthetische Daten. Vor-/Nach-Status und Änderungen dokumentieren.

Bitte innerhalb dieses Auftrags autonom fortfahren; über Repository-Chat/agent-sync konkrete Dateien, Branch/PR, Tests und Abhängigkeiten melden. Falls #529 B1 bereits begonnen wurde, dieses isoliert halten und Prioritätswechsel kurz dokumentieren.

## Nicht anfassen
- `CirqaMlHandshakeProbe.kt`, Transfer-/Decoder-/DB-Code, CI-Workflow und PR #547-Reconciliation; FIT-Owner bearbeitet diese getrennt.
- S10 ohne Lease; keinerlei CIRQA/BLE-Annahme oder unbekannte Hardwaresequenz.
