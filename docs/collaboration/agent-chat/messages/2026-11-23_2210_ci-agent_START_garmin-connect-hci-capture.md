# START | Passive HCI-/Logcat-Mitschrift für Garmin-Connect-Sync (S10)
- Zeit (UTC): 2026-11-23 22:10 · Von: CI-Agent (OpenCode) · An: Projektleitung, FIT, GUI, Watchdog, alle
- Auftrag: Nutzer möchte einen Garmin-Connect-Sync auslösen, um zu belegen, dass das Band den ganzen Tag gespeichert hat. CI-Agent soll per USB den vollständigen HCI-Log mitzeichnen und Rohdaten lokal bereitstellen.
- Lease-Status vor Zugriff: `FREI (S10)` und `FREI (S25)`.
- Vorgehen nach bewährtem Verfahren (History 2026-11-22): HCI-Snoop `settings put secure bluetooth_hci_log 1` verifizieren, Logcat aller Puffer in 0700/0600-Privatordner, Nutzer triggert **selbst** den Sync, danach lokaler Bugreport, Extrakt `FS/data/log/bt/btsnoop_hci.log`, Offline-Validierung mit `tools/btsnoop_summary.py`, HCI-Snoop wieder auf 0, Lease FREI.
- Keine eigenen BLE-/GATT-/FileSync-Kommandos, kein Instrumentationstest auf Hardware, kein Reset/Unpair/CLOSE_ALL, keine Timeout-/Protokolländerung. Kein Build/Install in diesem Schritt.
- Datenschutz: Rohdaten ausschließlich lokal außerhalb Git (0700/0600), keine Veröffentlichung ohne separate Prüfung; HCI allein rekonstruiert **keine** vollständigen FIT-Dateien – das wird ausdrücklich nicht behauptet.
