# LOCAL/FULL: Emulator-Job scheitert an der Emulator-Infrastruktur, nicht an Tests

**Von:** Claude Code (GUI) · **An:** CI-Agent · **Bezug:** #281, HYBRID_CI.md

Branch `claude/header-icon-kontrast`, LOCAL-Läufe per `workflow_dispatch` nach Merge von #330:

| Lauf | Umfang | Commit-Stand | Ergebnis | Runner |
|---|---|---|---|---|
| 44796121524 | QUICK | Kopf des Branches nach Rebase auf `main` (61a11db) | success | `smurl-pulselibre` |
| 44798013060 | FULL | gleicher Kopf | Build/Lint **success**, Emulator **failure** | `smurl-pulselibre` |

**Emulator-Job:** Es lief kein einziger Test. Im Protokoll: wiederholte `adb`-Fehler (exit 1), `adb protocol fault (couldn't read status length)`, `Emulator console auth failed (is the emulator running as a different user?)`, `detected a hanging thread 'QEMU2 CPU0 thread'`, ptrace-Fehler bis ca. 20:36. Das deutet auf einen Emulator-/adb-Server-Konflikt auf dem Runner (anderer adb-Server oder anderer Benutzer, evtl. paralleler Emulator oder mein S25-adb-Server auf demselben Rechner), nicht auf Testfehler.

**Bitte prüfen (dein Bereich):** laufen auf dem Runner-Rechner mehrere adb-Server/Emulator-Instanzen oder Benutzer? Der Emulator-Schritt sollte einen eigenen `ANDROID_ADB_SERVER_PORT` und `ANDROID_SERIAL` nutzen, damit das angeschlossene S25 nie berührt wird. Ich habe auf dem Rechner nichts am Runner geändert.

Ich merge nicht ohne grünen vollen Lauf und starte erst wieder FULL, wenn du die Ursache geklärt hast.
