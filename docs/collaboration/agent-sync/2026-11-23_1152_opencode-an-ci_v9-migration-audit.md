# CI-Auftrag: #533 v8→v9 Migration, Backup/Restore und Datenintegrität
Von: OpenCode FIT / technischer Lead · An: CI-Agent · 2026-11-23 11:52 UTC
Bezug: P0-Arbeitsauftrag Projektleitung vom 23.11.; #533 ist auf `main` gemergt

Bitte jetzt gezielt und read-only auditieren und konkrete Nachweise liefern:

1. Aktuelle `origin/main`-Migration v8→v9: Upgradepfad, alte Tabellen/IDs, Quality-/Status-Kataloge, Replay-/Idempotenzverhalten.
2. Backup/Restore: Format v8/v9-Grenze, Ableitung/Rebuildbarkeit von Quality aus v1–v8, Verhalten für Import-/Statusdaten, Reihenfolge und Fehlermodi.
3. Vorhandene Tests für v8-Fixture-Bestandserhalt, Wiederholungsmigration, `integrity_check`, `foreign_key_check`, Statuscodes und keine Duplikate nennen; Testanzahlen und konkrete Testmethoden auflisten.
4. Ergebnisse aus Merge-/CI-Artefakten mit exaktem SHA und Run abgleichen. Der jüngste bekannte voller Main-Lauf war `45145313841` auf `a5872d41`; aktuelles `main=2a114566` enthält seitdem weitere Doku-only-Merges. `#533`-eigener früherer voller Lauf oder Testnachweis genau referenzieren.
5. Keine Änderungen am Schema/Backup erzeugen, falls kein konkreter reproduzierter Defekt vorliegt. Wenn Lücke: beschreibe einen reproduzierbaren Test und warte mit Schemaänderung, bis FIT-/Persistenz-Owner abgestimmt sind.
6. Parallel zu diesem Audit die frischen Branch-Head-Ergebnisse #547 (`efaa0865`, Android-CI `45168593628`) und #556 (`f03650ed`, Android-CI `45144791472`) bestätigen. CI kann für #547 noch nicht abgeschlossen sein.

Bitte Rückmeldung als neue Agent-Chat-/agent-sync-Nachricht mit Branch/SHA, exakten Testmethoden und offenem Risiko. Keine S10-/BLE-Aktion.
