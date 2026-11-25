# P0 S10: begrenzter Full-Pull-Testplan (2026-11-24)

## Zweck und Freigabegrenze

Dieser Plan bereitet einen nutzerinitiierten Abruf über die bereits bekannte
Connect-kompatible Protobuf/FileAccess-Sync-Sequenz vor. Die Projektleitung hat
den kontrollierten Test mit bekannten, hinreichend charakterisierten Lese- und
Sync-Kommandos am aktuellen CIRQA-Testarmband ausdrücklich freigegeben.

Der Plan fügt keine Protokollkommandos hinzu und ändert keine Laufbudgets,
Leases oder Freigaben. Keine SetFlags-, Archive-, Delete-, Reset-, CLOSE_ALL-,
Legacy-Experiment- oder unbekannten Requests. Der Test ist nur nach
unabhängigem Review und grünem FULL/API-35-Gate auf dem exakten Branch-Head
auszuführen. Diese Voraussetzungen sind vor Ausführung erneut zu bestätigen.

## Einmaliger Lauf

1. **Abstimmung / Build**
   - CI-Agent bestätigt Dateigrenzen, prüft den Sync-/Queue-Testplan unabhängig
     und bestätigt das FULL/API-35-Ergebnis auf dem Branch-Head.
   - Einen korrekt signierten Build mit Versions-/Zertifikatskontinuität
     verwenden; keine App-Daten löschen und nicht deinstallieren.
   - Vor Testbeginn App-Build und laufende Geräteaufzeichnung/-jobs prüfen.
     Parallele BLE-/ADB-/UI-Zugriffe anderer Bearbeiter ausschließen.
2. **Lease / Sicherung**
   - Unmittelbar vor jeglichem S10-Zugriff `python3 tools/s25_lease.py status
     --device s10` prüfen. Nur wenn FREI: gesamten Aufbau, Lauf und Cleanup
     innerhalb eines einzelnen `run --device s10`-Wrappers halten.
   - Vor Sync das vorhandene lokale App-Backup privat exportieren und ZIP-
     Integrität prüfen. Keine Rohdatei, Hash, Seriennummer, UID, Token,
     Geräteadresse oder private Messreihe ins Git/PR schreiben.
   - `MainActivity.onResume()` kann einen automatischen Sync starten, wenn die
     lokale Frische-/Cooldown-Policy ihn als fällig bewertet. `ExportActivity`
     ist im Manifest `exported=false`; ein expliziter Component-Start durch
     `adb shell` ist daher kein verfügbarer, unterstützter Weg zum Vorab-Backup.
     Die Launcher-`MainActivity` nicht öffnen, bevor ein sicherer Exportpfad
     feststeht.
   - Falls kein bereits verfügbarer, unterstützter Weg zum Export ohne
     `MainActivity` existiert, vor jeglichem Start stoppen. Keine
     `run-as`-, Root- oder SharedPreferences-Manipulation und `ExportActivity`
     nicht für externe Starts freigeben. Der Ablauf benötigt dann eine
     abgestimmte technische Lösung, die Backup vor Sync gewährleistet.
3. **Vorher-Inventar**
   - Aus der privaten lokalen Datenbank read-only `integrity_check`,
     Schema-/Backup-Version, archivierte Originale (Anzahl/Bytes),
     Referenzbeziehungen, FIT-/Nicht-FIT-Aufteilung, Import-/Fehlerstatus,
     kanonische Beobachtungen und Rohfeld-Provenienz erfassen.
   - Export-/Inventarbericht lokal speichern; Rohoriginale und SQLite-Backup
     nicht überschreiben.
