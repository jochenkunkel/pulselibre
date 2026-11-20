# Hybride CI: LOCAL (Self-hosted Runner) und CLOUD (GitHub-hosted)

Stand: 2026-11-20 · Workflow: `.github/workflows/android.yml` · Lokale CI: `tools/ci-local.sh`

PulsLibre kann jeden Android-CI-Lauf entweder auf dem eigenen Entwicklungsrechner
(**LOCAL**, GitHub Actions Self-hosted Runner) oder auf einem GitHub-hosted Runner
(**CLOUD**, `ubuntu-latest`) ausführen. Beide Wege verwenden **dieselbe**
Pipeline-Implementierung (`tools/ci/run-ci.sh`). Es gibt keine zweite, separat
gepflegte CI-Definition.

## 1. Architektur

```text
                    GitHub Actions
                          │
                  prepare (ubuntu-latest)
                  ermittelt Runner + Umfang
                          │
             ┌────────────┴────────────┐
             │                         │
          LOCAL                      CLOUD
   [self-hosted, linux, x64,      ubuntu-latest
          pulselibre]
             │                         │
             └────────────┬────────────┘
                          ▼
                 tools/ci/run-ci.sh
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
   python-tests        build              verify
   (Offline-HCI)   Unit-Tests,        Lint + Test-APK
                   Debug-APK,        (nur FULL)
                   R8-Release,
                   JVM-Coverage
                          │
                          ▼
                    diagnostics
              APK-/Signatur-/R8-Prüfung
                          │
                          ▼
                     emulator (nur FULL)
              API-35-Instrumentationstests
                          │
                          ▼
                  GitHub Artifacts
```

Der Job `prepare` läuft bewusst immer auf GitHub-hosted (wenige Sekunden). So
wird die Zielumgebung auch dann korrekt bestimmt, wenn der lokale Runner offline
ist. Ein LOCAL-Job wartet dann in der Warteschlange, statt unbemerkt in die Cloud
auszuweichen.

## 2. Bedienung in GitHub

**Actions → Android CI → Run workflow**

| Eingabe | Werte | Bedeutung |
| --- | --- | --- |
| **Ausführungsort** (`run_environment`) | `LOCAL`, `CLOUD` | Wo die Tests laufen |
| **Testumfang** (`test_scope`) | `QUICK`, `FULL` | Wie viel geprüft wird |

| Umgebung | Umfang | Ergebnis |
| --- | --- | --- |
| LOCAL | QUICK | Build, R8-Release, Unit-Tests, APK-Diagnose auf dem eigenen Rechner |
| LOCAL | FULL | zusätzlich Lint, Instrumentation-Test-APK, API-35-Emulator |
| CLOUD | QUICK | dasselbe auf `ubuntu-latest` |
| CLOUD | FULL | dasselbe inklusive Emulator auf `ubuntu-latest` |

Die tatsächlich verwendete Umgebung steht im Job-Namen
(`… · LOCAL · FULL`), im Laufprotokoll des `prepare`-Jobs und in der
Job-Zusammenfassung („Android CI – Ausführungsziel“). Der Laufname zeigt den
**angeforderten** Wert; bei automatischen Läufen steht dort `AUTO`.

### Standardwerte bei automatischen Läufen

| Auslöser | Angefordert | Umfang | Tatsächlich |
| --- | --- | --- | --- |
| Push auf `main` | LOCAL | FULL | LOCAL, sobald freigegeben – sonst CLOUD |
| Pull Request | LOCAL | FULL | CLOUD, solange PRs nicht freigegeben sind; Forks immer CLOUD |
| Manuell | Eingabe | Eingabe | Eingabe, LOCAL nur wenn freigegeben |

## 3. Freigabe-Schalter (Repository-Variablen)

Die lokale Ausführung ist absichtlich **standardmäßig aus**. Solange kein Runner
registriert ist, würde ein LOCAL-Job endlos warten. Deshalb gibt es zwei
Repository-Variablen (Settings → Secrets and variables → Actions → Variables):

