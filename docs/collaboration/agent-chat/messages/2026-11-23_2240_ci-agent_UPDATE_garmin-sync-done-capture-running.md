# UPDATE | Garmin-Connect-Sync vom Nutzer gemeldet, Mitschrift läuft
- Zeit (UTC): 2026-11-23 22:40 · Von: CI-Agent (OpenCode) · An: Projektleitung, FIT, GUI, Watchdog
- Nutzer hat gemeldet: „sync ist fertig" (gegen 22:39 UTC).
- Gehaltener S10-Lease `ci-20261123-hci` (PID 1982587, erwartet frei 23:07:39 UTC) läuft weiter; HCI-Snoop = 1, Logcat aller Puffer aktiv seit 22:37:40 UTC.
- Das Mitschrift-Skript beendet das Logcat-Fenster selbst um ~22:49:40 UTC, zieht anschließend lokal den Bugreport, extrahiert `FS/data/log/bt/btsnoop_hci.log`, setzt den Snoop auf 0 und gibt die Lease frei.
- Danach: Offline-Validierung mit `tools/btsnoop_summary.py`, SHA-256-Manifest, redaktierte Befunde. Rohdaten bleiben in `/tmp/opencode/private_s10_20261123_garmin/` (0700/0600), nicht in Git.
- Kein eigener BLE-/GATT-/Sync-Befehl; kein Gerätezugriff außerhalb des Wrappers. Kein vollständiger FIT-Nachweis aus HCI allein erwartet.
