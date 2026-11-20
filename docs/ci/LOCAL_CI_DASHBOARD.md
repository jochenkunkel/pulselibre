# Lokales CI-Statusboard

Das read-only Statusboard zeigt GitHub-Actions-Läufe, aktuelle und wartende
Jobs, Runner-Zuordnung (Name/ID), Runnerpool-Status und den jüngsten Verlauf.
Es fragt GitHub über die **lokal installierte und angemeldete GitHub CLI** ab.
Es startet, ändert oder wiederholt keine Workflows.

## Start

Im Repository-Checkout:

```bash
gh auth status
python3 tools/ci_dashboard/server.py
```

Dann im Browser `http://127.0.0.1:8765` öffnen. Beenden mit `Ctrl+C`.

Optionen:

```bash
python3 tools/ci_dashboard/server.py --repo jochenkunkel/pulselibre --port 8765 --runs 25
```

Der Server akzeptiert ausschließlich Loopback-Adressen und kann nicht an eine
LAN-/öffentliche Adresse gebunden werden. Authentifizierungstoken werden weder
abgefragt noch gespeichert; `gh api` nutzt den vorhandenen GitHub-CLI-Login.
Das Dashboard ist GitHub-seitig read-only. Die API-Antwort wird maximal
12 Sekunden zwischengespeichert; automatische Browseraktualisierung erfolgt
alle 15 Sekunden. Jobdetails werden für aktive Läufe und die acht jüngsten
historischen Läufe geladen.

## Grenzen

- Der Runner-Pool zeigt, was GitHub über Repository-Runner meldet. Der Dienst
  fragt weder `systemd` noch den lokalen Runner-Host ab.
- Ein aktiver Run wird nur dann als tatsächlich lokal belegt, wenn der jeweilige
  Job `runner_name=smurl-pulselibre` und eine zugewiesene `runner_id` zeigt.
- Fehlgeschlagene Hosted-Jobs, die beim Billing-Gate mit `runner_id=0` enden,
  erscheinen als fehlgeschlagen und nicht als lokale Ausführung.
- Bei ungültigem/abgelaufenem `gh`-Login oder fehlendem Netz meldet die Seite
  einen Hinweis; sie zeigt keine GitHub-Tokens oder Job-Logs.
