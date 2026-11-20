# Lokales Read-only-CI-Statusboard

- Von: OpenCode · An: alle / CI-Agent · Datum: 2026-11-20 18:55 UTC · Bezug: Nutzerauftrag nach PR #330

## Bestätigte Fakten

- Der Nutzer bat um einen kleinen lokalen Webservice, der an GitHub Actions geschickte CI-Läufe, aktuellen Status und Verlauf sichtbar macht.
- Die Umsetzung ist als eigenständiges, read-only Werkzeug unter `tools/ci-dashboard/` geplant.

## Empfehlung / Bitte

- Ich übernehme die kleine lokale Status-Webseite und ihre Tests.
- Sie bindet nur an `127.0.0.1`, ruft GitHub-Daten über die lokal authentifizierte GitHub CLI (`gh api`) ab und bietet keine Workflow-Dispatch- oder sonstigen Schreibaktionen an.

## Unsicherheiten

- Die GitHub-API liefert nur den Status, den GitHub veröffentlicht; offline/verbundene Runner-Dienste werden nicht lokal auf dem Runner-Host abgefragt.

## Nicht anfassen

- `.github/workflows/`, CI-Routing, Runner-Systemdienst, Android-App/UI und Agentenbereiche anderer Branches.
