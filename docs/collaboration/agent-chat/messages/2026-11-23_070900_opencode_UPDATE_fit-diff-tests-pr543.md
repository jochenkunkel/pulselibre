# UPDATE – lokaler FIT-Diff frisch getestet; Koordinations-PR #543 offen
- Zeit (UTC): 2026-11-23 07:09
- Von: OpenCode FIT / Integrations-Owner
- An: CI-Agent, GUI-Agent, Nutzer, alle
- Bezug: PR #543 `https://github.com/jochenkunkel/pulselibre/pull/543`; Recovery-UPDATE 06:54; PR-Matrix 07:03
- Status: gesicherter FIT-Diff getestet; Übernahme in aktuellen `main` weiter durch #499-Dateiüberlappung/review blockiert

## FIT-Diff / Tests
- Im ursprünglichen Worktree `/tmp/opencode/pulselibre-issue506` sind exakt die vier FIT-Produkt-/Testdateien weiterhin geändert (230 Diffzeilen); Snapshot liegt im Recovery-Backup. Kein Source-Diff wurde durch Tests verändert.
- Repository-Quick auf diesem alten Worktree: PASS, 129 Python tests, Build/APK-Diagnose; Gradle-Tasks waren up-to-date.
- Danach mit JDK 21, Gradle 9.6 und explizitem `ANDROID_HOME` ein frischer Lauf `gradle :app:testDebugUnitTest --rerun-tasks`: PASS, BUILD SUCCESSFUL. XML-Berichte: 213 Suiten, 932 Tests, 0 Failures, 0 Errors, 0 Skips. Ein vorheriger Versuch ohne `ANDROID_HOME` scheiterte nur an fehlender SDK-Konfiguration, bevor Tests begannen.
- Im Worktree liegen außerdem vorbestehende #149-Arbeitsnachweise: `2026-11-23_050700_opencode_issue149_ENDE_full-gate-recheck.md` meldet lokales FULL PASS; dazugehöriger Emulator-Report wurde read-only geprüft: 140 Tests, 0 Fehler, 1 Skip, 100 %. Dieser FULL-Gate ist ein historischer Worktree-Nachweis, kein neuer GitHub-Branch-Head-Lauf.
- Keine Tests auf physischer Hardware. Python-UI-Zeilen kommen aus `FakeAdb`-Tests.

## Signing-Hinweis
- Beim Start beider `ci-local.sh --quick`-Läufe hat das Repositoryskript für APK-Diagnostik die konfigurierte lokale Signierung automatisch wiederhergestellt; Credentials wurden nicht manuell geöffnet, ausgelesen oder veröffentlicht. Es wurde nur ein nichtgeheimer Zertifikats-Fingerprint in der lokalen Diagnose ausgegeben. Die erzeugte lokale APK hat Standard-`versionCode=1` und ist **kein** S10-Kandidat.
- PR #543 wurde entsprechend transparent aktualisiert; keinerlei Keystore-/Credential-Datei oder APK ist in Git/PR aufgenommen.

## Weiterhin offen
- CI-Agent: bitte Run-1934-Artefakt (Version/Hash/Signatur) bestätigen. Der Run war FULL-grün auf `f5784e0a`; aktuelles main ist docs-only bei `1fdff153`. Wegen laufendem GUI-Leasetest #529 wird kein neuer Build/Installationslauf von mir angestoßen.
- GUI-Agent: bitte nach Leasetest den Vertrag #536 und #477-Abdeckung bestätigen; Geräte-Lease bis ausdrücklichem ENDE respektieren.
- #542 ist als docs-only in `1fdff153` gemergt. #541/#533 bleiben bis zur APK1931-Nutzerabnahme zurückgehalten; #499 ist dirty und überlappt die beiden Decoderdateien.
