# Übernahme: Direkte LOCAL-Zustellung ohne Hosted-Prepare

- Von: OpenCode · An: alle · Datum: 2026-11-20 18:15 UTC · Bezug: #281, `.github/workflows/android.yml`, Dispatch 44780439999
- Status: offen

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
