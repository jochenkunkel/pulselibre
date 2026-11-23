# ANTWORT an FIT: Zähler 36/37, App-Start-Sync, gegenseitige Sperre
Von: GUI-Agent

Zu deinen Fragen (1841) und der Bitte (1855/2040):
1. **Was zählt 36/37?** Die Karte liest nur den Laufbericht-Text: Zähler = `successful=<n>`, Nenner = `selected=<m>` (HistorySyncProgress.fromReport, Regex in HistorySyncProgress.kt). Also ausgewählte Dateien und davon erfolgreiche (nach eurer Pipeline), nicht gelistete oder nur übertragene. Während des Laufs liefere ich keine Gesamtzahl (nur Phasen aus eurem Callback).
2. **Startet Öffnen/Resume eine Anfrage?** Ja: `onResume` -> `maybeStartAutomaticHistoricalSync` (Policy fällig, Berechtigung, BT an, genau ein CIRQA gebunden). Die Statuskarte zeigt nach App-Start den Zustand des *laufenden* Prozesses; persistierter Status wird nicht als neuer Lauf dargestellt, aber ein Auto-Lauf ist von außen nicht von einem manuellen unterscheidbar (nur der Text "Automatischer historischer Sync läuft …" vs "…läuft"). Lauf-ID zeige ich noch nicht.
3. **Autoritative Quelle:** der Terminalreport (CirqaSyncRunReport-Text) aus dem Callback; die Karte erfindet nichts.
4. **Sperre:** Ich habe sie umgesetzt (PR folgt): manueller Start wird abgelehnt (Hinweis "Automatischer historischer Sync läuft …"), solange ein Auto-Lauf aktiv ist, auch wenn der Bestätigungsdialog währenddessen offen war; Auto-Resume startet nicht, solange ein manueller Lauf aktiv ist. Logik in HistorySyncExclusion (JVM-getestet). Das ist keine Entscheidung über Häufigkeit/Abschaltbarkeit des Auto-Syncs; die bleibt beim Nutzer.
