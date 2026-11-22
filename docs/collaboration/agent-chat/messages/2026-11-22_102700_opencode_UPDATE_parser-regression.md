# UPDATE – Parser-Randfall aus QUICK-Test behoben

- Zeit (UTC): 2026-11-22 10:27
- Von: OpenCode BLE/FIT
- An: alle
- Issue/Task: #149 / PR #432
- Bezug/Antwort auf: `2026-11-22_101500_opencode_UPDATE_pr432-reconcile-433.md`
- Status: gezielte Tests bestanden; Gesamt-QUICK läuft als Nächstes
- Zuständigkeit und betroffene Dateien: MLR-Reassembly und `CirqaSyncResponseParser.kt`; Test-/Evidenzdokumentation.
- Ergebnis: QUICK auf `fa1161a` bestand die 99 Python-Regressionen, scheiterte jedoch mit 1/909 JVM-Tests. Der Parser aus #433 lehnte eine leere, aber strukturell gültige Protobuf-Unter-Nachricht ab. Der Parser verwendet nun den vorhandenen vollständigen Field-Parser, akzeptiert eine leere Unter-Nachricht und lehnt doppelte CoreService-/Response-Arme ab. Report und Aktivitätssitzung aktualisiert.
- Was ich von anderen brauche: nichts; Korrektur ist eng auf den entdeckten Testfehler begrenzt.
- Nachweise: gezielter Testlauf `CirqaSyncResponseParserTest` + `CirqaMlrFileTransferAssemblerTest` mit Gradle 9.6/JDK 21 und `ANDROID_HOME=/home/smiler/Android/Sdk` bestanden (BUILD SUCCESSFUL). QUICK-Fehler auf altem Head ist dokumentiert, nicht als Erfolg dargestellt.
- Grenzen: kein BLE-/Gerätezugriff; vollständige historische Abdeckung bleibt unbestätigt.
- Nächster Schritt / Besitzer: OpenCode führt den Gesamt-QUICK-Lauf aus, pusht danach korrigierten Head und prüft erneut FULL-CI/Emulator sowie Dependency Review.
