# START – P0-Vorbereitung bis CIRQA-Rückkehr: Integration, Pagination und Hardwareplan
- Zeit (UTC): 2026-11-23 11:51
- Von: OpenCode FIT / technische Leitung
- An: Projektleitung, GUI-Agent, CI-Agent, alle
- Bezug: verbindlicher P0-Arbeitsauftrag Projektleitung vom 23.11.2026

## Aktueller Stand
- `origin/main=2a114566`; neuester bekannter vollständiger Main-Android-CI-Lauf `45145313841` auf `a5872d41` grün; danach Dokumentations-Merges #557/#558.
- #547 Head `efaa0865` (auf `e08216d8` aktualisiert), lokale gezielte FIT-Tests 33 bestanden; aktueller Android-FULL `45168593628` läuft. Vorheriger Head `c8a17303` hatte FULL `45144418644` grün, ist nach weiterem Rebase stale.
- #556 Head `f03650ed`, vollständiger Branch-Head-CI `45144791472` grün einschließlich API-35-Emulator. Der lokale `AnalysenTabTest`-Fehler wurde auf sauberem GitHub-Emulator nicht reproduziert; Diff verändert keine GUI- oder Datenvertragslogik.
- GUI-Agent meldet aus read-only S10-Build1939: FIT-Herzfrequenz im Analysen-Dashboard sichtbar, Quelle/Zeitsbasis markiert; dünne Daten über sieben Tage erschweren die Lesbarkeit. Kein BLE/Sync beim GUI-Test.
- CI-Agent um gezieltes v8→v9-/Backup-/Restore-/Idempotenz-Audit mit Testmethoden und SHA gebeten.
- Projektleitung bestätigt CIRQA-Verfügbarkeit und gibt neue getrennte Protokollsequenzen frei. S10-Sperre war zuletzt FREI; vor jedem ADB-/BLE-Zugriff erneut prüfen und Lease-Wrapper halten.

## Prioritäten
1. #547 FULL-CI vollständig lesen; bei grünem exaktem Head und sauberem Merge-Zustand integrieren, danach Main-FULL kontrollieren.
2. #556 unveränderten Head/CI und Konfliktfreiheit prüfen, danach integrieren; Main-FULL kontrollieren.
3. Pagination-Request/Response offline nachvollziehbar rekonstruieren, Cursor/Transaktionskontext unterscheiden und einen begrenzten, reproduzierbaren Test ohne Pulls vorbereiten.
4. v9-Migrationsnachweis und signierte APK/Hash/Signatur vom CI-Agent verifizieren.
5. Keine Hardwareaktion vor passendem getestetem APK-Commit und Geräte-Lease; Test strikt begrenzen, keine destruktiven/archival Aktionen.

## Prüfbare Pagination-Hypothese
- Connect-Callgraph unterscheidet `session_id` (Cursor derselben Item-List-Session) von `next_transaction_id` (Token für getrennte spätere Anfrage als `transaction_id`). Antwort ohne Session-ID aber mit Token ist keine nachgewiesene weitere Seite.
- Mögliche Erklärung für leere CIRQA-Folgeliste: bisher wurde nur der Token gesendet; Connect kann zusätzlich Filterkontext (`get_flags`, `exclude_all_flags`, `included_data_types`) übertragen. Das bleibt unbestätigt. Werte und Feldcodierung nicht raten, sondern aus vorhandener Trace-/Schemaevidenz verifizieren.
- Falls Codierung valide: genau ein `sync_request`, initiale Item-List plus höchstens ein transaction follow-up (max. 2 ItemList-Requests insgesamt), kein MLR/FIT-Pull/Import, höchstens 40 s, kein Retry. Nur Status-/Counts-/Presence-Bits protokollieren, keine UIDs/Tokens/Payloads.
- Ein positiver List-Befund beweist weder FIT-Erfolg noch vollständige Historie. `sync_request` kann Gerätedaten remote als übertragen markieren; diese Nebenwirkung muss im bestätigten Ablauf erkennbar bleiben.
