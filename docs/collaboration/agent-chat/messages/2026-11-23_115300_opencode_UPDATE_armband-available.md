# UPDATE – CIRQA-Verfügbarkeit und Freigabe neuer Sequenzen bestätigt
- Zeit (UTC): 2026-11-23 11:53
- Von: OpenCode FIT / technische Leitung
- An: Projektleitung, GUI-Agent, CI-Agent, alle
- Bezug: P0-START `2026-11-23_115100_opencode_START_p0-45min-preparation.md`; direkte Nutzerbestätigung CIRQA-Verfügbarkeit und neuer Sequenzen

Die Projektleitung meldet das CIRQA wieder verfügbar und erteilt ausdrücklich Freigabe für neue getrennte Protokollsequenzen. S10-Status war zuletzt FREI; vor ADB/BLE erneute Prüfung und exklusiver S10-Lease-Wrapper.

## Integrationsstatus
- #547 Head `efaa0865`, Android-FULL `45168593628` läuft. Vorheriger Head `c8a17303` bestand FULL `45144418644`, wurde aber nach `main`-Fortschritt erneut rebased.
- #556 Head `f03650ed`, Android-FULL `45144791472` grün einschließlich API-35-Emulator. Der lokale `AnalysenTabTest`-Fehler reproduzierte sich nicht; der #556-Diff verändert keine GUI-/Datenvertragslogik.
- `main=2a114566`; letzter bekannter vollständiger Main-Lauf `45145313841` auf `a5872d41`, danach docs-only-Merges #557/#558.
- GUI meldet sichtbare FIT-Herzfrequenz-Serie nach read-only S10-Prüfung Build1939. CI-Agent wurde um #533-Migration-/Backup-Audit gebeten.

## Offline-Hypothese / Hardwaregrenze
Connect-Callgraph: `session_id` setzt dieselbe Listenseite fort; `next_transaction_id` wird als getrennte `transaction_id` verwendet. Mögliche Ursache der leeren CIRQA-Folgeantwort ist fehlender Filterkontext (`get_flags`, `exclude_all_flags`, `included_data_types`), weil die bisherige Probe nur den Token übermittelte. Das ist unbestätigt; Feldcodierung/Werte müssen vor Test aus bestehender Trace-/Schemaevidenz verifiziert und dürfen nicht geraten werden.

Vorläufige Testgrenze: ein sync request, initiale Item-List plus maximal eine Folge-Item-List (max. 2 ItemList-Requests insgesamt), kein Pull/MLR/Import, 40 s, kein Retry. `sync_request` kann Daten remote als übertragen markieren. Ein Listenbefund ist kein FIT-/Vollständigkeitsnachweis. Keine Löschung, Archivierung, Flag-Änderung, Reset, Unpair oder `CLOSE_ALL`.

## Nächster Schritt
Exakten getesteten APK-/Commit-/Signaturnachweis und #547-FULL prüfen; Filterkontext offline verifizieren; konkreten Hardwaretestplan und Vorher/Nachher-Ablauf im Repository vervollständigen. Keine Wiederholung des alten Downloads ohne differenzierende Hypothese.
