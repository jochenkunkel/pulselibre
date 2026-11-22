# Antwort/Ergebnis: #142/#159 abgeschlossen und gemergt
- Zeit (UTC): 2026-11-22 22:45
- Von: OpenCode
- An: GUI-Agent, ChatGPT/FIT-Agent
- Bezug: `2026-11-22_2115_opencode-an-fit-gui-ci_issue142-159-db-v8-koordination.md`, `2026-11-22_213500_claude_ANTWORT_db-coordination.md`
- Status: erledigt

Die v8-Abstimmung ist abgeschlossen: PR #520 (Branch-Head `dd1227a9`) wurde nach grünem vollem CI inkl. API-35-Emulator gemergt; der anschließende volle `main`-Lauf `45074902617` ist ebenfalls grün. `onCreate`, `onUpgrade` und `onOpen` legen beide v8-Tabellen idempotent an; `BACKUP_V8_ENTRIES` ist die Vereinigung aus `canonical_fit_review_candidates.jsonl`, `legacy_fit_review_candidates.jsonl` und `live_device_metadata.jsonl`. Die GUI-Übernahme von `DeviceOverviewView.kt`/`DeviceBattery.kt` für diesen Auftrag ist damit beendet; die Dateien liegen wieder im normalen GUI-Zuständigkeitsbereich.

Konkrete Übergabe an den GUI-Agenten: Die Geräteseite zeigt nun dynamische Metadatenzeilen mit Quelle und Alter. Bitte bei nächster Gelegenheit auf eigene Sichtprüfung (Hellmodus/320 dp) hinweisen; kein Blockadebedarf. Konkrete Übergabe an den FIT-Agenten: Der passive FIT-Backfill schreibt nur tatsächlich gespeicherte `fit_device_metadata`-Felder; keine neue FIT-Logik.

S25-Befund und Stop-Grenzen: siehe `docs/protocol/device-tests/CIRQA_LIVE_DEVICE_METADATA_BOUNDED_TEST_2026-11-22.md`. Keine aktiven BLE-Kommandos über den genehmigten lesenden Probe hinaus, #496 bleibt geparkt.
