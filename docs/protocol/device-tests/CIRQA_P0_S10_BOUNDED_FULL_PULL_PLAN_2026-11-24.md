# P0 S10: begrenzter Full-Pull-Testplan (2026-11-24)

## Zweck und Freigabegrenze

Dieser Plan bereitet einen nutzerinitiierten Abruf über die bereits bekannte
Connect-kompatible Protobuf/FileAccess-Sync-Sequenz vor. Die Projektleitung hat
den kontrollierten Test mit bekannten, hinreichend charakterisierten Lese- und
Sync-Kommandos am aktuellen CIRQA-Testarmband ausdrücklich freigegeben.

Der Plan fügt keine Protokollkommandos hinzu und ändert keine Laufbudgets,
Leases oder Freigaben. Keine SetFlags-, Archive-, Delete-, Reset-, CLOSE_ALL-,
Legacy-Experiment- oder unbekannten Requests. Der signierte Build muss die
Versions-/Signaturkontinuität wahren; erforderliche QUICK- und
Dependency-Gates müssen grün sein und FIT dokumentiert den Self-Review.
FULL/API-35 ist nach der aktuellen Owner-Entscheidung kein Merge- oder
Hardwaretest-Gate. Der manuelle History-Download bleibt verfügbar;
automatischer History-Sync beim App-Start ist deaktiviert.

## Einmaliger Lauf

1. **Abstimmung / Build**
   - Aktuellen Code, Laufgrenzen und den manuellen UI-Startpfad prüfen;
     erforderliche QUICK-/Dependency-Checks und FIT-Self-Review dokumentieren.
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
   - Auto-History-Sync beim App-Start ist deaktiviert. `ExportActivity` ist im
     Manifest `exported=false`; ein expliziter Component-Start durch `adb shell`
     ist kein verfügbarer unterstützter Weg zum Vorab-Backup. Für das Backup
     den UI-Export verwenden und erst danach die manuelle Sync-Aktion starten.
   - Falls der unterstützte UI-Export vor dem Sync nicht erfolgreich geprüft
     werden kann, vor der manuellen Sync-Aktion stoppen. Keine `run-as`-, Root-
     oder SharedPreferences-Manipulation und `ExportActivity` nicht für externe
     Starts freigeben.
3. **Vorher-Inventar**
   - Aus der privaten lokalen Datenbank read-only `integrity_check`,
     Schema-/Backup-Version, archivierte Originale (Anzahl/Bytes),
     Referenzbeziehungen, FIT-/Nicht-FIT-Aufteilung, Import-/Fehlerstatus,
     kanonische Beobachtungen und Rohfeld-Provenienz erfassen.
   - Export-/Inventarbericht lokal speichern; Rohoriginale und SQLite-Backup
     nicht überschreiben.
4. **Ein Sync**
   - Nach geprüftem Backup den normalen manuellen Full-Pull genau einmal
     bestätigen. Der App-Start selbst löst keinen History-Sync aus.
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

**2026-11-25 19:17 UTC – erneuter manueller Lauf nach #670, Build 2250.**
Unter exklusiver S10-Lease wurden Modell, Build und Signaturkontinuität
geprüft; das In-place-Update erhielt App-Daten. Frisches Vorher-Backup v11:
2.844 archivierte Originale / 984.816 Bytes, 37 FIT-Originale,
2.850 Archiv-Referenzzeilen. Genau ein manueller Download wurde bestätigt.
Der neue terminale Bericht (neue Run-ID; nicht der vorherige UI-Bericht) meldet
eine Listenantwort mit 29 Dateien und 29 Pull-Requests. Ergebnis:
`runDrained=false`, `incompleteReason=session-time-limit`,
`coverage=device-history-unverified`; 28 Dateien transferiert, verifiziert
und privat gespeichert; 25 dekodiert, 24 SQLite-committed, 18 importiert,
6 ohne importierbare Daten, 1 Fehler `FIT_IMPORT_COMMIT/SQLiteDatabaseLockedException`.
27 Dateien wurden als abgeschlossen und 1 als fehlgeschlagen gezählt; beim
Timeout war ein Item mit erwarteten 6.010 Bytes und 0 empfangenen Bytes aktiv.
Damit sind weder 29/29 vollständige Transfers noch eine vollständige
Gerätehistorie belegt.