4. **Ein Sync**
   - Nach geprüftem Backup die Launcher-`MainActivity` genau einmal öffnen.
     Wenn deren bekannte automatische Historien-Sync-Policy den Lauf startet,
     zählt das als den einen freigegebenen Lauf; den manuellen Button nicht
     zusätzlich betätigen. Nur wenn kein automatischer Lauf startet und der
     Status einen eindeutigen, nicht laufenden Zustand zeigt, darf der Nutzer
     genau einmal den normalen manuellen Full-Pull bestätigen.
   - Ein vorhandener alter Terminaltext oder ein unklarer Status genügt nicht
     als Nachweis eines neuen Laufs: dann stoppen, nicht zusätzlich manuell
     starten. Kein Testlabor-Retry, keine zweite Verbindung und keine neue
     BLE-Probe im selben Lease.
   - Bestehende Grenzen unverändert: 120 s, 64 Item-List-Seiten, 256 Dateien,
     8 MiB je Datei, 64 MiB je Lauf. Eine erreichte Grenze wird als
     unvollständig beendet und nicht erweitert.
   - Vollständige terminale Diagnose lokal aufbewahren: Listenantworten/-seiten,
     gelistete und ausgewählte Elemente, Pull-Requests, Transfer-/Integritäts-/
     Archiv-/Decode-/SQLite-Stufen, empfangene Bytes, Fehler/Abbruch und
     `coverage=device-history-unverified`.
   - Bei negativer Protokollantwort, ungültigen Frames, unerwartetem
     Handshake-/Gerätezustand oder möglicher Zustandsmutation sofort den
     bestehenden kontrollierten Stopp nutzen. Keine alternative Sequenz
     ausprobieren.
5. **Nachher-Inventar / Cleanup**
   - Nach dem terminalen Ergebnis privates Post-Run-Backup exportieren und
     ZIP-Integrität prüfen. Danach read-only dieselben Inventarzähler,
     `integrity_check`, neue Originalinhalte/Referenzen, FIT-Feldabdeckung,
     Importstatus und SQLite-Provenienz erfassen und mit vorher vergleichen.
   - Empfangene vollständige Originaldateien gegen gespeicherte Bytes/Hash
     prüfen; keine lokale Dateiidentität vor dem Pull als Skip verwenden.
     Unbekannte und Nicht-FIT-Inhalte mitzählen, ohne medizinische Validierung
     vorauszusetzen.
   - Lease freigeben und Status erneut prüfen; nachher MUSS S10 FREI sein.

## Auswertung / Folgeentscheidung

- `runDrained` bedeutet nur, dass die Queue dieses Laufs abgearbeitet wurde.
  Es ist kein Nachweis vollständiger Gerätehistorie.
- Vergleiche Listen-/Seitengrenzen, fehlende oder wiederholte Antworten,
  erfolgreiche Pull-Responses, Transferabschluss, erhaltene Originalbytes,
  SQLite-Inventar und tatsächliche Rohfeldabdeckung getrennt.
- Bei Timeout/Abbruch keine Vollständigkeit behaupten. Offline feststellen, ob
  verbleibende Arbeit in Listentransaktion, Transfer, Persistenz oder Import
  liegt; ein erneuter Full-Pull ist weiterhin der sichere Fallback, nicht ein
  Identitäts- oder Resume-Gate.
- Rohcaptures, Backups, Hashes, Gerätekennungen, UIDs, Protokolltokens und
  Gesundheitswerte bleiben lokal/autorisiert; Git-Berichte enthalten nur
  redigierte Summen und Methodengrenzen.

## Status

**2026-11-24 – genau ein automatischer Lauf, terminal unvollständig.**
Unter exklusiver S10-Lease (danach Status `FREI`) wurde das S10-Modell
`SM-G970F` geprüft, das vorhandene Datenbankbackup intern exportiert und dessen
ZIP-Integrität sowie Inventar lokal geprüft (Backup v9, 50 Originale,
682,604 Originalbytes; keine Rohdaten im Git). Signierter Build 2062 mit
bestätigter Zertifikatskontinuität wurde installiert und `MainActivity` genau
einmal geöffnet. Ein erneuter read-only UI-Nachblick zeigte den durch diesen
Start ausgelösten automatischen Lauf; es wurde kein manueller Lauf gestartet.
Der terminale Report ist **unvollständig**: `runDrained=false`,
`coverage=device-history-unverified`,
`incompleteReason=next-transaction-followup-empty`, letzte
`nextTransaction`-Folgeantwort leer, keine weitere Transaktion angefordert.
Berichtete Zähler: 34 Dateien übertragen, verifiziert und privat gespeichert;
32 dekodiert, 12 SQLite-committed, 0 importiert, 12 ohne importierbare Daten,
20 als nicht erfolgreich/fehlgeschlagen ausgewiesen. Gemäß Codeklassifikation
der Stage-Zähler bedeutet das: Die übrigen 20 dekodierten Dateien scheiterten
im FIT→SQLite-Import/Commit-Pfad; die konkrete Exception-Ursache ist im
sichtbaren Reportausschnitt nicht enthalten. Der leere Transaction-Follow-up ist
ein separater Listen-Fortsetzungsfehler, nicht Erklärung für diese 20
Importfehler. Kein Nachweis vollständiger Gerätehistorie. Post-Run-Backup wurde
nicht erstellt; deshalb keine unabhängige Nachprüfung der Persistenz durch
Backup-Diff. Lauf-ID und Rohdiagnose bleiben lokal. Keine Wiederholung.

