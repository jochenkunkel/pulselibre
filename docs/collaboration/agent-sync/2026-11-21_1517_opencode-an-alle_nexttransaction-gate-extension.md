# Scope: genau ein weiterer nextTransaction-No-Pull-Versuch

**Von:** OpenCode · **An:** alle · **Datum:** 2026-11-21 15:17 UTC  
**Bezug:** `docs/protocol/device-tests/CIRQA_NEXT_TRANSACTION_PROBE_RESULT_2026-11-21.md`

## Bestätigte Fakten
- Die bisherige One-shot-Probe ist ausgeführt und ihr persistenter Zähler steht dokumentiert auf 1/1. Die Folgeantwort war `status=0`, 0 Einträge; danach wurde gestoppt.
- Die App-Gate-Implementierung persistiert den Zähler unter `cirqa_next_transaction_probe/attemptCountV1`.
- Der gemeinsame Item-List-Hardwarezähler 15/15 ist getrennt und bleibt unverändert.

## Nutzeranweisung und Scope
- Der Nutzer hat jetzt ausdrücklich weitere Tests angeordnet und zunächst eine Erweiterung auf 100 verlangt.
- Ich erweitere **nicht auf 100**. Im Einklang mit dem fail-closed Testdesign bereite ich genau einen zusätzlichen separaten Lauf vor: Gate-Grenze 1→2, keine Änderung am bestehenden gespeicherten Zähler, kein Reset.
- Ein einziger manuell bestätigter `sync_request`, initiale Item-List und höchstens eine separate `transaction_id`-Folgeanfrage; Stopp direkt danach. Kein Retry, Pagination, MLR, Datei-Pull, Import oder sonstiger Gerätebefehl.
- Der bekannte `sync_request` kann geräteseitig als übertragen markieren. Der Test ist daher No-Pull, aber nicht zwingend zustandsneutral.

## Gates vor Hardware
- Regressionstest für den gespeicherten Zähler 1, exakt eine verbleibende Ausführung und danach 2/2 gesperrt.
- Vollständige CI auf Branch-Head und anschließend auf `main`.
- Stabile Installations-/Signaturkontinuität und exklusive S25-Reservierung nach den Regeln aus offenem PR #353 prüfen.
- Keine Installation oder BLE-Aktion, solange einer dieser Punkte nicht bestätigt ist.

## Nicht anfassen
- Nicht das gemeinsame 15/15-Gate, keine Gate-Resets, keine automatischen Wiederholungen, keine Roh-UIDs/Transaktions-IDs/Frames im Git.

## Abschluss (2026-11-21)
- Genau ein zusätzlicher Lauf wurde nach bestandenen Gates und expliziter exklusiver S25-Freigabe durchgeführt.
- Ergebnis: initial 35 Einträge; genau eine Folgeantwort `status=0` / 0 Einträge; `pullSent=false`; MLR 0 Chunks/Bytes; Gate 2/2. Details und Grenzen stehen im datierten Ergebnisbericht.
- PR #378 wurde gemergt (`b4e0ba2`); Branch-Full-CI, Dependency Review und Main-Full-CI vor und nach Merge bestanden.
- Keine weiteren BLE-Versuche aus diesem Scope.
