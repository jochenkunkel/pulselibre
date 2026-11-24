# Offene Fragen und ungelöste Punkte

> Stand 24.11.2026. Nur Punkte, die **nachweislich offen** sind. Gelöste Widersprüche stehen in [`01_ble-protokoll.md`](01_ble-protokoll.md) §8 und [`02_feldkatalog.md`](02_feldkatalog.md) §12. **Keine vollständige Gerätehistorie behaupten**, solange die Kriterien in §1 nicht erfüllt sind.

## 1. Abdeckungs-Beweis (P0, höchste Priorität)

`coverage=device-history-unverified` bleibt zwingend; `runDrained` heißt nur „Queue dieses Laufs abgearbeitet". Für einen Abdeckungsnachweis fehlen:

1. Alle vom Gerät **angebotenen** Dateien bytegleich archiviert (nur 38/39 im Garmin-Connect-Vergleich; 39. Transfer nicht belegt).
2. Replay-/Feldabdeckung für **alle decoding-relevanten** Felder (109.960 unbekannte Feldvorkommen, 10.283 Inventareinträge).
3. Konsistente Backup-/Restore-Kette über alle Stores.

Offen: ob das Band die fehlenden recent Intervalle überhaupt anbot; ob das 120-s-Limit fehlende Messwerte **verursachte**; Raw-Device-List-Total, exakte Request-Anzahl, per-File-Stage-Totals; Fortsetzungszustand (Session-Cursor/Token) beim Timeout und Identität/Stage des 37. ausgewählten Items (Screenshot unvollständig). → [`04_geraete-evidenz.md`](04_geraete-evidenz.md) §5/§7

## 2. Der S10-Lauf 24.11.

- **`failed=20`:** 20 dekodierte FITs scheiterten im Import-/Commit-Pfad; konkreter **Exception-Typ nicht belegt**. Synthetische `StageCounts`-Regression ausstehend (kein Hardware-Repeat).
- **Leere letzte Transaction-Follow-up-Antwort**, `incompleteReason=next-transaction-followup-empty`: sicherer, eindeutiger Sync-Status-Pfad für einen künftigen Einzelversuch fehlt; kein weiterer S10-Zugriff ohne neue Entscheidung.
- **Kein Post-Run-Backup** am 24.11. → kein Backup-Diff-Nachweis der Persistenz.
- **GUI-Schnittstelle:** lesbare Quelle für vier Zähler (abgerufen/archiviert/importiert/offen) und Lauf-ID/Auslöser fehlt; ohne persistierte Quelle keine Zahlen erfinden.

## 3. Protokoll (Details in [`01_ble-protokoll.md`](01_ble-protokoll.md) §8)

- 19-Byte-`5050`-Konfigurationsfelder und eventuell nötige zusätzliche Antwort nicht entschlüsselt.
- 5024-Binärbytes, CIRQA-Produkt-/Modellzuordnung, Capability-Maske, 5031-Subtyp-Map, 16 Directory-Headerbytes.
- Bedeutung der 5002-Konstantfelder und des 5004-`byte0`; komprimierter 5004-Transfer.
- Request-Felder 4/5/6 (`exclude_all_flags`, `get_flags`, `included_data_types`) und ihre UUID-Quellen – **keine** Werte raten.
- Ob `next_transaction_id` jemals weitere **befüllte** Transaktionen liefert: drei No-Pull-Probes waren leer, Gate 3/3 verbraucht, kein vierter Versuch freigegeben.
- Service 16 (Accelerometer) auf CIRQA unbestätigt; `startMotionDiscovery` sendet keinen Start-Byte; quatix-7-Referenz nicht übertragbar.
- Timeouts/MTU/Flow-Control, Mitternachtsverhalten, genaue Einheiten/Skalierungen (Kalorien, HRV, Atmung).
- Auth 5101–5112 (`AuthManager mr2`, 15-s-Gate) als führende, **unbewiesene** Hypothese für den fehlenden Connect-Schritt.
- RFCOMM/SPP für dieses Gerät widerlegt; USB-Dateizugriff ungeprüft.

