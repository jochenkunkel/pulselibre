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

**2026-11-24 23:08 UTC – Folgetest #637 nach Merge, Ergebnis nicht feststellbar.**
Unter S10-Lease wurde das Modell geprüft, die Signatur des installierten
Pakets mit dem signierten `main`-Build verglichen und Version 2132 ohne
Datenlöschung installiert. `MainActivity` wurde genau einmal geöffnet.
Danach fand die automatisierte UI-Prüfung weder einen eindeutig laufenden
Sync noch den normalen manuellen Download-Button und stoppte, ohne einen
Sync-Dialog zu bestätigen. Ein einzelner lesender UI-Dump scheiterte mit
„could not get idle state“. Daher ist **nicht feststellbar**, ob der
App-Start einen automatischen Lauf ausgelöst hat oder ob ein Laufterminal
vorliegt. Kein manueller Sync wurde ausgelöst; kein Retry, keine zweite
App-Öffnung und keine weitere Protokollverbindung. Ein Post-Run-Backup und
Nachher-Inventar liegen nicht vor. Der Test ist unvollständig; keine Aussage
zu PullItem-Kompatibilität oder Datenpersistenz. S10-Lease danach `FREI`.
Lokales Vorher-Backup und private Installations-/UI-Evidenz bleiben außerhalb
des Repositories. Nicht erneut ausführen, bis Status und ein sicherer
Backup-/Inventarpfad unabhängig geklärt sind.
