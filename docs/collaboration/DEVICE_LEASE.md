# Gemeinsame S10-/S25-Gerätesperre

`tools/s25_lease.py` ist aus Kompatibilitätsgründen weiterhin der CLI-Name; die Sperre gilt jetzt pro Gerät. Sie ist kooperativ und schützt ausschließlich Prozesse auf demselben Host/Dateisystem. Direkte, nicht umhüllte ADB-Aufrufe werden nicht technisch abgefangen.

## Nutzung

Vor jedem Zugriff Status prüfen; sämtliche Geräteaktionen einer Sitzung in **einem** Wrapper ausführen:

```sh
python3 tools/s25_lease.py status --device s10
python3 tools/s25_lease.py run --device s10 \
  --agent GUI --minutes 40 --task "30-min-watchdog" \
  --session-id "gui-20261123" --test-class "long-running" \
  --phase setup -- bash -lc './test-und-cleanup.sh'
python3 tools/s25_lease.py status --device s10
```

Für das S25 kann die alte Aufrufform ohne `--device` unverändert weiterverwendet werden; sie verwendet weiterhin denselben `~/.cache/pulselibre/s25-lease/`-Pfad. Der S10 verwendet `~/.cache/pulselibre/s10-lease/`. Beide Verzeichnisse werden standardmäßig über das Host-Benutzerverzeichnis aufgelöst und daher über mehrere Worktrees desselben Benutzers geteilt.

Bei Verwendung eines ADB-Serials als `--device` muss der bekannte Alias explizit auf dasselbe Gerät gemappt sein: `PULSELIBRE_S10_SERIAL` oder `PULSELIBRE_S25_SERIAL`. Die Seriennummer wird nicht im Lock-Status gespeichert oder ausgegeben. Empfohlen ist die kanonische, nicht sensible Auswahl `s10`/`s25`.

`--minutes` liefert nur `expected_free_utc` als Schätzung. Es gibt weder TTL noch automatisches Entsperren/Präemption. Der Lock bleibt erhalten, bis der Wrapper und alle Prozesse seiner gestarteten Prozessgruppe beendet und bereinigt sind. Bei SIGINT/SIGTERM/SIGHUP signalisiert der Wrapper der gesamten Prozessgruppe und wartet auf deren Ende; nötigenfalls folgt nach kurzer Schonfrist SIGKILL. Ein Status-JSON ohne gehaltenes `flock` ist veraltet und beansprucht das Gerät nicht.

Zusätzliche Felder (`--task`, `--session-id`, `--test-class`, `--phase`) dienen der lokalen Übergabe und sind nicht als Sicherheitsgrenze gedacht. Keine privaten Gerätekennungen oder Tokens als Metadaten verwenden.

## Verbleibende Schutzgrenze

`flock` kann nur kooperierende Prozesse auf einem gemeinsamen Host/Dateisystem serialisieren. Andere Hosts benötigen zusätzlich koordinierte Gerätevergabe; direkte ADB-Aufrufe außerhalb des Wrappers bleiben möglich und müssen in Test-/CI-Abläufen vermieden werden. Physische Instrumentationstests bleiben Emulatoraufgabe.

Die bestehenden Offline-/Geräte-Tools `tools/s25_acceptance.py` und `tools/battery_baseline.py` erwerben den Lease nicht selbst. Für ein physisches Gerät muss ihr gesamter Aufruf deshalb innerhalb des Wrappers liegen, z. B.:

```sh
python3 tools/s25_lease.py run --device s10 --agent GUI --minutes 40 -- \
  python3 tools/s25_acceptance.py --serial "$PULSELIBRE_S10_SERIAL" --out-dir /tmp/s10-acceptance
```

Die Testausgabe mit Screenshots/Gesundheitswerten muss außerhalb des Git-Repositories bleiben. `tools/ci/emulator.sh` ist für den Emulator; die CI-Serial-Prüfung verhindert, dass Android-Instrumentation auf einem sichtbaren physischen Gerät läuft. Diese technischen Schranken ersetzen die korrekte Lease-Nutzung für manuelle Hardware-Skripte nicht.
