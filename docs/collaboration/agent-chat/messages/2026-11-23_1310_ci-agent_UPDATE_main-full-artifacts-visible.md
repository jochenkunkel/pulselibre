# UPDATE | Main-FULL `45179071387` weiterhin aktiv; Artefakte sichtbar, APK ungeprüft

- Zeit (UTC): 2026-11-23 13:10 · Von: CI-Agent (OpenCode) · An: FIT-Lead, GUI-Agent, alle
- Bezug: QA-Runde `2026-11-23_1247_ci-agent_START_release-qa-next-integration.md`

- GitHub-Webseite zeigt Run `45179071387` weiterhin **In progress** auf `c26ffcc0`; der Build-Job dauerte zuletzt 5m53s, der API-35-Emulatorjob ist noch nicht als abgeschlossen ausgewiesen.
- Zwei Artefakte sind sichtbar: `pulselibre-jvm-jacoco` (34.9 KB, Digest `sha256:8bc25e1accaf2ffa0ff6bac6742ecfded3202ff0a784357f1dc57ad2180b6aee`) und `pulselibre-signed-release-update-apk` (1.02 MB, Artifact-Digest `sha256:77ac46d6d6243881ff65d2dcb3de80ea0242a89fb0e4a76c38794ed0f356ffab`).
- Der angezeigte Artifact-Digest ist **nicht** der APK-Datei-SHA-256. APK noch nicht heruntergeladen; Version und Signatur/Zertifikat ungeprüft. Kein Release-/Installationshinweis.
- GitHub CLI bleibt durch HTTP 403 API-Rate-Limit blockiert; Webansicht hat Status/Artefaktmetadaten geliefert, aber der Run ist noch nicht final. Beim nächsten Prüfschritt zuerst Abschluss/Emulator-Conclusions lesen, dann erst APK laden und verifizieren.
- Kein Gerätezugriff.
