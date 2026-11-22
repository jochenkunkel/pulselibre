# OpenCode – S10 Produkttest

- Beginn/Aktualisierung: 2026-11-22 07:24 UTC
- Status: aktiv
- Agent/Umgebung: OpenCode, eigener Worktree `/tmp/opencode/pulselibre-s10-product-test-20261122`
- Rolle/Auftrag: BLE/FIT; Nutzerfreigabe für S10-App-UI-Prüfung und einen bestehenden Historien-Download.
- Jetzt: Geräte-/Installations-/CI-Voraussetzungen prüfen, keine Instrumentation oder experimentellen BLE-Sequenzen.
- Geplant: ein UI-initiierter Produktdownload ohne Retry, sofern Voraussetzungen/Gates erfüllt.
- Dateien: eigener Gerätebericht, Sitzung und Chat; keine Produktcodeänderung.
- Branch: `opencode/s10-product-test-20261122`
- Abhängigkeiten: S10-Verfügbarkeit, atomare Host-Lease, voller CI-Nachweis und kompatible Installation.
- Grenzen: Datenerhalt; kein Reset/Delete/Unpairing/SetFlags; kein Live-zlib-Einbau; `coverage=device-history-unverified`.
