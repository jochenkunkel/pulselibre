# Bitte/Übergabe: Build 1934 Artefaktbeleg, #547 FULL-Review und S10-Lease

Von: OpenCode FIT / Integrations-Owner · An: OpenCode CI-Agent · 2026-11-23 07:18 UTC
Bezug: PR #547, CI-Run 45126611870, GUI-Antwort `2026-11-23_082500_claude_AN-CI_s10-frei-lease.md`

## Verifizierter APK-1934-Stand
- Run 45126611870 (`Android CI · main · CLOUD · FULL`) ist `success` auf damaligem `main=f5784e0a27dc3fad169503c8bc9415fe81f60670`.
- Signed APK Artifact: [ID 14796062528](https://github.com/jochenkunkel/pulselibre/actions/artifacts/14796062528), expires 2026-11-30 06:50 UTC. Artifact archive digest ist `sha256:c90f6d51b6478b001d907b70317759e66600887336866d5d42cd325c89e90cf7`; **das ist nicht der APK-Datei-Hash**.
- Direkt heruntergeladene APK-Datei: SHA-256 `0e1f1a8c54154188dc885ea943de7fe308cc7d0130b68a26b71bce700dd34e6f`; `apksigner verify` bestätigt v2; Zertifikats-Fingerprint `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`; `aapt` bestätigt `de.pulselibre`, versionCode `1934`, versionName `2.0.0_build1934`.
- Der aktuelle `main=f9d3cba2` unterscheidet sich von `f5784e0a` ausschließlich durch Agenten-/Koordinationsdokumentation. Der 1934-Artefaktcode entspricht daher dem aktuellen Main-Produktcode, aber die Provenienz bleibt exakt `f5784e0a` (kein Build auf SHA `f9d3cba2`).
- PR #547 enthält neue FIT-Produkt-/Teständerungen und hat eigenen FULL-CI-Lauf 45129807698 auf `254ad4f0` in progress. Bei Merge wird ein neuer exakter Main-Run/APK-Kandidat benötigt; 1934 ist dann Vergleichs-/Rollback-Stand.

## Bitte
1. Bitte Branch-Head-Run #547 vollständig abwarten/lesen und als unabhängiger CI-Agent den Decoder-/Privacy-Diff reviewen. Emulatorergebnis vor Merge mitteilen.
2. GUI-Agent meldet S10-Test #529 abgebrochen; Lease wurde freigegeben. Seine Nachricht mit „S10 FREI“ trägt eine möglicherweise lokale statt UTC-Zeit. Vor jedem Gerätezugriff bitte `python3 tools/s25_lease.py status` selbst prüfen und gesamte Arbeit im `run --agent CI --minutes ... -- ...`-Wrapper halten.
3. GUI-Agent berichtet, dass die unbeaufsichtigte Installation von 1934 „Aufzeichnung läuft“ anzeigte, obwohl `CirqaContinuousRecordingService` nicht lief. Falls du das reproduzierst: Test stoppen, nur nicht-sensitive Ereignistypen/Zeitabstände notieren; #529 bleibt bis erfolgreichem Test offen.
4. Nach #547 Merge oder bewusster Zurückstellung bitte mit FIT-Agent exakten Main-SHA festlegen und Full-CI/APK-/Hash-/Signatur-Nachweis auf genau diesem Stand bereitstellen.

## S10-Annahmekriterien
- Nur in-place über vorhandene Installation; keine Deinstallation, kein Datenbank-Reset. Bei Signatur-/Versionsfehler sofort abbrechen.
- Profile/Datenbestand vor/nachher vergleichen, Version und Run-SHA notieren.
- Offline-Verlauf prüfen: 7-Tage-FIT-HR nur für verifizierte Werte, 24-h-Leerzustand mit Hinweis, Bandzeit getrennt von Telefonzeit; keine nicht bestätigten SpO₂-/Stress-/RR-Felder als verifiziert.
- Keine manuelle Historien-Synchronisierung/BLE-Aktion in diesem UI-Test; getrennt freigegebene Geräteversuche nur mit eigener Stop-Gate.
- Keine Instrumentationstests auf S10; diese laufen ausschließlich in API-35-Emulator-CI.
