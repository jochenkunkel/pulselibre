# Collaboration-Daten: Leseführer und separater Freigabevorschlag

> **Entscheidung der Projektleitung (24.11.2026):** Der vollständige Chat- und Abstimmungsbestand unter `docs/collaboration/` **bleibt erhalten**. Diese Datei ist rein beschreibend und schlägt **keine Löschung** vor. Etwaige Verdichtung oder Archivierung erfolgt ausschließlich nach einer gesonderten Freigabe.

## 1. Was vorhanden ist (Stand 24.11.2026)

| Bereich | Dateien | Größe | Inhalt |
|---|---:|---:|---|
| `docs/collaboration/agent-chat/messages/` | 373 | 1,5 MB | Zeitliche START/UPDATE/ENDE/BITTE/ANTWORT/BLOCKIERT-Nachrichten, 21.–24.11.2026 |
| `docs/collaboration/agent-sync/` | 197 | 892 KB | Gezielte, adressierte Abstimmungen inkl. `INDEX.md` (Zentralregister, 97 Einträge mit Status `offen`) |
| `docs/collaboration/agent-activity/sessions/` | 82 | 356 KB | Sitzungsprotokolle mit Zuständigkeiten, Tickets, Dateien, Fortschritt |
| `docs/collaboration/*.md` (Root) | 4 | – | `README.md`, `DEVICE_LEASE.md`, `CURRENT_BAND_RAW_DATA_POLICY.md`, `AGENT_HISTORY_REVIEW_2026-11-20.md` |
| **Gesamt** | **656** | **2,8 MB** | unverändert, nichts entfernt |

Verteilung der Chat-Nachrichten nach Absender: `opencode` 179, `ci-agent` 61, `claude` 50, `watchdog` 63, `chatgpt` 12, `claude-code` 7.

## 2. Wo man mit wenigen Klicks einsteigt

1. **`docs/collaboration/agent-sync/INDEX.md`** – zentrales Register, chronologisch, mit Status und Link zur jeweiligen Nachricht. Für offene Abstimmungen zuerst nach `offen` filtern.
2. **`docs/collaboration/agent-chat/messages/`** – nach Dateiname (UTC-Zeitstempel) sortieren; START/ENDE-Paare eines Tasks gehören zusammen.
3. **`docs/collaboration/agent-activity/sessions/`** – wer war wann mit welchem Ticket und welchen Dateien aktiv.
4. **`docs/collaboration/CURRENT_BAND_RAW_DATA_POLICY.md`** und **`DEVICE_LEASE.md`** – verbindliche Daten- und Gerätegrenzen.

**Für die P0-Themen besonders relevante Stränge** (als Einstieg, nicht als Ersatz für die Originale):
- S10-Gerätetest und Full-Pull: Chat `2026-11-24_103000_*ENDE_s10-test-incomplete`, `103900_*UPDATE_s10-auto-run-recheck`, `105300_*UPDATE_s10-counter-root-cause`; Sync `2026-11-24_1330_ci-agent-an-fit_dateiabstimmung-freigabe`, `1400_*_s10-test-freigabe-prioritaet`, `1430_*_s10-test-entsperrt`, `1500_*_s10-final-go`, `1530_*_backup-v11-gemergt-hinweis`.
- Backup v11 / #601: Sync `2026-11-24_1000_*_backup_fit-kandidaten-freigabe`, `1530_*_backup-v11-gemergt-hinweis`, `1600_*_uebergabe-manifest-rollenwechsel`.
- BLE-Protokoll/Filter: Sync `2026-11-24_0931_*_p0-transaction-filter-audit`, `1030_*` Serien-/Filtertests; Chat `2026-11-24_093100_*_p0-transaction-filter-audit`, `093800_*_filter-serializer-tests`.

## 3. Automatisierte Nachrichten (nur Lesbarkeit, keine Löschung)

63 der 373 Chat-Dateien sind `watchdog_an-gui_erinnerung_*.md` (automatische Erinnerungen, 21.–23.11.). Sie sind **inkhaltlich repetitiv** und für die fachliche Suche meist nicht relevant. Sie bleiben physisch unangetastet; für den Lesefluss einfach überspringen. Eine spätere Verdichtung (z. B. ein monatliches Sammelarchiv) wäre ein eigener Auftrag mit eigener Freigabe.

## 4. Warum wir nichts anfassen

- Die Nachrichten sind **Beweismittel** und in Issues/PRs referenziert (z. B. Review- und Freigabevermerke zu #614/#616, S10-Freigaben, Geräte-Lease-Koordination).
- `INDEX.md` verweist auf Dateien; eine Löschung würde Verweise zerstören.
- Die Rollenübergabe an FIT (PR #618) setzt ausdrücklich voraus, dass Übergaben und Abstimmungen nachvollziehbar bleiben.

## 5. Freigabevorlage (bitte von der Projektleitung entscheiden)

- [ ] **A – Empfohlen:** Alle 656 Dateien unverändert behalten; dieser Leseführer ist der einzige neue Eintrag.
- [ ] **B:** Zusätzlich einen kuratierten "wichtige Stränge"-Index pflegen (neue Datei, keine Löschung).
- [ ] **C:** Die 63 Watchdog-Erinnerungen in einem späteren, separat freigegebenen Schritt verdichten/archivieren.

**Aktueller Status:** A ist umgesetzt. B und C sind **nicht** ausgeführt. Es wurden keine Collaboration-Dateien gelöscht, verschoben oder überschrieben.
