# START – S10 Verlauf und Live-Aufzeichnung read-only diagnostizieren

- Zeit (UTC): 2026-11-22 07:44
- Von: OpenCode FIT/Protokoll
- An: GUI-Agent, FIT-Agent, ChatGPT, alle
- Bezug: Nutzerfrage nach fehlenden Verlaufspunkten und nicht funktionierendem Live-Modus nach dem einmaligen Build-1757-Produktlauf; Nutzer hat jetzt read-only Zugriff auf das Test-S10 ausdrücklich bestätigt.
- Umfang: Gerät unter gemeinsamer S25-Lease eindeutig als S10 identifizieren; installierte Version/Berechtigungen/Recorder- und App-Prozesszustand prüfen; wenn technisch und ohne Änderung möglich, bestehende lokale Datenbank/Log-Datei read-only hinsichtlich Metriken, Zeitstempel, Importstatus untersuchen. Kein Sync, keine BLE-Verbindung, kein Instrumentationstest.
- Ergebnisziel: S25-MAC-Hypothese mit Code und Gerätenachweis einordnen; zwischen fehlenden FIT-Herzfrequenzdaten/Zeitraum und Live-GATT-Verbindungsfehler unterscheiden.
- Nicht tun: keine UI-Aktion, die Scan, Download, Aufzeichnung oder BLE-Verbindung startet; keine App-Daten ändern/exportieren/ löschen, kein Reset/Unpairing. Rohdiagnosen bleiben lokal privat.
- Dateien: Aktivität, Agenten-Chat und ggf. redigierter Zusatz im bestehenden PR #422.
- Abhängigkeit: ADB-Zugriff auf SM-G970F unter atomarer Lease; bei fehlendem nichtdestruktivem DB-Zugriff den Blocker melden statt Backup-/Export-Workarounds.
