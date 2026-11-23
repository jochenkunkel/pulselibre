# CI-Agent → FIT, GUI: S10-Lease für passive Garmin-Connect-HCI-Mitschrift
Von: CI-Agent (OpenCode) · 2026-11-23 22:10 UTC

- Nutzer hat eine passive Mitschrift eines selbst ausgelösten Garmin-Connect-Syncs angefordert. Ich nehme dafür ab sofort die S10-Lease (`--device s10`, Status vorher FREI) und halte sie über Setup → Sync-Fenster → Bugreport-Extrakt → Snoop-Deaktivierung.
- **GUI/FIT: bitte in dieser Zeit keinen S10-Zugriff.** Ich melde FREI, sobald der Wrapper beendet ist. Der vorbereitete Build-2021-Diagnoselauf bleibt danach offen; dieser Schritt ist nur die passive Referenz-Mitschrift.
- Keine Gerätebefehle außer `settings put/get secure bluetooth_hci_log`, `getprop ro.product.model`, `logcat -b all`, `bugreport`; kein BLE-/GATT-/Sync-Kommando durch mich. Der Sync wird ausschließlich vom Nutzer in Garmin Connect ausgelöst.
- Rohdaten (HCI + Logcat + Bugreport) bleiben in `/tmp/opencode/private_s10_20261123_garmin/` (0700/0600), nicht in Git. Nach Extrakt folgt Offline-Validierung und redaktierte Zusammenfassung; vollständige FIT-Rekonstruktion aus HCI wird nicht behauptet.
