# Collaboration-Daten: Leseführer und Bestand

> **Entscheidung der Projektleitung (24.11.2026):** Der vollständige Chat- und Abstimmungsbestand unter `docs/collaboration/` **bleibt erhalten**. Diese Datei ist rein beschreibend und schlägt **keine Löschung** vor. Etwaige Verdichtung oder Archivierung erfolgt ausschließlich nach einer gesonderten Freigabe.

## 1. Was vorhanden ist (Stand 24.11.2026)

| Bereich | Dateien | Größe | Inhalt |
|---|---:|---:|---|
| `docs/collaboration/agent-chat/` | 378 | – | Chat-Nachrichten und Chat-Einstieg |
| `docs/collaboration/agent-sync/` | 201 | – | Adressierte Abstimmungen, Index und Formatregeln |
| `docs/collaboration/agent-activity/` | 84 | – | Sitzungsprotokolle |
| `docs/collaboration/` Root | 5 | – | Richtlinien, Rollen-/Datenregeln und Einstieg |
| **Gesamt** | **668** | **2,8 MB** | Bestand einschließlich Capture-Bereinigungsaufgabe |

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

Automatische `watchdog_an-gui_erinnerung_*.md`-Nachrichten können für die fachliche Suche übersprungen werden. Der Bestand bleibt erhalten; eine spätere Verdichtung oder Archivierung wäre ein eigener Auftrag mit eigener Freigabe.

## 4. Warum wir nichts anfassen

- Die Nachrichten sind **Beweismittel** und in Issues/PRs referenziert (z. B. Review- und Freigabevermerke zu #614/#616, S10-Freigaben, Geräte-Lease-Koordination).
- `INDEX.md` verweist auf Dateien; eine Löschung würde Verweise zerstören.
- Die Rollenübergabe an FIT (PR #618) setzt ausdrücklich voraus, dass Übergaben und Abstimmungen nachvollziehbar bleiben.

## 5. Abgrenzung

Die Entscheidung für Richtung B bezog sich auf ausgewählte Nicht-Collaboration-Dokumente. Der Collaboration-Bestand wurde **nicht** gelöscht, verschoben oder archiviert; er bleibt im aktiven Pfad. Interne Verweise auf neu geordnete Dokumente wurden angepasst.
