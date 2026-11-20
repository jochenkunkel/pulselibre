# Runner-Übergabepaket und aktueller Betriebsstatus

- Von: OpenCode · An: alle / CI-Agent · Datum: 2026-11-20 20:10 UTC · Bezug: Nutzerauftrag „alles von heute dokumentieren und paketieren“

## Bestätigte Fakten

- PR #330 für direktes LOCAL-Routing ist in `main` integriert (Merge `61a11dbe`).
- Main-FULL [44803534788](https://github.com/jochenkunkel/pulselibre/actions/runs/44803534788) auf `534b4669` war vollständig erfolgreich; Build- und API-35-Emulatorjobs verwendeten den Self-hosted Runner.
- Dashboard-Commit `5307a53` hat 94 Python-Tests und den echten CLOUD+FULL-Lauf [44803609839](https://github.com/jochenkunkel/pulselibre/actions/runs/44803609839) bestanden; PR #331 ist offen und noch nicht in `main`.
- Neueste Abfrage: GitHub meldet Repository-Sichtbarkeit `public`, Runner offline/idle und lokalen systemd-Dienst inaktiv. `PULSELIBRE_LOCAL_RUNNER_ENABLED=true`, PR-LOCAL bleibt `false`.

## Empfehlung / Bitte

- Ich dokumentiere den Wiederaufbau auf einem dedizierten Linux-Runner in `docs/ci/LOCAL_RUNNER_MIGRATION_PACK_2026-11-20.md`.
- Runner bis zur Eigentümerentscheidung zur Repository-Sichtbarkeit offline lassen. Keine Installation mit `--allow-public`, keine PR-/Fork-Jobs auf LOCAL.
- Ich aktualisiere nur den datierten Runner-Statusabschnitt in `docs/ci/HYBRID_CI.md`, weil er einen älteren Snapshot „privat/online“ als aktuellen Betrieb ausweist. Bitte nicht denselben Statusabschnitt parallel bearbeiten; sonst kurz abstimmen.

## Unsicherheiten

- Es ist nicht bestätigt, ob die aktuelle öffentliche Sichtbarkeit beabsichtigt oder ein Rückschritt gegenüber der früheren Privatsphäre-Anforderung ist.
- CLOUD+FULL von #331 ist erfolgreich, aber PR #331 muss nach Update auf aktuellen `main` mit FULL am endgültigen Head erneut geprüft werden.

## Nicht anfassen

- Keine Workflow-/Runner-Variablen, Repository-Sichtbarkeit, Runner-Registrierung, Secrets oder Dienste werden in diesem Dokumentationsschritt geändert.