| Variable | Wirkung | Empfohlener Wert |
| --- | --- | --- |
| `PULSELIBRE_LOCAL_RUNNER_ENABLED` | `true` = LOCAL wird wirklich auf dem Self-hosted Runner ausgeführt. Fehlt sie oder ist sie `false`, fällt LOCAL sichtbar auf CLOUD zurück. | `false`, bis der Runner registriert und geprüft ist |
| `PULSELIBRE_PR_ON_LOCAL_ENABLED` | `true` = Pull Requests dürfen auf dem Self-hosted Runner laufen. | `false`, solange das Repository öffentlich ist |

Der Rückfall auf CLOUD ist **kein stilles Verhalten**: Er wird als Warnung im
Lauf protokolliert, in der Zusammenfassung begründet und ist hier dokumentiert.
Ein Offline-Runner führt dagegen **nicht** zu einem Cloud-Lauf, sondern zu einem
wartenden Job.

Setzen per CLI:

```bash
gh variable set PULSELIBRE_LOCAL_RUNNER_ENABLED --body true  -R jochenkunkel/pulselibre
gh variable set PULSELIBRE_PR_ON_LOCAL_ENABLED   --body false -R jochenkunkel/pulselibre
```

## 4. Lokale CI ohne GitHub

```bash
./tools/ci-local.sh --quick     # Standard: Build, R8-Release, Unit-Tests, APK-Diagnose
./tools/ci-local.sh --full      # zusätzlich Lint, Test-APK, API-35-Emulator
./tools/ci-local.sh --full --keep-emulator
./tools/ci-local.sh --bootstrap # JDK 21 + Gradle 9.6.0 nach ~/.local/share/pulselibre-ci
```

Eigenschaften:

- **Kein GitHub-Token** und keine Netzwerkverbindung zu GitHub nötig (nur Maven/
  Google-Repositories für Abhängigkeiten, sofern nicht gecacht).
- **Exit-Code 0** nur bei vollständigem Erfolg; die erste fehlgeschlagene Phase
  bricht mit deren Exit-Code ab.
- **Gleiche Phasen** wie in Actions (`preflight`, `python-tests`, `build`,
  `verify`, `diagnostics`, `emulator`).
- **Berichte** unter `build/ci-reports/` (gitignoriert), Gradle-Berichte wie gewohnt
  unter `app/build/reports/`.
- **Voraussetzungen:** JDK 21, Gradle 9.6.x, Android SDK mit `platforms;android-37.0`,
  `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`, Python 3 mit
  PyYAML (`python3-yaml`) für die Workflow- und Regressionsprüfungen; für `--full`
  zusätzlich Emulator, System-Image `android-35;google_apis;x86_64` und `/dev/kvm`.

`tools/ci/bootstrap-toolchain.sh` installiert JDK und Gradle benutzereigen und
prüft SHA-256. `tools/ci/ensure-android-sdk.sh --install` ergänzt fehlende
SDK-Pakete.

## 5. Was die Phasen tun

| Phase | Inhalt |
| --- | --- |
| `preflight` | JDK 21, Gradle ≥ 9.6, Android SDK, Build-Tools, freier Speicher; bei FULL: KVM |
| `python-tests` | `python3 -m unittest discover -s tools -p 'test_*.py' -v` (Validatoren, Workflow-Struktur, Runner-Installer-Sicherheit) |
| `build` | `:app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:createDebugUnitTestCoverageReport -PpulselibreVersionCode=<n>` |
| `verify` (FULL) | `:app:lintDebug :app:assembleDebugAndroidTest` |
| `diagnostics` | `apksigner verify --print-certs`, `aapt dump badging`, R8-Mapping vorhanden, Release nicht debuggable, Debug-/Release-Zertifikat identisch (wenn signiert) |
| `emulator` (FULL) | `:app:connectedDebugAndroidTest`, danach Coverage-Gate `.github/scripts/verify-instrumentation-coverage.sh` |

QUICK lässt ausschließlich Lint, Test-APK und Emulator weg – diese Prüfungen
gehören per Definition zu FULL. Es wird kein Test „übersprungen“, um ein
Ergebnis zu erzeugen.

`python-tests` prüft vorab, dass PyYAML verfügbar ist, und parst damit
`tools/test_workflow_structure.py` die Workflow-Dateien. Dieser Test
sichert die Fehler ab, die beim Umbau real aufgetreten sind:

