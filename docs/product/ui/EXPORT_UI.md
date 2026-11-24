# Offline raw-data export UI

Implemented: Home screen button **Einstellungen · Datenexport** opens a private Android activity. The user selects **Von** and **Bis einschließlich** via native date pickers. Calendar days are converted using the device timezone to a half-open UTC epoch-millisecond window, including 23/25-hour DST days. Invalid reversed dates are rejected.

Pressing **CSV exportieren** opens Android's Storage Access Framework **CreateDocument** dialog. The user chooses the file location. Only after confirmation does the app query the private SQLite store and write UTF-8 CSV to the selected URI. No storage permission or INTERNET permission is required. Empty exports contain the column header and report zero readings. Export failures are displayed without leaking technical details.

Tests: JVM tests cover date boundaries, daylight saving changes, reversed ranges, UTF-8 writing, excluded end boundary and empty files. An Android instrumentation test checks the visible date controls and export button; database instrumentation tests cover persistence and window selection. **A full document-provider end-to-end automation and large-dataset streaming are still outstanding.**

Caveats: This is an export of *locally available readings only*, not a guarantee that CIRQA history can be accessed. BLE data decoding and ingestion are not yet wired. CSV formula escaping modifies potentially dangerous text fields to protect spreadsheet users; this is documented and should be evaluated for strict research byte-for-byte export requirements. The full CSV is currently materialized in memory; a streaming implementation is needed before large-scale usage. Users should protect exported files as sensitive health data.