Separater Gate-Hinweis: PR-Head-FULL/API-35 war grün; Post-Merge-Main-FULL
`45370109779` war rot (`DashboardTest#dashboardShowsApprovedOverviewSections`,
Assertion `Schlaf`). Dieser Fehler wurde vor dem Test offengelegt.

**Offline-Fortsetzung 2026-11-24:** Fehlerdiagnostik wurde so erweitert, dass
künftige Sync-Berichte nur die redigierte Pipeline-Phase und Exception-Klasse
pro aggregiertem Fehler ausweisen; Exception-Texte und Pfade werden nicht
ausgegeben. Der konkrete Typ bzw. die genaue Unterphase der 20 Fehler dieses
Laufs bleibt rückwirkend **unbekannt**: Die damaligen Daten trennen Import
und tatsächliche SQLite-Commit-Ausführung nicht ausreichend.

Der erste lokale FULL-Lauf auf dem aktuellen Arbeitsstand hatte einen einzelnen
API-35-Fehler in
`CirqaRecorderStallSimulationTest#connectionWithReadingsIsNeverRecycled`
(`stall_detected=1`, obwohl Test-Readings flossen). Der unveränderte Test
bestand anschließend im vollständigen lokalen FULL: **122 Python-Tests**,
JVM/Build/R8, Lint,
Instrumentation-APK und API-35-Emulator; letzter Emulatorbericht **177 Tests,
0 Fehler, 1 übersprungen**, beide Stall-Simulationstests bestanden. Damit ist
der frühere Fehler derzeit nicht reproduziert und als möglicher Timing-Flake
abzugrenzen, nicht als behobener Produktfehler. Ein direkter isolierter
Gradle-Aufruf scheiterte zuvor nur, weil der Emulator bereits beendet war;
der CI-Wrapper startete ihn kontrolliert neu. Das grüne lokale FULL ersetzt
weder den unabhängigen Review noch den erforderlichen Main-FULL-Gate.

**2026-11-24 23:08 UTC – Folgetest #637 nach Merge, zunächst unklar.**
Unter S10-Lease wurde das Modell geprüft, die Signatur des installierten
Pakets mit dem signierten `main`-Build verglichen und Version 2132 ohne
Datenlöschung installiert. `MainActivity` wurde genau einmal geöffnet.
Danach fand die automatisierte UI-Prüfung weder einen eindeutig laufenden
Sync noch den normalen manuellen Download-Button und stoppte, ohne einen
Sync-Dialog zu bestätigen. Der UI-Dump scheiterte zunächst mit „could not get
idle state“; deshalb wurde erst kein Laufstatus behauptet. Bei der späteren
rein lesenden Screenprüfung wurde der von genau diesem App-Start ausgelöste
automatische Lauf samt Terminalbericht sichtbar. Es gab keinen manuellen Lauf,
Retry, zweite App-Öffnung oder weitere Protokollverbindung.

**Fehleranalyse 2026-11-24 23:18–23:21 UTC:** Terminalgrund
`session-time-limit`, `runDrained=false`, Laufzeit 120,017 ms. Der Sync wurde
angenommen; eine Item-List-Antwort kam nach 2,150 ms und enthielt 31 Elemente
auf einer Seite (5,208,827 gelistete Bytes). `nextTransaction=true`, aber
kein Follow-up war vor dem Abbruch eingereiht. Ein Pull-Request wurde gesendet
und seine FileAccess-Antwort war erfolgreich (`result=0`, `transport=0`,
keine Error-Codes, kein Dateipfad); die Antwort meldete Feld 7
`compressionWindow=11`. MLR-Service-Registrierung und -Konfiguration waren
erfolgreich. Beim ersten Element (erwartet 4,841 Bytes) kamen 17 MLR-Chunks
mit insgesamt 2,424 Bytes an, danach wurde der Datei-Transfer nicht
vervollständigt. Für diesen Transfer wurden 115,923 ms gemessen; am
120-s-Laufbudget wurde abgebrochen. 30 ausgewählte Elemente blieben in der
Queue. Keine Datei erreichte Transferabschluss, Integritätsprüfung, Archiv
oder SQLite; daher zeigt die UI korrekt **0 von 31**.