- `runs-on` wird ausschließlich über `fromJSON(needs.prepare.outputs.runs_on)`
  aufgelöst; ein bloßer String wie `ubuntu-latest` ist kein gültiges JSON und
  lässt GitHub den Job verwerfen.
- Job-Namen enthalten keine `${{ ... }}`-Ausdrücke (GitHub wertet `needs` dort
  nicht aus).
- Alle Actions sind auf einen vollständigen Commit-SHA gepinnt.
- `actions/checkout` setzt `persist-credentials: false`; es gibt kein
  `permissions: write-all`.

`tools/test_runner_installer.py` verwendet isolierte Command-Stubs, um den
Runner-Installer ohne Netzwerk, echten Token, Service- oder Systemänderung zu
prüfen: tokenloser System-/User-Dry-Run sowie Ablehnung eines echten
Installationsversuchs bei öffentlichem Repository, bevor Seiteneffekte
auftreten.

## 6. APK-Signierung

Die bestehenden Signaturschlüssel werden **nicht** ersetzt oder neu erzeugt.
Beide Umgebungen verwenden dieselben vier Repository-Secrets:

`PULSELIBRE_SIGNING_KEY_B64`, `PULSELIBRE_SIGNING_STORE_PASSWORD`,
`PULSELIBRE_SIGNING_KEY_ALIAS`, `PULSELIBRE_SIGNING_KEY_PASSWORD`.

- **CLOUD/LOCAL-Runner:** Der Workflow schreibt den Keystore nach `$RUNNER_TEMP`
  (wird vom Runner zwischen Jobs verworfen) und exportiert die Variablen über
  `GITHUB_ENV`.
- **Lokale Läufe:** `tools/ci/signing.sh` liest optional
  `~/.config/pulselibre/ci-signing.env` (Rechte `600`, außerhalb des Repos).
  Bereitgestellt wird der **bestehende** Keystore als Base64 sowie Store-Passwort,
  Alias und Key-Passwort. Der Keystore wird für den Lauf temporär mit Modus `600`
  nach `$TMPDIR` dekodiert und nach Abschluss entfernt; die Originaldatei bleibt
  am sicheren Speicherort.

### Lokalen Keystore sicher bereitstellen

1. Den bereits vorhandenen `.jks`-/`.keystore`-Keystore und die dazugehörigen
   Zugangswerte aus dem eigenen sicheren Speicher/Passwortmanager verwenden.
   Keinen neuen Schlüssel erzeugen. Ein GitHub-Token kann die Signaturwerte
   **nicht** ersetzen; GitHub zeigt gespeicherte Secret-Werte nicht wieder an.
2. Das Konfigurationsverzeichnis nur für das eigene Konto lesbar machen und
   den Keystore **direkt in die Datei außerhalb des Repositorys** Base64-kodieren
   (Beispiel für Linux; den Platzhalter durch den Pfad zur vorhandenen Datei
   ersetzen):

   ```bash
   install -d -m 700 "$HOME/.config/pulselibre"
   chmod 700 "$HOME/.config/pulselibre"
   umask 077
   signing_env="$HOME/.config/pulselibre/ci-signing.env"
   touch "$signing_env"
   chmod 600 "$signing_env"
   {
     printf 'PULSELIBRE_SIGNING_KEY_B64='
     base64 < "/sicherer/pfad/zum/vorhandenen-keystore.jks" | tr -d '\n'
     printf '\n'
   } > "$signing_env"
   ```

3. Die Datei mit einem lokalen Editor ergänzen:

   ```bash
   "${EDITOR:-nano}" "$HOME/.config/pulselibre/ci-signing.env"
   ```

   ```bash
   PULSELIBRE_SIGNING_STORE_PASSWORD='bestehendes-Store-Passwort'
   PULSELIBRE_SIGNING_KEY_ALIAS='bestehender-Alias'
   PULSELIBRE_SIGNING_KEY_PASSWORD='bestehendes-Key-Passwort'
   ```

   Shell-kompatibel quoten. Keystore/Base64 und Passwörter weder in Chat,
   Kommandozeilenargumente, Shell-History noch Repository-Dateien kopieren.
   Base64 ist **keine Verschlüsselung**; der Schutz erfolgt durch Verzeichnis-
   und Dateirechte.
