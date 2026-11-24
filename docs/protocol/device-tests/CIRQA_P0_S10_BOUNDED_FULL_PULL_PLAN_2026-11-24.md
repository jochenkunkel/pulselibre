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
3. **Vorher-Inventar**
   - Aus der privaten lokalen Datenbank read-only `integrity_check`,
     Schema-/Backup-Version, archivierte Originale (Anzahl/Bytes),
     Referenzbeziehungen, FIT-/Nicht-FIT-Aufteilung, Import-/Fehlerstatus,
     kanonische Beobachtungen und Rohfeld-Provenienz erfassen.
   - Export-/Inventarbericht lokal speichern; Rohoriginale und SQLite-Backup
     nicht überschreiben.
4. **Ein Sync**
   - Nutzer startet genau einen normalen historischen Full-Pull im bestehenden
     Produktpfad. Keine Testlabor-Wiederholung, kein zweiter Sync und keine
     neue BLE-Probe im selben Lease.
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

Nur Vorbereitung. Für diese Sitzung ist kein Hardwarezugriff erfolgt. Offen
vor Ausführung: unabhängige CI-Prüfung/Abstimmung und PR-CI auf dem aktuellen
Head.