Der sichtbare `5043-Transport-ACK` hatte `gfdiAck=0`, `successFlag=0`,
`status=0`. Das ist laut aktuellem Code-Kandidaten ein Transport-ACK ohne
Fehlerstatus, aber kein Anwendungsnachweis für eine Item-List-Antwort. Die
separate Item-List-Antwort ist im selben Terminalbericht dennoch belegt. Der
Fehler ist somit **kein belegter negativer 5043-ACK und kein FIT-/SQLite-
Importfehler**: der App-Assembler wartete nach 2.424 Bytes, obwohl die
aufgezeichneten Datenbenachrichtigungen zusammen einen vollständigen
Kompressionsstrom bilden. Die private HCI-Aufzeichnung wurde offline und ohne
Ausgabe von Nutzdatenbytes korreliert: die 17 Datenbenachrichtigungen ergeben
unverändert als zlib exakt 4.841 Ausgabebytes und erreichen den zlib-Endmarker.
Wenn wie im bisherigen Assembler zwei Bytes je Benachrichtigung abgeschnitten
werden, ist der Stream nicht mehr dekomprimierbar. Der CIRQA-Stream in diesem
Lauf hatte somit **keinen** solchen Zwei-Byte-Präfix; die App behandelte ihn
stattdessen als Rohdatei und wartete fälschlich auf 4.841 eingehende Bytes.
Das erklärt den Stillstand und den Timeout, ohne einen Fehler des Bandes oder
einen negativen 5043-ACK zu behaupten.

Der angeforderte Kompressionsfensterwert 15 und der gemeldete Wert 11 erklären
diesen Befund nicht; die tatsächlich empfangene zlib-Nutzlast ist vollständig
und größenkonsistent. Eine TransferStatus-Antwort ist damit für diesen
konkreten Stillstand nicht ursächlich. Der lokale Assembler-Fix erkennt jetzt
direkte zlib-Streams zusätzlich zur bereits unterstützten Präfix-Variante und
behält einen Rohdaten-Fallback für zlib-ähnliche Datei-Anfänge. Neue
synthetische Regressionstests und der gesamte JVM-Unit-Testlauf waren lokal
grün; ein Hardware-Re-Run ist nicht erfolgt und daher nicht als verifiziert
ausgewiesen. Keine Wiederholung. Post-Run-Backup und Nachher-Inventar fehlen;
 Vorher-Backup und Screenshots bleiben privat.

**2026-11-25 11:56–12:15 UTC – nutzerautorisierter Folgetest vor Sync gestoppt.**
Unter S10-Lease wurden Modell und installierte Paket-Signaturkontinuität
read-only verifiziert; der aktuelle Source-Stand deaktiviert den History-Sync
beim App-Eintritt. Der unterstützte In-App-Datenbankbackup-Export wurde vor
jedem History-Request versucht. Innerhalb des begrenzten Wartefensters erschien
keine bestätigte Backup-Fertigmeldung; der privat gezogene Kandidat war leer und
bestand die ZIP-Prüfung nicht. Dieses Artefakt wurde nicht als Sicherung
akzeptiert und nicht veröffentlicht. **Kein History-Button bestätigt, kein
BLE-/GFDI-Sync-Request gesendet, keine App-Daten gelöscht oder überschrieben.**
Der S10-Lease wurde freigegeben und anschließend als `FREI` verifiziert.
Der Lauf ist blockiert, nicht erfolgreich oder unvollständig übertragen: Vor
einem neuen Versuch muss ein frisches, vom Exporter als abgeschlossen
bestätigtes und integritätsgeprüftes Vorher-Backup verfügbar sein. Frühere
Backups gelten nicht automatisch als aktueller Gerätestand.