4. Rechte setzen und die lokale CI starten:

   ```bash
   chmod 600 "$HOME/.config/pulselibre/ci-signing.env"
   ./tools/ci-local.sh --quick
   ```

   Ohne Signing-Datei laufen Tests weiter, aber es entsteht **keine signierte
   Update-APK** (`APK_SIGNING_READY=false`). Mit gültigen Werten zeigt die
   Diagnose `APK_SIGNING_READY=true`; die Release-Datei liegt dann unter
   `app/build/outputs/apk/release/app-release.apk`.

Eine lokal signierte APK ist nicht automatisch ein freigegebenes Update. Vor
Installation oder Verteilung müssen Signaturfingerprint **und** Versionscode
zur installierten/verteilten App passen. Insbesondere erzeugt ein lokaler
Standardlauf keinen garantierten fortlaufenden Release-Versionscode.
- Pull Requests erhalten keine Signatur-Secrets.

Ein Signaturvergleich zwischen Debug- und Release-APK findet in `diagnostics`
statt. Ein Abgleich mit einer bereits veröffentlichten APK ist nur mit dem
Original-Keystore möglich und bleibt offen, solange er lokal nicht vorliegt.

## 7. Sicherheit

**Grundregel: Solange das Repository öffentlich ist, läuft kein fremder Code auf
dem Self-hosted Runner.**

| Fall | Runner | Begründung |
| --- | --- | --- |
| Push auf `main` | LOCAL (wenn freigegeben) | Nur Personen mit Schreibrechten erzeugen solche Commits |
| PR aus dem eigenen Repository | CLOUD, bis `PULSELIBRE_PR_ON_LOCAL_ENABLED=true` | Review-Stand, Branch-Code ist nicht automatisch vertrauenswürdig |
| PR aus einem Fork | **immer CLOUD** | Fremder Code; Variablen können das nicht überschreiben |
| Manuell LOCAL | LOCAL (wenn freigegeben) | Ausdrückliche Entscheidung des Bedieners |

Weitere Maßnahmen:

- Der Runner läuft unter dem dedizierten Systembenutzer `ghrunner`
  (keine Login-Shell, kein sudo).
- Das Arbeitsverzeichnis `_work` ist **nicht** ephemer: keine persönlichen
  Zugangsdaten, keine Keystores und keine `~/.ssh`-Kopien dort ablegen.
- Signatur-Secrets werden nur in `$RUNNER_TEMP` materialisiert und nach dem Job
  verworfen.
- `persist-credentials: false` beim Checkout verhindert, dass das
  Workflow-Token im Git-Remote des Runners liegen bleibt.
- Ein Runner, der einmal fremden Code ausgeführt hat, gilt als kompromittiert.
  Nach einem Vorfall: Runner entfernen, neu registrieren, Secrets rotieren.

## 8. Runner installieren (vorbereitet, nicht automatisch ausgeführt)

Die Installation ist vorbereitet, die **Registrierung erfolgt erst nach der
Freigabe** (Repository privat oder bewusste Risikoakzeptanz).

```bash
# 1. Dedizierten Android-SDK für den Runner-Benutzer bereitstellen (optional)
sudo bash tools/runner/prepare-android-sdk.sh --target /opt/android-sdk --owner ghrunner

# 2. Runner installieren (Systemdienst, Benutzer ghrunner)
sudo bash tools/runner/install-runner.sh --system \
  --android-sdk /opt/android-sdk \
  --java-home /home/smiler/.local/share/pulselibre-ci/jdk-21

# 2a. Trockenlauf ohne root und ohne Registrierung (zeigt alle Befehle)
bash tools/runner/install-runner.sh --system --dry-run \
  --allow-public --android-sdk /opt/android-sdk

# 3. In GitHub aktivieren
gh variable set PULSELIBRE_LOCAL_RUNNER_ENABLED --body true -R jochenkunkel/pulselibre
```

Details:

- Label: `pulselibre` (zusätzlich zu den automatischen Labels `self-hosted`,
  `linux`, `x64`).
