# ENDE – Read-only S10-Verlauf-/Live-Diagnose

- Zeit (UTC): 2026-11-22 07:48
- Von: OpenCode FIT/Protokoll
- An: GUI-Agent, FIT-Agent, ChatGPT, alle
- Bezug: START `2026-11-22_074400_opencode_START_s10-history-live-diagnosis.md`; Nutzerfreigabe für read-only Zugriff auf das Test-S10.
- Gerät: SM-G970F, Android 12/API31, Build1757. `BLUETOOTH_SCAN` und `BLUETOOTH_CONNECT` gewährt. Kein Recorder-Service lief zum Abfragezeitpunkt; dies belegt nicht den Ausgang eines vorherigen Startversuchs. Gefilterte letzte Logcat-Rückschau ohne relevante App-/GATT-Zeilen.
- DB-Grenze: `run-as` für die nicht-debuggbare Release-App verweigert; Manifest setzt `allowBackup=false`. Keine DB-Extraktion, kein Export/Backup, kein App-Neustart, kein Sync/Scan/GATT-Aufbau; damit keine riskanten Umwege und keine zusätzliche Geräteoperation.
- Codebefund: keine hardcodierte S25-MAC; Kopplungsauswahl dynamisch über „CIRQA“-Namensfilter, ML-Probe nimmt den ersten Treffer. Live erfordert nach Auswahl erfolgreichen GATT-/CCCD-/ML- und Notification-Pfad. Gleichnamige Kopplungen bleiben als theoretischer Auswahlkonflikt offen.
- Verlauf: FIT-Karte liest nur FIT-Herzfrequenz `heart_rate`/`bpm`, standardmäßig 24 h, navigierbar bis 30 Tage. 17 importierte Dateistatus beweisen keine HR-Beobachtung oder Messzeit in diesem Fenster. Build1757 enthielt Decoder-Fix #416 nicht; Gerätekausalität nicht belegt.
- Berichtsergänzung liegt im offenen PR #422. Keine Produktcodeänderung. Lease nach Zugriff FREI.
- Übergabe: Für DB-Ursache wäre ein gezielter, autorisierter In-App-Export oder Debug-Build erforderlich; für Live-Ursache ein explizit beobachteter UI-Start mit datensparsamem Fehlerstatus. Dafür hier keine weiteren Geräteaktionen durchgeführt.
