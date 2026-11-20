# Übergabepaket: PulsLibre LOCAL-CI-Runner

Stand: 2026-11-20, 20:10 UTC · Zweck: Die heute verifizierte CI-Konfiguration, Werkzeuge,
Testergebnisse und einen sicheren Wiederaufbau auf einem dedizierten Host
zusammenfassen. Diese Datei ergänzt die [Hybrid-CI-Betriebsdoku](HYBRID_CI.md)
und die [lokale CI-Statusseite](LOCAL_CI_DASHBOARD.md).

## Kurzstatus und Freigabesperre

- Routing für vertrauenswürdige Pushes auf `main` und manuelle LOCAL-Dispatches
  ist über PR #330 in `main` integriert (Merge `61a11dbe`).
- Der registrierte Self-hosted Runner war zuletzt **offline**; sein
  systemd-Dienst war `inactive`. Keine laufenden Jobs waren zu diesem Zeitpunkt
  zugewiesen. Nicht als verfügbar einplanen, bis Host und GitHub beide `online`
  melden.
- GitHub meldet derzeit Repository-Sichtbarkeit **`public`**. Das widerspricht
  der früheren privaten Betriebsannahme. Runner-Installation und -Start auf
  einem öffentlichen Repository sind daher **gesperrt**, bis der Eigentümer
  die Sichtbarkeit wieder auf `private` setzt oder eine gesonderte,
  informierte Sicherheitsentscheidung trifft. Keine Registrierung mit
  `--allow-public` und keine PR-/Fork-Jobs auf LOCAL.
- Repository-Variablen bei letzter Prüfung:
  `PULSELIBRE_LOCAL_RUNNER_ENABLED=true`,
  `PULSELIBRE_PR_ON_LOCAL_ENABLED=false`. Die zweite Variable muss `false`
  bleiben. Wegen der öffentlichen Sichtbarkeit sollte LOCAL nicht wieder
  aktiviert werden, bevor der Eigentümer die Sichtbarkeit geklärt hat. Die
  Variablen wurden für diesen Übergabeschritt **nicht** geändert.
- Die frühere Statusangabe in Abschnitt 8 von `HYBRID_CI.md` („privat/online“)
  beschreibt einen älteren Snapshot und ist durch diesen aktuellen Befund
  überholt.

## Heutige Umsetzung und echte CI-Nachweise