- Registrierungstoken: wird über `gh api` geholt oder per `--token-file`
  übergeben, nie protokolliert und nicht gespeichert.
- Prüfsumme: Ist eine `.sha256`-Datei im Release vorhanden, wird sie geprüft.
  Fehlt sie, zeigt das Skript den berechneten SHA-256 an und legt ihn in
  `.runner-archive.sha256` ab; mit `--sha256 <hex>` lässt er sich festnageln.
- Dienst: systemd, `Restart=always`, Start beim Booten.
- Umgebung für Jobs: `.env` im Runner-Stamm (`ANDROID_HOME`, `JAVA_HOME`).
- Das Skript verweigert die Registrierung bei öffentlichem Repository, außer
  `--allow-public` wird ausdrücklich gesetzt.
- `--dry-run` funktioniert für `--user` und `--system` auch ohne root: es wird
  nichts registriert und nichts nach `/opt` geschrieben; nur die Befehle werden
  angezeigt. Der Trockenlauf benötigt oder ruft **keinen Registrierungs-Token**
  ab. Damit lässt sich der Ablauf vor der Freigabe vollständig prüfen.

### KVM

Emulator-Tests brauchen `/dev/kvm`. Auf diesem Rechner ist das Gerät vorhanden
und für den Entwicklerbenutzer per ACL beschreibbar. Für den Runner-Benutzer:

```bash
sudo usermod -aG kvm ghrunner      # dauerhaft, empfohlen
# oder einmalig:
sudo chmod 666 /dev/kvm
```

`tools/ci/ensure-kvm.sh` prüft die Rechte und eskaliert nur, wenn nötig.

### Updates und Wartung

- Der Runner aktualisiert sich selbst über die offizielle Update-Logik. Nach
  einem Major-Update den Dienst neu starten:
  `sudo systemctl restart 'actions.runner.*'`.
- Neue Runner-Version manuell installieren:
  `sudo bash tools/runner/install-runner.sh --system --runner-version <x.y.z>`.
- Android SDK aktualisieren:
  `sudo -u ghrunner /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --update`.
- Logs: `journalctl -u 'actions.runner.*' -f`.

### Wiederherstellung nach Runner-Ausfall

1. `systemctl status 'actions.runner.*'` und `journalctl` prüfen.
2. Reicht ein Neustart nicht: `sudo bash tools/runner/uninstall-runner.sh --system`
   (entfernt **nicht** die GitHub-Registrierung), danach neu installieren.
3. Runner in GitHub unter Settings → Actions → Runners als offline markiert?
   Nach Neuinstallation mit `--replace` registrieren.
4. Bis dahin: manuell `CLOUD` wählen – oder LOCAL bewusst warten lassen.

## 9. Performance

- **Gradle-Cache:** `gradle/actions/setup-gradle` cached Abhängigkeiten und
  Build-Cache pro Runner; lokal nutzt Gradle `~/.gradle`.
- **Build-Cache:** `org.gradle.caching=true` ist aktiv; identische Tasks werden
  zwischen Läufen wiederverwendet.
- **Kein vollständiger Rebuild:** Gradle ist inkrementell; `--rerun-tasks` wird
  nirgends verwendet.
- **Parallele Läufe:** Die Concurrency-Gruppe enthält Ref und Umgebung, damit
  LOCAL und CLOUD desselben Branches parallel laufen können. Bei neuen Commits
  dürfen überholte PR-Läufe derselben PR-Gruppe abgebrochen werden. Push- und
  manuelle Läufe brechen dagegen keinen bereits laufenden Job ab. GitHub hält pro
  Gruppe maximal einen Pending-Lauf; bei weiteren Commits wird der Pending-Lauf
  durch den neuesten ersetzt. Damit endet ein aktiver Main-FULL-Lauf vollständig,
  danach wird der jüngste wartende Main-Stand geprüft. Offline-Policy-Tests
  simulieren dieses Verhalten; die echte Scheduler-Semantik wird zusätzlich in
  Actions anhand der Main-FULL-Runs beobachtet.
- **Emulator:** QUICK startet keinen Emulator. FULL nutzt einen Cold Boot ohne
  Snapshot (reproduzierbar) und deaktivierte Animationen.
