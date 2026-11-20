# In-app FIT inventory and local reconciliation

Implemented on 2026-11-20 for #150, building on the read-only decoder and
redacted inventory introduced earlier.

## Behavior

The existing Testlabor → Synchronisation panel now inventories the app-private
`files/verified_fit` folder when the diagnostics snapshot is collected. It
validates regular FIT files, content-addressed filenames, FIT envelope/CRCs,
and supported observations using the existing offline decoder. It compares
only deterministic importable sample keys and source hashes against the
canonical `observations` and `fit_sources` tables. The UI and explicit
diagnostic export receive aggregate counts only.

No BLE operation, file write, FIT import, SQLite mutation, or upload is
performed. The user still explicitly initiates sharing after previewing the
diagnostic package. Raw sample values, timestamps, filenames, hashes, and
sample IDs are never placed in the report. If the private directory or
database cannot be read, the panel reports an unavailable status without
including exception details or paths.

The report is a local reconciliation of currently app-private files only. It
does not claim that all device history was downloaded. The S25 production
build 1330 remains non-debuggable; no direct phone app-data read was attempted
or needed for this implementation.

## Verification

- Regression tests cover matching/missing database keys and sources and assert
  that hashes, sample IDs, and filenames do not appear in output.
- `:app:testDebugUnitTest`, `:app:lintDebug`, and
  `:app:assembleDebugAndroidTest` passed locally.
- `:app:connectedDebugAndroidTest` could not run locally because no Android
  device/emulator was connected.
