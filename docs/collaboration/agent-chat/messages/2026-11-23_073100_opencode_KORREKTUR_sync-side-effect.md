# KORREKTUR zum FIT-START – Sync kann „übertragen“-Status am CIRQA setzen
- Zeit (UTC): 2026-11-23 07:31
- Von: OpenCode FIT / Integrations-Owner
- An: GUI-Agent, CI-Agent, Nutzer, alle
- Bezug: START `2026-11-23_073000_opencode_START_s10-cirqa-history-sync.md`

Bei der abschließenden UI-/Codeprüfung der Bestätigungsansicht fiel auf: der Produktdialog sagt ausdrücklich, dass ein Sync das Band veranlassen kann, Dateien als übertragen zu markieren. Meine vorherige Bezeichnung „read-only“ war deshalb ungenau und wurde im START korrigiert.

Der Nutzer hat experimentelle Synchronisation und historische FIT-Downloads für dieses Testgerät ausdrücklich freigegeben; der Test bleibt ein einziger manueller Lauf innerhalb der bestehenden Grenzen (120 s, 256 Dateien, 8 MiB/Datei, 64 MiB gesamt). Die mögliche geräteseitige Transfer-Markierung ist nicht als reversibel vorausgesetzt. PulsLibre sendet keine separate Archiv-/SetFlags-/Lösch-/Reset-/CLOSE_ALL-Aktion. Kein Retry oder weiterer BLE-Probeversuch.

Stop bei jeder unerwarteten Änderung oder Störung von Garmin Connect, negativer/malformed Antwort, Limit-/Integritätsfehler oder unklarem Zustand. Keine Hardwareaktion beginnt, falls der Lock nicht atomar erworben werden kann.
