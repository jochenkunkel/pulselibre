# ENDE – Coverage-Semantik ist per JVM-Test festgelegt

- Zeit (UTC): 2026-11-22 14:20
- Von: CI-Agent (OpenCode)
- An: FIT-Agent, GUI-Agent, ChatGPT, alle
- Auftrag: P1 aus der ChatGPT-Gesamtanalyse – klare Coverage-Semantik.
- Ergebnis: `CirqaSyncCoverage.marker(runDrained, incompleteReason)` liefert unverändert `coverage=device-history-unverified`; `CirqaMlHandshakeProbe.finish()` nutzt die Funktion (eine Zeile, keine Verhaltensänderung). `CirqaSyncCoverageTest` prüft 2/2 lokal grün (Gradle 9.6.0, `ANDROID_HOME=/home/smiler/Android/Sdk`), dass weder `runDrained=true` noch ein anderer `incompleteReason` die Marke aufwertet.
- CI/Nachweise: PR #489 gemergt als `4bda9e1`. Voller CLOUD/FULL-Lauf auf dem Branch-Head (`45031141035`) und der PR-/Merge-Lauf (`45031136855`) grün, inkl. Emulator API 35; dependency-review `45031136832` grün. Der FULL-Lauf auf dem resultierenden `main` (`45032146900`) wurde gestartet.
- Abgrenzung: `coverage=device-history-unverified` bleibt fachlich unverändert; kein Gerätezugriff, kein BLE/GATT/FileSync, keine echten Gesundheitsdaten, keine Rohdaten in GitHub. Backup-v7-Manifest-/Prüfsummen-Tampering bleibt als nächste, noch nicht bearbeitete Lücke (Emulator oder Produkt-Extraktion; vorherige Ankündigung nötig).
- Übergabe: FIT-Agent/GUI-Agent benötigen keine Rückfrage; ChatGPT kann die Semantik als durch Test abgesichert betrachten, weiterhin ohne Nachweis der vollständigen Gerätehistorie.
