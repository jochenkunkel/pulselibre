# Übernahme: Direkte LOCAL-Zustellung ohne Hosted-Prepare

- Von: OpenCode · An: alle · Datum: 2026-11-20 17:15 UTC · Bezug: #281, `.github/workflows/android.yml`, Dispatch 44780439999
- Status: erledigt

## Bestätigte Fakten
- Der bestehende `prepare`-Job lief auf `ubuntu-latest` und musste fertig werden, bevor `build`/`emulator` ihren Runner aus Outputs wählen konnten.
- Dispatch 44780439999 wurde durch GitHub Billing blockiert; `runner_id=0`. Eine Zustellung an `smurl-pulselibre` ist nicht erfolgt.
- Der Runner ist laut Issue-Nachweis online; `PULSELIBRE_LOCAL_RUNNER_ENABLED=true`, PR-Schalter bleibt `false`.
- Der aktive Issue-#321-Concurrency-Fix ist abgeschlossen und gemergt; diese Änderung nimmt keine Concurrency-Änderungen vor.

## Empfehlung / Bitte
- Ich übernehme den Routing-Umbau: vertrauenswürdige Main-Pushes und manuelle LOCAL-Dispatches wählen `self-hosted, linux, x64, pulselibre` direkt per `runs-on`; CLOUD und nicht freigegebene PR-Läufe bleiben hosted.
- PR-/Fork-Schutz wird im direkten Routingausdruck explizit erhalten; keine Änderung an `PULSELIBRE_PR_ON_LOCAL_ENABLED`.
- Bitte Workflow-/Runner-Routingänderungen in dieser Datei für die Dauer dieses PRs nicht parallel bearbeiten. Der bestehende Coverage-Vorschlag betrifft einen unabhängigen Emulator-Schritt und wird nicht angefasst.

## Unsicherheiten
- Community-Berichte und GitHub-Abrechnungsdokumentation belegen nicht, dass genau dieses Konto Self-hosted Jobs trotz Billing-Hold annimmt. Das kann erst ein echter Actions-Run auf `smurl-pulselibre` zeigen.
- Bei verbleibender Kontosperre können CLOUD-PR-Jobs weiterhin nicht starten.

## Nicht anfassen
- `tools/ci/run-ci.sh` / Emulator-Coverage-Vorschlag
- Repository-Sichtbarkeit, Billing-Einstellungen, Secrets und PR-Runner-Schalter

## Fortschritt
- LOCAL+QUICK 44782461165 (`f175bb9`) und 44783019997 (`7db6a6c`) wurden `smurl-pulselibre` (`runner_id=21`) zugewiesen, scheiterten aber an zwei umgebungsabhängigen Erwartungen im Installer-Dry-Run-Test (bestehende `.runner`-Konfiguration bzw. bereits vorhandenes Runner-Archiv).
- Die Testannahmen wurden korrigiert; lokale Python-Suite mit 90 Tests bestanden.
- LOCAL+QUICK [44783305618](https://github.com/jochenkunkel/pulselibre/actions/runs/44783305618), Commit `211c239`, lief auf `smurl-pulselibre` (`runner_id=21`) erfolgreich. Python-Regressionen, Build/Unit-Tests und APK-Diagnose grün; Artefakte `pulselibre-jvm-jacoco` und `pulselibre-signed-release-update-apk` vorhanden.
- LOCAL+FULL [44784374536](https://github.com/jochenkunkel/pulselibre/actions/runs/44784374536), Commit `80096b7`, ebenfalls erfolgreich auf `smurl-pulselibre` (`runner_id=21` für Build und Emulator); Lint, Instrumentation-APK, API-35-Emulator, Coverage und Artefakte bestanden.
- Direkte Self-hosted-Zustellung ist damit für QUICK und FULL dieses Repositorys belegt. Der Billing-Block betrifft weiterhin Hosted-CLOUD-Jobs: Main-Lauf 44785095335 auf altem Main-Workflow scheiterte am Hosted-Prepare mit `runner_id=0`.
- `PULSELIBRE_PR_ON_LOCAL_ENABLED=false` blieb unverändert; keine PR-/Fork-Ausführung auf LOCAL.
- Nächster Schritt: Routing-Fix per PR integrieren; CLOUD-/PR-Checks wegen Billing separat als blockiert dokumentieren.
