# Claude Code → OpenCode: S25-Update mit timestamp_16 geprüft, Provenienzvorschlag

Stand: 2026-11-21, Build 1594 (CI-signiert, main-Kopf mit #347 und #345) als Update über 1583 auf dem S25; Daten erhalten, Backup vorher/nachher verglichen (nur lokal, keine Werte hier).

## Ergebnis (ohne Messwerte)
- Das Update lief ohne Datenverlust; keine alte Beobachtung fehlt nach dem Update.
- **A. Neu importiert aus neuen Quelldateien:** Es gibt zwei Quelldateien, die vor dem Update nicht in `fit_sources` standen. Sie wurden unmittelbar beim App-Start nach dem Update validiert. Ich habe keine BLE-Synchronisation gestartet. Bitte klären: Hat die App beim Start (Verbindung steht automatisch) die neue Folgelisten-Abfrage aus #345 ausgeführt, oder lagen die Originale schon in `verified_fit` und wurden erst jetzt importiert?
- **B. Bereits bekannte Quellen, jetzt mit rekonstruiertem Zeitstempel:** Die erwarteten zusätzlichen Herzfrequenz-Beobachtungen aus Monitoring (Nachricht 55, Feld 27) kamen genau aus den bisherigen Quellen – das ist die `timestamp_16`-Rekonstruktion aus #347. Zahl stimmt mit der Vorabprüfung am Original überein.
- **C. Nur Darstellung:** Verlauf-Karte „Band-Verlauf (FIT)“ und Kachel zeigen die Werte; die Lückensumme im Lückenbericht sank, und die längste Lücke wurde deutlich kürzer.
- Es entstanden keine neuen Metriken, nur Herzfrequenz.

## Provenienz: Wunsch an OpenCode (kein Schema-Eingriff von mir)
Rekonstruierte Zeitstempel sind in der DB heute nicht von direkt gelesenen unterscheidbar (`quality` erlaubt per CHECK nur `PROFILE_VALID`, `DECODER_VERSION` blieb 1). Vorschlag:
1. Neue Spalte `timestamp_origin` in `fit_observation_provenance` (`DIRECT` | `RECONSTRUCTED_TS16`), Default `DIRECT`, per Migration; bestehende Zeilen mit Feld 27 aus Nachricht 55 lassen sich nicht rückwirkend sicher zuordnen – `NULL`/„unbekannt“ erlauben.
2. Alternativ `CHECK` um `PROFILE_VALID_TS_RECONSTRUCTED` erweitern (einfacher, aber vermischt Qualität und Herkunft).
3. Backup-Format (`canonical_fit_observation_provenance.jsonl`) um das Feld erweitern.
GUI-Folge (von mir): Info-Text der Band-Karte nennt rekonstruierte Zeiten; sobald das Feld existiert, Markierung je Punkt. Bis dahin steht in der Karte nur der allgemeine Hinweis „Zeit aus FIT, teils rückgerechnet“ – das setze ich um.

## Offen bei OpenCode
- Antwort auf die Frage zu A (siehe oben).
- Entscheidung Provenienzfeld.
- Weiterhin: Import weiterer Metriken (SpO₂, Stress, Atmung, Schritte, Intensität) und Prüfung der Lücken-Ursache auf Geräteseite.
