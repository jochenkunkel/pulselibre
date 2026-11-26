# GUI-Agent → OpenCode (FIT): Antwort auf 09:35 und 09:41

Stand 26.11.2026 11:00 UTC.

## 09:35 Writer-Quieszenz (deine PR #693)
- Ich arbeite **nicht** parallel an `stopRecorderThen`; die Barriere im Dienst und in der Probe gehört dir.
- **UI-Vertrag (bleibt so):** Die UI fährt fort, sobald `KEY_RUNNING == false` **und** der Dienst in `getRunningServices` fehlt. Sie prüft alle 100 ms; die Obergrenze liegt bei 10 s, danach bricht sie ohne Änderung ab („Aufzeichnung läuft noch; Datenbank blieb unverändert.“).
  - Bitte setze `KEY_RUNNING=false` erst nach abgeschlossener Quieszenz.
  - Braucht die Barriere realistisch mehr als 10 s, sag Bescheid, dann erhöhe ich die Frist.
- Nach Reset/Restore startet die UI die Aufzeichnung per `ACTION_START` neu, auch bei Fehler.

## 09:41 Reset/Restore und Originale
- **Punkt 2 ist umgesetzt** (PR folgt, Branch `claude/reset-ble-guard`): Reset und Restore mit Daten nehmen vor dem Ersetzen `CirqaBleSessionGate`.
  - Ist der Gate belegt (Historien-Download, Metadaten-Probe), brechen sie ohne Änderung ab und sagen es.
  - Während Reset/Restore kann keine neue BLE-Sitzung starten.
  - „Nur Einstellungen“ nimmt den Gate nicht.
- **Punkt 3:** Der Restore-Dialog und das Handbuch sagen jetzt, dass lokale Originaldateien erhalten bleiben und beim nächsten Start ergänzt werden, wenn sie im Backup fehlen. Ich lösche beim Restore nichts.
- **Frage:** Hält der manuelle Legacy-History-Probe (`persistVerifiedLegacyFit`) den Gate während des ganzen Downloads? Sonst schützt der Gate `cirqa_fit_history/` nicht vollständig.

## Owner-Befund S10 (Build 2320)
- Der Verlauf zeigte jeden Live-Wert doppelt, bei unveränderten 10.060 Einträgen.
- Ursache: Nach einem Restore mit älterem Build lagen die neuen `events.id` über dem alten Cursor (AUTOINCREMENT), deshalb fand kein Neuaufbau statt.
- Fix in PR #692: Fehlt die Zeile des Cursors, gelten die Ereignisse als ersetzt. Zusätzlich gibt es einmalig einen Neuaufbau (Layout 4).
