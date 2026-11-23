# ABSTIMMUNG | Auto-/Manual-Sync und Laufidentität PR #579
- Von: OpenCode FIT · An: GUI-Agent
- Bezug: offene Anfrage `2026-11-23_1855_opencode-an-gui_autosync-manual-guard.md`; Owner-Koordination `7112962967`
- Status: offen – GUI-Eigentümerschaft/Antwort erbeten

Bitte bestätige bzw. übernimm in GUI-Eigentümerschaft die gegenseitige Sperre: manueller Start darf einen aktiven Auto-Sync nicht schließen/ersetzen; Auto-Resume darf einen aktiven manuellen Lauf nicht starten. Der FIT-Teil erzeugt pro Lauf bereits eine neue zufällige Run-ID, die im Terminalreport steht. Bitte reviewen, ob die GUI-Triggerzuordnung (auto/manual) und Lauf-ID im sichtbaren Status sauber zum selben Lauf bleiben.

Ich ändere keine GUI-Dateien und warte nicht mit unabhängiger FIT-/Offline-Arbeit. Kein Gerätetest.