## 4. Felder/Semantik (Details in [`02_feldkatalog.md`](02_feldkatalog.md) §7/§13)

- Monitoring-Schritt-Feld: alte Doku F9 `cumulative_steps (×2)` vs. 18.11-CSV (Global 55 ohne F9, F3 `cycles` scale 2) → **offen, nicht still auflösen**.
- Global 233 (15.385 Datensätze, 4-Byte-Bitfeld) und 397 (2.830×, evtl. Hauttemperatur-Abweichung) – Hypothesen, keine Profilnamen.
- `record/135,136,143`, `stress_level/2–4`, `hrv_status_summary/7,8`, `skin_temp_overnight/3` und alle restlichen unmapped Felder → nur im Inventar/Archiv, keine geratene Semantik.
- RR-Codes `-100/-200/-300`, SpO₂-Konfidenz/Modus, `stress_level_time`-Epoch/Offset, Temperatur, Messintervalle: `quality_status_catalog`/`measurement_modes` bleiben `UNKNOWN`.
- Semantische/klinische Validität aller FIT-Felder nicht bewiesen; Parser ist Allowlist, Unbekanntes wird inventarisiert.

## 5. Persistenz/Pipeline (Details in [`03_parser-datenpfad.md`](03_parser-datenpfad.md) §11)

- **`raw_code`-Lücke:** kein Produktionsschreiber befüllt `observation_status_events.raw_code` mit einem echten FIT-Sentinel; `ExcludedField`-Fälle bleiben nur im In-Memory-Ergebnis und als Rohbytes im Archiv. Rohcodes sind nicht als SQLite-Statuszeilen auffindbar.
- **Dedizierte Restore-Tests für v5/v6/v8/v10** im Checkout nicht gefunden (Codepfade vorhanden; Tests decken v1–v4, v7, v9, v11 ab).
- Decoder-Version: Code `"2"` vs. alte Doku `"1"` – im Katalog korrigiert, aber alte Dateien können weiterhin "1" behaupten.
- Unbekannte Restore-/Migrationspfade bei strukturellem `onOpen()`-Reparaturfall (`hasTable`/`hasColumn`) nicht vollständig testabgedeckt.

## 6. Prozess/Organisation

- **PR #522:** NO MERGE bis Parser-Zweitreview und Zähler-/Fragmentabgleich.
- **#599:** resumierbarer inkrementeller Abruf – Optimierung, **kein** Full-Pull-Blocker.
- **#601:** technisch erledigt (Backup v11, `#616` gemergt, Post-Merge-FULL `45377106828` grün); formale Schließung durch FIT/Owner offen.
- **PR #618 (Rollenwechsel):** `gh pr checks` meldet „no checks reported" (docs-only); FIT+GUI müssen Prüfung/Abnahme abstimmen. CI nur Monitor/Berater.
- **Watchdog-Rückbau** separat vom Owner beauftragt; keine neue Watchdog-Funktion aufbauen.
- **Aufräumung:** [`07_aufraumungs-vorschlag.md`](07_aufraumungs-vorschlag.md) Option A umgesetzt (keine Löschung); B (Archiv) und C (Löschung) **nicht** ausgeführt. Collaboration-Daten vollständig behalten ([`06_collaboration-daten.md`](06_collaboration-daten.md)).

## 7. Nicht offen (bewusst festgehalten)

- **Vollständige Gerätehistorie** ist nicht bewiesen – aber auch nicht widerlegt; das ist die zentrale offene Frage, kein Negativbefund.
- **Datei 39** ist nicht vollständig belegt – 38/39 bytegleich; kein 39. Transfer im Capture.
- **36/37** = erfolgreiche selektierte Pipelines, **nicht** 36 von 37 Gerätedateien oder Tagen; Terminalursache 120-s-Limit.
- **294 (S25) vs. 27.736 (S10)** sind verschiedene Geräte-/Versuchsstände, **nicht addierbar**.
- **CSV/DB-Differenz 157** ist aufgelöst (Live-Events nach Snapshot, kein Verlust), kein offener Punkt mehr.