- **Speicher:** `org.gradle.jvmargs=-Xmx2048m` bleibt bewusst konservativ, damit
  parallele Agenten nicht in Swapping geraten.

Optimierungen dürfen keine Tests entfernen. Der Umfang wird ausschließlich über
`QUICK`/`FULL` gesteuert und ist im Laufnamen sichtbar.

## 10. Repository privat schalten (vorbereitet, **nicht** ausgeführt)

Die Sichtbarkeit wird nur nach ausdrücklicher Freigabe geändert. Checkliste:

1. **Actions-Minuten:** Private Repos verbrauchen das Kontingent des Kontos;
   Self-hosted Runner sind kostenlos. Deshalb LOCAL als Standard für Push/PR.
2. **Secrets:** bleiben beim Wechsel erhalten; nach dem Wechsel einmal prüfen,
   dass alle vier Signatur-Secrets vorhanden sind.
3. **Runner-Gruppen:** Ein Repo-Runner ist nur für dieses Repository sichtbar.
   Für mehrere Repos eine Runner-Gruppe mit „Selected repositories“ verwenden.
4. **PR-Sicherheit:** Nach dem Wechsel sind PRs nur noch von Mitarbeitern
   möglich. `PULSELIBRE_PR_ON_LOCAL_ENABLED=true` erst dann setzen, wenn keine
   Fork-PRs mehr erwartet werden. Fork-PRs bleiben technisch immer auf CLOUD.
5. **Branch-Schutz für `main`:**
   - Force Push verhindern
   - Branch-Löschung verhindern
   - Statusprüfung „Build, Unit-Tests, Lint, APK …“ erforderlich
   - keine Pflicht-Reviews, solange parallele Agenten ohne Review mergen
   - „Require branches to be up to date“ aus lassen (verhindert Blockaden)
6. **Codex/OpenCode:** Zugriff erfolgt über persönliche Tokens/SSH-Keys, nicht
   über den Runner. Nach dem Wechsel `gh auth status` und SSH-Remote prüfen.

## 11. Troubleshooting

| Symptom | Ursache | Lösung |
| --- | --- | --- |
| Job fehlt im Lauf / `Error when evaluating 'runs-on'` | `runs_on` ist kein gültiges JSON | `tools/ci/resolve-target.sh` muss ein JSON-Array liefern (`["ubuntu-latest"]`, nicht `ubuntu-latest`); Tests `tools/test_ci_target.py` und `tools/test_workflow_structure.py` |
| LOCAL-Job bleibt „Queued“ | Runner offline oder Label falsch | `systemctl status 'actions.runner.*'`, Label `pulselibre` prüfen |
| Lauf zeigt `CLOUD` trotz LOCAL | `PULSELIBRE_LOCAL_RUNNER_ENABLED` fehlt/`false` | Variable auf `true` setzen |
| `JDK 21 nicht gefunden` | nur JDK 17 installiert | `tools/ci/bootstrap-toolchain.sh` |
| `Gradle 9.6+ nicht gefunden` | System-Gradle zu alt | `tools/ci/bootstrap-toolchain.sh` |
| `KVM nicht verfügbar` | Benutzer nicht in `kvm` | `usermod -aG kvm <user>` |
| Emulator bootet nicht | zu wenig RAM/kein KVM | QUICK nutzen; FULL nur mit KVM |
| `APK_SIGNING_READY=false` | Secrets/Env-Datei fehlen | Secrets prüfen, `~/.config/pulselibre/ci-signing.env` |
| Signatur weicht ab | falscher Keystore | **nicht** neu erzeugen; Original-Keystore einspielen |
| `sdkmanager` fehlt | cmdline-tools fehlen | `tools/runner/prepare-android-sdk.sh` |

## 12. Testnachweise

Siehe Abschnitt „Testmatrix“ in [CI_TEST_STRATEGY.md](../CI_TEST_STRATEGY.md) für
die dokumentierten Ergebnisse. Nicht ausführbare Tests (z. B. GitHub-Läufe ohne
registrierten Runner oder ohne Keystore) bleiben dort ausdrücklich offen.
