# Offline-Härtung des historischen FIT-Pfads (18.11.2026)

Bezug: PR #67, Issues #2/#20/#22. Keine Geräteübertragung und keine DB-Änderung.

## Schnittstellenvertrag

- `CirqaConnectDirectoryTransition` modelliert Bootstrap→5030→5007→Directory-
  Claim. Ohne bewiesene innere Ergebnisabbildung stoppt der Produktionsdefault
  vor 5007. Die positive Ende-zu-Ende-Simulation injiziert **synthetische**
  Bedeutungen; diese sind keine Garmin-/CIRQA-Erfolgswerte.
- Im modellierten Pfad werden 5031-Dateitypen aus einem vorher bestätigten
  Cache verlangt. Es wird kein frischer 5031 modelliert/gesendet. Ein leerer
  Cache stoppt vor Verbindung statt Typen zu raten. Das ist die konservative
  Wahl aus Connects optionalem 5031-Schritt, keine allgemeine Pflichtreihenfolge.
- `CirqaBoundedLegacyFile` übernimmt nach separatem Claim genau einen
  5002→5004-Dateiabschnitt. Aufrufer übergibt den Dateiindex, die bestätigte
  Directory-Größe (für FIT verpflichtend) und dieselbe übergeordnete Deadline
  an jeden Abschnitt. Callbacktoken ist pro Dateiobjekt neu. Nicht in den
  aktiven BLE-Downloader eingebunden.
- 5002-Länge exakt 14 Bytes: korrelierte nichtkompakte äußere Status-0-Antwort,
  innerer Read-Status 0 plus u32LE-Dateigröße. Kein NOT_READY-Retry; Null,
  Überlauf, falsche Größe und Grenzüberschreitung sind terminal.
- Write-Abschluss und Antwort erforderlich, Reihenfolge beliebig; 5004 vor
  beiden Nachweisen wird abgelehnt. 5004 ist eine eingehende Requestnachricht,
  keine Antwort mit eingebettetem Dateiindex. Zuordnung geschieht über
  isolierte Sitzung, ausstehenden Dateiabschnitt und erwarteten Byteoffset.
  Ein beliebig neuer Dateiindex lässt sich **nicht** aus 5004 herauslesen.
- GFDI-CRC vor Bodyzugriff, maximal 32 KiB pro Datenframe, unkomprimierter
  Status 0, exakter nächster Offset, nichtleere Payload, kumulativer
  CRC-16/ARC-Akkumulator. CRC wird nur nach erfolgreicher Offset-/Größenprüfung
  übernommen. Bei jedem Fehler sofort terminal und Teilpuffer verwerfen;
  keine Neuannahme oder Duplikat-ACK-Schleife.
- Rückgabe eines validierten nächsten Offsets erlaubt einen künftigen
  ACK-Adapter, sendet aber selbst keine ACKs. Vollständigkeit allein reicht
  nicht: Directory strukturell strikt / FIT zusätzlich unabhängig per
  Header- und Datei-CRC prüfen. Kein SQLite-Import und kein Completion-Cursor.
- Grenzen: Directory 1 MiB, FIT je 4 MiB; bisherige Auswahlpolicy maximal
  vier Dateien (damit maximal 16 MiB). Der künftige vollständige Live-
  Koordinator muss Auswahl, Gesamtbytes und gemeinsame Deadline erzwingen.
  Neue Primitive ersetzt diesen noch nicht und darf nicht als fertige
  produktive Synchronisation bezeichnet werden.

## Directory- und Speicherungskorrekturen

Der strukturelle Parser erhält die bisherigen Analysemöglichkeiten, inklusive
duplizierter Zeilen; Maximalgröße ist jetzt begrenzt. Der neue
`parseForDownload` lehnt doppelte Dateiindizes ab, auch wenn deren Zeilen gleich
aussehen. Der bestehende gegatete Live-Reader benutzt diesen strengeren Parser.
FIT-Auswahl schließt Directoryindex 0 explizit aus, selbst bei behauptetem
FIT-Typ und READ-Flag. Keine zusätzlichen Typ-/Subtyp-Fallbacks eingeführt.

`CirqaVerifiedFitStore` bleibt ein privater, contentadressierter Speicher
ohne DB-/Dashboard-Schnittstelle. Der frühere ATOMIC_MOVE konnte auf Unix ein
zwischenzeitlich entstandenes Ziel trotz fehlendem REPLACE_EXISTING ersetzen.
Jetzt: temporäre Datei schreiben und fsync, dann atomarer **No-replace-Hardlink**
auf den SHA-256-Zielnamen. Bestehendes Ziel bytegenau vergleichen; niemals
überschreiben. Temporäre Datei anschließend entfernen. Bei fehlender
Hardlink-Unterstützung sicher fehlschlagen, kein Copy-Fallback. Verhalten auf
Androids privatem Zieldateisystem ist noch **nicht** hardwaregetestet; keine
Aussage zur Persistenz bei Stromverlust (Verzeichnis-fsync fehlt).

Der aktive ältere Transfer hat weiterhin seine eigene per-Session-Ablage;
der neue Speicherpfad ist ihm noch nicht untergeschoben. Diese Trennung
verhindert eine unbemerkte Änderung der vorigen Geräteversuche.

## Regressionen

- Synthetische Kette Bootstrap→5030→5007→Directory→Typauswahl→5002→5004→
  FIT-Hüllen-/Datei-CRC→Speicherung→Deduplizierung.
- Alle möglichen Zweichunk-Splits der synthetischen FIT-Datei prüfen
  kumulative CRC und exakte Offsets.
- Innerer/äußerer CRC-Fehler, ungültige FIT-CRC, Überlänge, falsche Größe,
  NOT_READY, Duplikat, verfrühte Daten, verspätete Callbacktokens, Timeout
  und Disconnect: keine vollständige Datei freigeben.
- Gemeinsame Parent-Deadline verhindert neues Fünf-Minuten-Budget pro Datei.
- Duplizierte Directory-Indizes, Trunkierung, Directory-Übergröße und
  reservierter Dateiindex 0 werden im Downloadpfad abgelehnt.
- Acht konkurrierende Storeinstanzen: genau eine atomare Veröffentlichung,
  sieben ALREADY_PRESENT, unveränderte Bytes, keine temporären Restdateien.

Im ersten Regressionlauf fielen zwei ältere Tests aus, die absichtlich
duplizierte Directoryzeilen für Analyse/Selector-Deduplizierung verwenden.
Der Strukturparser wurde deshalb nicht künstlich eingeschränkt; die strikte
Downloadprüfung ist jetzt explizit separat. Diese Korrektur erhält die
vorhandene Analysemantik, ohne die Downloadprüfung abzuschwächen.

**Evidenzgrenze:** alle Bytes sind synthetisch; kein CIRQA-FIT nachgewiesen.
Keine neue Gerätefreigabe, Installation oder BLE-Ausführung.

Lokaler Abschlusslauf: **458 JVM-Tests**, keine Fehler/Failures/Skipped,
Debug-APK, Lint und Instrumentation-APK-Build erfolgreich; **8 Python-Tests**
erfolgreich. Instrumentation-APK nur gebaut, nicht auf S25/Emulator gestartet.
GitHub-CI hat einen eigenen, manuell zu startenden Nachweis.
