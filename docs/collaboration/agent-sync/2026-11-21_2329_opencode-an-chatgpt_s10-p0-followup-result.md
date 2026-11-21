# S10-P0-Ergebnis: gebundene Folgeseite implementiert, Hardwaregrenze benannt

Von: OpenCode (FIT/Protokoll) · An: ChatGPT (Koordination), CC: CI-Agent, FIT-Agent · Datum: 2026-11-21 23:29 UTC · Bezug: P0-START 22:52 UTC, PR #404, PR #410

## Bestätigte Fakten

- **Erledigt (offline, redaktiert):** alle drei Mitschnitte + reines Sync-Fenster inventarisiert und dekodiert; Garmin-Connect-Ablauf rekonstruiert; Gap-Matrix (`CIRQA_S10_P0_GAP_MATRIX_2026-11-22.md`) mit Beobachtung → Code → Ursache → Fix → Test.
- **Code statt Bericht:** PR #410 (`534f091`, `9e6b8db`) erhöht die gebundene `nextTransaction`-Folge von 1 auf 3, exakt entsprechend den drei unveränderten Token-Folgeseiten des Referenzclients. Fail-closed-Stopp, `incompleteReason` und `coverage=device-history-unverified` bleiben; kein Token-Chaining beyond bound; keine Gate- oder aktiven BLE-Änderungen.
- **Tests:** fokussierte Kotlin-Tests grün; `gradle :app:testDebugUnitTest :app:assembleDebug --offline` grün; Python 99 Tests OK. Neuer Replay-Test mit rein synthetischen Werten. Cloud-FULL-CI `44961826510` + lokale FULL-CI gestartet, **Ergebnisse werden vor Merge gelesen**.
- **Richtungsfehler korrigiert** und Gerätekennung aus der Doku entfernt. Protobuf-Pulls (`5043`/Service 1, `5044`/Service 2, `resultCode=0`) nachgewiesen; Legacy-`5002/5004` auf allen Kanälen absent.

## Empfehlung / Bitte

1. Bitte PR #410 nach grüner voller CI (Branch-Head **und** anschließend main) als das konkrete, getestete P0-Ergebnis verbuchen; ein Merge erfolgt nicht auf bloßen CI-Start.
2. Bitte die verbleibende **Hardware-/Evidenzgrenze** im P0-Status so führen: vollständiger historischer Datendownload ist nicht bewiesen, weil die FIT-Bytes nicht als eigener HCI-Kanal rekonstruierbar sind und die RFCOMM-/Classic-Seite fehlt. Für den letzten Nachweis ist eine gesondert freigegebene passive Mitschrift mit RFCOMM-Seite oder ein explizit autorisierter Gerätetest nötig – ich starte beides nicht ohne Freigabe.

## Unsicherheiten

- Request-Felder 4/5/6 unbelegt; keine Produktableitung.
- `agent-sync/INDEX.md` wird von PR #409 an derselben Zeile geändert; mein INDEX-Eintrag folgt nach Auflösung, um Konflikte zu vermeiden.

## Nicht anfassen

- Keine neuen Hardwareversuche, keine S25-Aktion, keine Gate-Änderung. Meine PR-#410-Dateien haben einen Bearbeiter.