| Umfang | Commit / Lauf | Ergebnis und Ausführungsort |
| --- | --- | --- |
| Direktes LOCAL-Routing | PR #330, Merge `61a11dbe` | In `main`; beseitigt den vorgelagerten Hosted-Prepare-Job für vertrauenswürdige LOCAL-Pushes/Dispatches |
| LOCAL + QUICK | [44783305618](https://github.com/jochenkunkel/pulselibre/actions/runs/44783305618), `211c239` | Erfolgreich auf dem registrierten Self-hosted Runner |
| LOCAL + FULL | [44784374536](https://github.com/jochenkunkel/pulselibre/actions/runs/44784374536), `80096b7` | Build und API-35-Emulator erfolgreich auf dem registrierten Self-hosted Runner; Lint, Instrumentation und Coverage erfolgreich |
| Main nach Routing-Merge | [44795967033](https://github.com/jochenkunkel/pulselibre/actions/runs/44795967033), `61a11db` | FULL erfolgreich; Build und API-35-Emulator auf dem Self-hosted Runner |
| Neuerer Main-Head | [44803534788](https://github.com/jochenkunkel/pulselibre/actions/runs/44803534788), `534b4669` | FULL erfolgreich; Build und API-35-Emulator auf dem Self-hosted Runner |
| Lokales CI-Statusboard | PR #331, Branch-Commit `5307a53` | Read-only Webservice unter `tools/ci_dashboard/`; vollständige Python-Suite mit 94 Tests erfolgreich; `/health` und `/api/status` per lokalem HTTP-Smoke-Test geprüft |
| Statusboard CLOUD + FULL | [44803609839](https://github.com/jochenkunkel/pulselibre/actions/runs/44803609839), `5307a53` | Erfolgreich auf GitHub-hosted Runnern; Build- und Emulatorjob erfolgreich, Coverage-/APK-Artefakte vorhanden. **PR #331 ist noch offen; dieser Code ist noch nicht in `main`.** |

PR-Checks, die am Billing-Gate mit `runner_id=0` enden, sind keine ausgeführten
Builds. Der manuelle CLOUD+FULL-Lauf 44803609839 ist davon getrennt und ein
echter Hosted-Nachweis. QUICK ersetzt FULL nicht.

## Enthaltene, versionierte Komponenten

- `.github/workflows/android.yml`: Routing und getrennte Ausführungsort-/
  Testumfang-Eingaben.
- `tools/ci/run-ci.sh`, `tools/ci-local.sh`: dieselbe QUICK/FULL-Pipeline für
  GitHub Actions und direkte lokale Läufe.
- `tools/runner/install-runner.sh`: GitHub Actions Runner installieren und als
  systemd-Dienst betreiben.
- `tools/runner/prepare-android-sdk.sh`: dediziertes Android SDK inklusive
  API 35 Emulator/System-Image und benötigter Build-Tools vorbereiten.
- `tools/ci/bootstrap-toolchain.sh`: JDK 21 und Gradle 9.6.0 mit
  SHA-256-Prüfung benutzereigen bereitstellen.
- `tools/runner/uninstall-runner.sh`: Dienst/Dateien entfernen; GitHub-
  Registrierung bleibt ohne explizites `--deregister` erhalten.
- `tools/ci_dashboard/` und `docs/ci/LOCAL_CI_DASHBOARD.md`: lokales read-only
  Statusboard; nutzt vorhandene `gh`-Anmeldung, bindet an Loopback und
  dispatcht keine Workflows.

## Wiederaufbau auf einem dedizierten Runner-Host

### 0. Vorbedingungen und Sicherheitsfreigabe

1. Sauberen, dedizierten Linux-x86_64-Host bereitstellen. Kein Entwicklerkonto,
   persönliche SSH-Schlüssel, Keystores oder produktive Daten im Runner-
   Arbeitsverzeichnis verwenden.
2. Repository-Sichtbarkeit **vorher** prüfen:

   ```bash
   gh auth status
   gh api repos/jochenkunkel/pulselibre --jq '.visibility'
   ```

   Erwartung für den in dieser Doku beschriebenen sicheren Betrieb:
   `private`. Bei `public`, unbekanntem Ergebnis oder fehlender Berechtigung
   **abbrechen**; weder Runner installieren/registrieren noch
   `--allow-public` verwenden. Die GitHub-Variable für PR-LOCAL bleibt
   `false`.
3. Auf dem GitHub-Host unter **Settings → Actions → Runners** kontrollieren,
   dass es keinen anderen aktiven Runner mit denselben Labels gibt. Keine
   bestehende Registrierung blind ersetzen.
4. Systemanforderungen des aktuellen Android-Emulators, freier Speicher und
   KVM-Virtualisierung prüfen. `/dev/kvm` muss für den Dienstbenutzer
   beschreibbar sein. Der Emulator ist Teil von FULL.

### 1. Quellcode holen und Installation zuerst trocken prüfen

Auf dem neuen Host mit einem autorisierten Git-/GitHub-Zugang:

```bash
RUNNER_HOME="<dedicated-runner-home>"
ANDROID_SDK_ROOT="<android-sdk-directory>"
RUNNER_USER="<dedicated-runner-account>"
git clone git@github.com:jochenkunkel/pulselibre.git
cd pulselibre
gh auth status
gh api repos/jochenkunkel/pulselibre --jq '.visibility'
bash tools/runner/install-runner.sh --system --dry-run \
   --android-sdk "$ANDROID_SDK_ROOT" \
   --java-home "$RUNNER_HOME/.local/share/pulselibre-ci/jdk-21"
```

Der Dry-Run registriert nichts und schreibt keine Installationsdateien. Die
Installationsskripte und deren Tests nicht mit `--allow-public` überlisten.

### 2. Dedizierten Benutzer und Toolchain vorbereiten

Die folgenden Befehle sind ein Zielablauf für einen **frischen** Host. Vorher
prüfen, ob Benutzer/Pfade bereits existieren; keine bestehende Installation
überschreiben.

```bash
sudo useradd --system --create-home \
  --home-dir "$RUNNER_HOME" \
  --shell /usr/sbin/nologin "$RUNNER_USER"
sudo usermod -aG kvm "$RUNNER_USER"

# JDK 21 und Gradle 9.6.0 checksum-validiert im Home des Runner-Kontos installieren.
sudo -u "$RUNNER_USER" -H bash tools/ci/bootstrap-toolchain.sh
```

Vorbereitung des Android SDK mit dem bereitgestellten JDK:

```bash
sudo env \
  JAVA_HOME="$RUNNER_HOME/.local/share/pulselibre-ci/jdk-21" \
  PATH="$RUNNER_HOME/.local/share/pulselibre-ci/jdk-21/bin:/usr/bin:/bin" \
  bash tools/runner/prepare-android-sdk.sh \
    --target "$ANDROID_SDK_ROOT" \
    --owner "$RUNNER_USER"
```

Das SDK-Skript installiert `platform-tools`, `platforms;android-37.0`,
`platforms;android-35`, Build-Tools 35.0.0/36.0.0, Emulator und
`system-images;android-35;google_apis;x86_64`.

### 3. Runner registrieren und Dienst starten

Voraussetzung: Sichtbarkeit ist nochmals als privat bestätigt, eine
kurzlebige Repository-Registrierungsberechtigung ist verfügbar, und der
Zielrunner wurde in GitHub nicht versehentlich mit gleichem Namen registriert.
Registrierungstoken nie in Git, Shell-History, Prozessargumenten oder Logs
schreiben. `install-runner.sh` nimmt `--token-file` entgegen und protokolliert
den Tokeninhalt nicht; die Token-Datei muss geschützt und sofort gelöscht
werden. Der Installer erstellt/konfiguriert den systemd-Dienst.

Beispiel der nicht geheimen Installationsargumente:

```bash
sudo bash tools/runner/install-runner.sh --system \
  --repo jochenkunkel/pulselibre \
  --name "<dedizierter-runner-name>" \
  --labels pulselibre \
  --android-sdk "$ANDROID_SDK_ROOT" \
  --java-home "$RUNNER_HOME/.local/share/pulselibre-ci/jdk-21" \
  --token-file "<geschützte-kurzlebige-token-datei>"
```

Kurzlebigen Token auf einem sicher verwalteten Operator-Terminal nur im
RAM-Dateisystem `/run` ablegen und mit einem Trap löschen:

```bash
token_file="$(sudo mktemp /run/runner-registration-token.XXXXXX)"
trap 'sudo rm -f "$token_file"' EXIT
set -o pipefail
gh api -X POST repos/jochenkunkel/pulselibre/actions/runners/registration-token \
  --jq .token | sudo tee "$token_file" >/dev/null
sudo chmod 600 "$token_file"
sudo bash tools/runner/install-runner.sh --system \
  --repo jochenkunkel/pulselibre \
  --name "<dedizierter-runner-name>" \
  --labels pulselibre \
  --android-sdk "$ANDROID_SDK_ROOT" \
  --java-home "$RUNNER_HOME/.local/share/pulselibre-ci/jdk-21" \
  --token-file "$token_file"
sudo rm -f "$token_file"
trap - EXIT
```

Den Token nicht mit `set -x` ausführen oder ausgeben. Der Token wird durch
`gh api` erzeugt und nicht in dieses Repository geschrieben.

Nach Installation:

```bash
systemctl list-units --type=service 'actions.runner*'
systemctl status 'actions.runner.*'
journalctl -u 'actions.runner.*' -n 100 --no-pager
gh api repos/jochenkunkel/pulselibre/actions/runners \
  --jq '.runners[]|{id,name,status,busy,labels:[.labels[].name]}'
```

Erst nach bestätigter Sicherheit und echtem Runner-Smoke-Test die
Repository-Variable `PULSELIBRE_LOCAL_RUNNER_ENABLED=true` setzen. Die
PR-Variable bleibt `PULSELIBRE_PR_ON_LOCAL_ENABLED=false`.

### 4. Validierung nach Inbetriebnahme

Zuerst manuellen LOCAL+QUICK-Dispatch auf einem vertrauenswürdigen Branch
ausführen, dann auf demselben Commit FULL:

```bash
gh workflow run android.yml -R jochenkunkel/pulselibre \
  --ref <vertrauenswürdiger-branch> \
  -f run_environment=LOCAL -f test_scope=QUICK

# Erst nach erfolgreichem QUICK:
gh workflow run android.yml -R jochenkunkel/pulselibre \
  --ref <derselbe-branch-und-commit> \
  -f run_environment=LOCAL -f test_scope=FULL
```

Für jeden Lauf Run-ID und Jobzuweisung prüfen; erfolgreiche Runs allein
beweisen nicht, wo sie ausgeführt wurden:

```bash
gh run list -R jochenkunkel/pulselibre --workflow android.yml -L 10
gh api repos/jochenkunkel/pulselibre/actions/runs/<RUN_ID>/jobs \
  --jq '.jobs[]|{name,status,conclusion,runner_name,runner_id}'
```

Erwartung für LOCAL: `runner_name` entspricht dem konfigurierten Runner und
`runner_id` ist zugewiesen (nicht `0`). Für FULL müssen Build,
Unit-Tests, Lint, Instrumentation-Test-APK und API-35-Emulator erfolgreich
sein; Coverage-Artefakt kontrollieren. Instrumentation ausschließlich im
Emulator, niemals auf dem S25.

### 5. Sicher pausieren und später reaktivieren

Nur bei `busy=false` stoppen; nicht mitten in einem Job:

```bash
systemctl list-units --type=service 'actions.runner*'
# Exakten Unit-Namen aus der Ausgabe einsetzen, statt pauschal alle Runner zu stoppen:
UNIT='actions.runner.<owner>-<repo>.<runner-name>.service' # Platzhalter ersetzen
sudo systemctl stop "$UNIT"
systemctl is-active "$UNIT"
gh api repos/jochenkunkel/pulselibre/actions/runners \
  --jq '.runners[]|{name,status,busy}'
```

Der Dienststopp erhält die GitHub-Registrierung. **Nicht** `uninstall-runner.sh`
oder `--deregister` verwenden, wenn nur pausiert werden soll.

Später, nach erfüllter Sichtbarkeits-/Sicherheitsprüfung:

```bash
UNIT='actions.runner.<owner>-<repo>.<runner-name>.service' # denselben Unit-Namen einsetzen
sudo systemctl start "$UNIT"
sudo systemctl status "$UNIT"
```

### 6. Statusboard

Auf einem Rechner mit `gh auth status`:

```bash
python3 tools/ci_dashboard/server.py --repo jochenkunkel/pulselibre
```

Browser: `http://127.0.0.1:8765`. Das Board fragt nur GitHub-API-Daten ab,
zeigt Run-/Jobstatus, Runnerzuordnung und Verlauf und löst keine Workflows aus.
Es fragt nicht lokal `systemd` auf dem Runnerhost ab. Beenden mit `Ctrl+C`.

## Noch offene Punkte vor dem finalen Paketabschluss

- PR #331 enthält das Dashboard und hat für Commit `5307a53` einen echten
  CLOUD+FULL-Erfolg; die PR ist noch offen. Vor Merge muss der Branch auf
  aktuellen `main` gebracht und FULL für den endgültigen PR-Head erneut
  nachgewiesen werden.
- GitHub meldet derzeit `public`, obwohl ältere Runner-Dokumentation `private`
  annimmt. Eigentümerentscheidung ist nötig, bevor ein neuer lokaler Runner
  installiert oder der vorhandene Dienst wieder gestartet wird.