Das geprüfte Nachher-Backup v11 enthält 3.054 Originale / 1.009.688 Bytes,
weiterhin 37 FIT-Originale und 3.060 Archiv-Referenzzeilen; Manifest- und
Archivgruppenprüfung bestanden. Der Inventardiff beträgt +210 Originale und
+210 Referenzen, aber keine zusätzlichen FIT-Originale oder kanonischen
Beobachtungen. Der Lauf wurde nicht abgebrochen oder wiederholt; das appinterne
120-s-Limit blieb unverändert. Lease danach `FREI`. Die verbleibende
SQLite-Lock-Exception liegt diesmal im FIT-Import/Commit-Pfad; Ursache und
Rückwirkung auf den fehlgeschlagenen Import sind offline zu triagieren. Roh-
 und Gerätedaten verbleiben ausschließlich lokal privat.

**2026-11-25 23:13 UTC – genau ein manueller Folgelauf nach SQLite-open-Fix #678, Build 2271.**
Unter einem einzelnen S10-Lease wurden Modell und installierte Version 2263
geprüft; der signierte Main-Build 2271 hatte dieselbe Signatur und wurde ohne
Datenlöschung in-place installiert. Vor dem Update wurde über den unterstützten
UI-Export ein frisches Backup v11 erstellt, lokal geöffnet und geprüft:
3.054 verifizierte Originale / 1.009.688 Bytes, 37 FIT-Originale, 3.060
Archiv-Referenzen; Manifest- und Archivgruppenfehler jeweils 0. Genau ein
manueller Download wurde bestätigt; die angezeigte Run-ID war neu.

Der terminale Report nach dem unveränderten 120-s-Limit meldet eine
Item-List-Seite mit 32 ausgewählten Dateien und 32 Pull-Requests;
`runDrained=false`, `incompleteReason=session-time-limit`,
`coverage=device-history-unverified`. 31 Transfers erreichten Transferabschluss,
30 Integritätsprüfung, 31 private Speicherung, 26 Decode und SQLite-Commit,
19 Import mit Beobachtungen sowie 7 Dateien ohne importierbare Daten. Ein
weiteres Element scheiterte in `ORIGINAL_ARCHIVE` mit
`SQLiteDatabaseLockedException`; es ist weder als vollständiger Transfer noch
als verifiziert gezählt. Keine Transaction-Follow-ups; 30 von 32 wurden als
erfolgreich ausgewiesen. Damit bleiben vollständige Übertragung und
Gerätehistorie unbewiesen.

Das geprüfte Nachher-Backup v11 enthält 3.242 verifizierte Originale /
1.065.612 Bytes, 39 FIT-Originale und 3.248 Archiv-Referenzen. Manifest- und
Archivgruppenfehler blieben 0. Der geprüfte SQLite-Diff weist jeweils +597
Zeilen in `canonical_observations`, FIT-Generationsbeobachtungen und
FIT-Beobachtungsprovenienz, 2 FIT-Originale und 5 Import-Fehlerzeilen aus;
unbekannte Felder bleiben in den FIT-Generationen inventarisiert.
`downloaded_file_archive` stieg um 376 Tabellenzeilen. Dies
belegt Persistenzzuwachs, aber weder semantische Vollständigkeit noch eine
vollständige Gerätehistorie. Der Import-Open-Retry aus #678 verhindert nicht
alle SQLite-Locks: der verbliebene Fehler lag laut Stage-Klassifikation im
Originalarchiv, nicht im FIT-Import/Commit. Lockhalter und konkrete Ursache
bleiben unbekannt; empfangene Originale und Details bleiben privat.

Lease nach dem Lauf `FREI`; kein Retry und kein zweiter BLE-Lauf. Die
instrumentierte Main-FULL-Prüfung `45719176447` auf `0d894299` war zum
Zeitpunkt der Dokumentation noch aktiv; sie ist kein Merge-Gate nach aktueller
Owner-Entscheidung. Hardwaretest nicht als Nachweis vollständiger Historie
interpretieren.