**Read-only-Nachprüfung 2026-11-25 12:28–12:30 UTC:** Vorhandene Dateien
wurden unter einer exklusiven S10-Lease lokal kopiert; es wurde keine App-,
BLE- oder History-Aktion ausgelöst. Von 15 gefundenen Einträgen war einer kein
kopierbarer Dateipfad, drei Kopien waren leer und elf bestanden den ZIP-Test.
Zwei validierte v11-Backups stimmen bytegenau mit bereits privat untersuchten
Snapshots überein: der frühere Vorherstand enthält 1.689 verifizierte Originale
(27 FITs), der frühere Nachherstand 1.938 (37 FITs). Beide sind historische
Kopien des vorherigen Laufs, keine frische Sicherung nach dem aktuellen
Exportversuch. Die drei neu angelegten `pre-run`-Dateien sind leer. Damit bleibt
der aktuelle Backup-Gate geschlossen; kein History-Request wurde gesendet.
Lease anschließend erneut `FREI`.

**Frischer Export-Folgeversuch 2026-11-25 12:46–13:10 UTC – vor BLE gestoppt.**
S10-Modell, App-Version 2204 und Signaturkontinuität wurden unter Lease erneut
verifiziert. Ein neu benannter Export im bisherigen DocumentsUI-Unterordner
`Downloads/VideoDownloader` endete mit der App-Rückmeldung „Backup
fehlgeschlagen“; die lokale Kopie war 0 Byte. Der gleiche In-App-Export wurde
anschließend im `Downloads`-Wurzelverzeichnis versucht und ebenfalls als
fehlgeschlagen gemeldet; auch diese lokal kopierte Zieldatei war 0 Byte und
bestand keine ZIP-Prüfung. Ein Zielordnerwechsel löst den Fehler somit nicht.
Der Save-Dialog/Export wurde nur für diese Backup-Diagnose verwendet. Kein
History-Button bestätigt, kein BLE-/GFDI-Request gesendet, keine vorhandene
Datei gelöscht oder überschrieben. S10-Lease nach dem letzten Wrapper `FREI`.
Der History-Test bleibt blockiert; als nächster Schritt ist die Ursache des
generischen Exportfehlers offline zu untersuchen, statt weitere identische
Geräte-Exporte oder einen Sync ohne aktuelles Backup zu starten.

**Einzellauf nach GUI-Backup-Fix 2026-11-25, S10 Build 2218:** Mit
Signaturkontinuität wurde ein frisches Vorher-Backup erstellt, lokal kopiert
und validiert: Backup v11, 23 Manifest-Einträge, 1.938 verifizierte Originale
(779.267 B; 37 FITs), keine Manifest-/Archivfehler. Danach genau einmal den
manuellen CIRQA-History-Download bestätigt. Ein terminaler Bericht lag nach ca.
75 s vor; kein Retry.

Der Lauf wurde angenommen (`accepted=true`, `itemListSent=true`, `pullSent=true`)
und meldete 4 Listenseiten, 27 ausgewählte / 27 erfolgreiche / 0 fehlgeschlagene
Dateien (510.892 ausgewählte Bytes). Drei Transaktions-Folgeanfragen wurden
gestellt; die letzte Antwort enthielt 0 Einträge. Terminalstatus:
`runDrained=false`, `incompleteReason=next-transaction-followup-empty`,
`coverage=device-history-unverified`. Daher ist keine vollständige Remote-Historie
belegt. Der terminale Bericht wurde zunächst übersehen, weil der Status außerhalb
des sichtbaren UI-Bereichs lag; die private Accessibility-Ausgabe bestätigt ihn.

Anschließend wurde ein Nachher-Backup lokal gezogen und geprüft: v11, 23
Manifest-Einträge, 2.177 verifizierte Originale (804.113 B; 37 FITs), keine
Manifest-/Archivfehler. Gegenüber vorher: +239 Originale, +24.846 B, FIT-Anzahl
unverändert. Die kanonischen FIT-Quellen, Provenienz und 29.123 Beobachtungen
blieben unverändert; `import_runs` stieg um 1 und `observation_status_events`
um 2. Diese lokalen Differenzen beweisen keine vollständige Geräteabdeckung.
Keine Wiederholung/Retry; S10-Lease danach `FREI`.
