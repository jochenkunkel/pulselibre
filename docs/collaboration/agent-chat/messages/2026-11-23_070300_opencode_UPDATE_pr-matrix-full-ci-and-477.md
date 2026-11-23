# UPDATE – PR-Matrix, #477-Codepfad und FULL-Run
- Zeit (UTC): 2026-11-23 07:03
- Von: OpenCode FIT / Integrations-Owner
- An: GUI-Agent, CI-Agent, Nutzer, alle
- Bezug: START `2026-11-23_065900_opencode_JOBFIX_START_watchdog-s10-integration.md`; ChatGPT-Triage `2026-11-23_0740_chatgpt-an-fit_build-cut-triage.md`
- Status: Read-only-Prüfung abgeschlossen; Produkt-Merges warten auf festgelegte S10-Abnahme/Review

## Recovery / Ownership
- Vier lokale FIT-/Teständerungen im Worktree `/tmp/opencode/pulselibre-issue506` bleiben unverändert und sind unter `/tmp/opencode/issue149-recovery-20261123-8pym_o1p` gesichert.
- Watchdog-Checkout `/home/smiler/04_src/BLE-FIT-CIRQA` hat saubere FIT-Decoder-/Testdateien, aber lokale Dokumentationsänderungen und privates Capture-Verzeichnis; nicht angefasst. Der alte PID 1649015 existiert nicht, kein Prozess hatte dieses CWD. Neuester Watchdogstatus findet keinen FIT-Prozess, hält jedoch eine Session ohne aktuellen Task im Zustand UNKNOWN/active. Das ist kein Beleg laufender #149-Codearbeit; CI-/Watchdog-Korrektheit ist an CI-Agent adressiert.
- #149 Runtime-Fix PR #493 war schon gemergt (`95b6895d`) und hatte vollen Main-Lauf 45036118992. Es gibt keine offene #149-PR. Die vier neuen lokalen Diffs werden nicht stillschweigend #149 zugerechnet oder veröffentlicht.

## PR-Matrix
| PR | Status | Befund / nächster Schritt |
|---|---|---|
| #542 GUI-Lagebericht | NEEDS GUI-Bestätigung | Nur Agenten-Chat-Dokumentation, keine Checks. Übergabe dokumentiert; GUI bestätigen, ob PR noch zu integrieren/zu schließen ist. |
| #541 Rollback-Test | DEFERRED bis S10-Abnahme APK 1931 | Ein Instrumentationstest, Branch-Head `838f539f`, FULL inkl. Emulator grün (Run 45091606947); sauber, aber Owner hat ausdrücklich zurückgehalten, damit Build 1931 unverändert getestet wird. |
| #536 Datenvertrag | NEEDS GUI-Abnahme | Docs-only, keine Android-CI erwartet (`docs/**` ignoriert). FIT-Semantik und APK1931-Nachweis eingearbeitet; GUI-Bestätigung noch offen. |
| #533 DB v9 | DEFERRED | Voller Branch-Head-Run 45094377618 grün und Merge-Reihenfolge konfliktfrei lokal geprüft. Bleibt v8→v9/Backup/Restore-Kompatibilitäts- und S10-Update-Abnahme vorbehalten; nicht in ersten S10-Testbuild. |
| #522 HCI Replay | BLOCKED/REVIEW | FULL CI grün, aber unabhängiger Protokollreview ausstehend; dokumentierte Differenzen zu #515 (49 L2CAP/5 ATT) nicht reconciled; keine G2–G4-/FIT-Download-Evidenz. |
| #499 FIT candidates | NEEDS FIX/REVIEW | FIT 269/227 nur ReviewCandidates; keine Persistenz. Alle Checks grün, aber GitHub API meldet `mergeable=false`, `mergeable_state=dirty`; kollidiert genau mit `FitHistoricalObservations.kt` und `FitHistoricalObservationsTest.kt`. Lokale vier-Datei-Änderung ebenfalls berührt beide; kein Merge oder Datei-Übernahme ohne koordinierte Konfliktlösung. |

## #477 / Main / CI
- Main enthält PR #539 und den kanonischen `FitHistoryReader`: `unified_measurements`, `MeasurementOrigin.BAND`, Availability via kanonische Quelle.
- `FitHistoryViewTest.availabilityAndLatestWorkWhenOnlyTheActiveGenerationHoldsTheImport` simuliert den S10-Zustand mit leerer Legacy-Provenienz und prüft 70/75 bpm, `available=true`, neueste Messung sowie den erklärten Leerzustand im Zeitfenster. `fitCardIsSeparateAndNamesTheBandClock` prüft getrennte FIT-/Live-Karten und Zeitachsen. Das deckt die historische #477-Regression im Quellstand ab; keine neue S10-Geräteabnahme wird behauptet.
- Run `45126611870` ist abgeschlossen SUCCESS, `main` SHA `f5784e0a`, Titel `Android CI · main · CLOUD · FULL`, Run 1934. Build/Unit/Lint/APK und API-35-Emulatorjobs beide grün. `origin/main` ist inzwischen `9b008d0a` (nur Dokumentationscommits nach `f5784e0a`); dieser Run ist nicht auf exaktem aktuellem SHA und kein S10-Installationsnachweis.
- CI-Agent: bitte melden, ob Run 1934 den signierten Build samt APK-Hash/Signatur geprüft und gesichert hat; für Releasekandidat SHA `9b008d0a` bitte exakten FULL-Lauf/Artefaktnachweis koordinieren. APK 1931/DB v8 bleibt unverändert als Referenz.

## Lokaler Quick-Gate
- `./tools/ci-local.sh --bootstrap --quick`: PASS, Exit 0, vorgesehene JDK 21 / Gradle 9.6.0.
- Python Regression suite: 112 Tests, OK. `:app:testDebugUnitTest`, Debug- und R8-Release-APK erfolgreich; Gradle meldete Unit-Testtask `FROM-CACHE`.
- APK-Diagnose: lokaler Build meldet `de.pulselibre`, lokales Standard-`versionCode=1`, etablierter Zertifikat-Fingerprint `36e7f3b6…`; dies ist **kein** S10-Artefakt und nicht die angeforderte Versionskennung.
- Quick führt keinen Emulator-/Instrumentationstest aus. Die unter Python-Tests ausgegebenen GUI-Zeilen stammen aus `tools/test_s25_acceptance.py` mit `FakeAdb`/`TESTSERIAL`; keine ADB-/S25-/Hardwareaktion.
- Voriger direkter `gradle :app:testDebugUnitTest`-Versuch mit systemweitem Gradle 4.4.1 scheiterte vor Projektkonfiguration („Project app not found“); kein Codefehler. Der Repo-Bootstrap löste die Toolchainabweichung.

## Übergaben
- GUI-Agent: gezielte Abnahmebitte für Datenvertrag #536, #477-Testabdeckung und PR #542 liegt in `2026-11-23_0702_opencode-an-gui-agent_fit-ui-contract-477-acceptance.md`.
- CI-Agent: Watchdog-PID/Sessionstatus und exakter Release-CI-Commit sind angefragt; keinen laufenden Run abbrechen.
- Keine Änderung an Produktcode, keine Merges, kein Push, keine Hardware-/BLE-/S25-Aktion.
