# ENDE | All-data acceptance abgeschlossen – frische Historie weiter offen
- Von: OpenCode FIT · An: Projektleitung, GUI-/CI-Agent, ChatGPT
- Auftrag: freigegebene Punkte 1–5. Produktcode PR #567 ist bereits gemergt;
  FULL-CI Run 1989 auf Head `28b49fb1` grün, lokal ebenfalls FULL grün
  (990 JVM / 165 Emulator, beide Originalarchivtests bestanden).
- Geräteabnahme: signierter Build 1989 / unverändertes Zertifikat; Vorher- und
  Nachher-Backup privat erstellt und geprüft. Ein manueller Bestätigungstap
  nach Fix; Terminalstatus **unvollständig, 36/37 Dateien**. Kein manueller
  Retry. S10-Lease anschließend FREI.
- Wichtige Abweichung: Build 1989 löst beim App-Start automatisch Historien-
  Sync aus. GUI meldet während #529 einen automatischen 2/37-Lauf. In der
  FIT-Sitzung war nach App-Start ebenfalls ein frischer 36/37-Status sichtbar,
  bevor der eine manuelle Lauf bestätigt wurde. Damit wurde die strikte
  Ein-Request-Grenze über App-Starts nicht eingehalten. Kein weiterer Request
  wurde ausgelöst; vor einem nächsten Gerätetest Auto-Sync zuerst koordinieren/
  deaktivieren oder bewusst als einzigen Lauf verwenden.
- Aggregierter Vorher/Nachher-Befund (privat/offline): 38 Originale und
  642709 Bytes in beiden Backups; 0 neue Inhaltsversionen; Referenzen 40→74
  (+34); 28668 kanonische Beobachtungen unverändert, 0 hinzugefügt/entfernt.
  Das geprüfte rollende Drei-Stundenfenster 2026-11-23 15:13:47–18:17 UTC blieb
  leer: 0 neue Beobachtungen, 0 Review-Kandidaten, 0 FIT-Dateien mit jüngsten
  Kandidaten. 34 FIT-Originale/16056 Records geprüft; unbekannte Feldvorkommen
  bleiben in den Originalbytes erhalten. SHA-Prüfungen des Archivs ohne Fehler.
- Ergebnisgrenze: Freshness-Skip ist behoben, aber die jüngsten Daten kamen in
  diesem Lauf nicht nachweisbar an; eine von 37 Dateien fehlte. Vollständige
  Gerätehistorie und Ursachenhypothese bleiben offen. Kein Erfolg behauptet.
- Datenschutz: Backups und Zeitreihen blieben in privaten, mode-0600-Dateien;
  keine persönlichen Rohdaten/Screenshots wurden committet.
- Übergabe an GUI/CI: App-entry Auto-Sync kollidiert mit expliziter Testzählung;
  bitte vor nächstem kontrollierten Transfer ein eindeutiges Single-run-Gate
  koordinieren. #529-Recorderzustand laut GUI ENDE: Aufzeichnung aus, S10 frei.
