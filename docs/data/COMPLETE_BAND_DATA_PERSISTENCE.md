# Projektentscheidung: CIRQA-Banddaten vollständig lokal erhalten

**Entscheidung vom 24.11.2026:** Vollständigkeit der lokalen Speicherung hat Vorrang. Fehlende medizinische Vertrauensbewertung ist **kein** Grund, empfangene Banddaten oder tatsächlich dekodierte FIT-Felder auszulassen. Diese Regel ist keine Freigabe zur Anzeige, medizinischen Interpretation, Weitergabe oder zu zusätzlichen Hardware-/BLE-Aktionen.

## Persistenzvertrag

1. **Originaldatei ist maßgeblich.** Jede empfangene Datei wird vor semantischer Verarbeitung durch `DownloadedFileArchive` bytegenau und SHA-256-geprüft in `downloaded_files` abgelegt. Unbekannte FIT-Nachrichten/-Felder und nicht-FIT-Dateien bleiben dadurch unabhängig vom Decoder erhalten. Das versionskontrollierte Backup enthält die Originaldatei-Archive.
2. **FIT-Decoder-Ausgabe ist generation-scoped.** `FitHistoricalObservations` erfasst die exakten Bytes jedes FIT-Datenfelds (Standard- und Developer-Felder) mit Quellhash, Decoder-/Profilgeneration, Nachrichtenordinal, globaler Nachrichtennummer, Feldnummer, BaseType soweit vorhanden, Größe, Endianness, Developer-Index und Timestamp-Herkunft. Auch Sentinel-/Status-/unbekannte Feldwerte werden nicht aus dieser Rohfeldablage herausgefiltert. Semantische Beobachtungen und Review-Kandidaten sind zusätzliche Projektionen, keine Ersatzdaten.
3. **Unveränderlichkeit und Aktivierung.** Generationen sind unveränderlich. Der SHA-256-Output-Digest umfasst normalisierte Beobachtungen und sämtliche generation-scoped Rohfelder. Replay schreibt eine neue inaktive Generation; Aktivierung prüft Anzahl und Digest vollständig und aktualisiert den aktiven Pointer atomar. Die vorherige Generation bleibt als Rollback-Ziel erhalten.
4. **SQLite-/Backup-Kompatibilität.** Das kanonische SQLite-Schema wird auf v11 angehoben. Backup-v10 ergänzt generation-scoped FIT-Rohfelder; Restore akzeptiert weiterhin die Formate v1–v9 und erhält Daten, die diese Formate enthalten.
5. **Keine Umdeutung.** Untrusted/Profil-Kandidaten werden nicht allein durch Persistenz zu verifizierten Health-Observations. Projektionen können Werte filtern; die lokale Rohdatei und FIT-Feldgeneration bleiben davon unberührt.

## Decodergrenzen und explizite Fehler

Der App-Decoder ist kein vollständiger FIT-SDK-Ersatz. Er interpretiert bekannte Profilfelder und bewahrt die originalen Bytes aller tatsächlich geparsten FIT-Datenfelder strukturiert auf; die Originaldatei erlaubt späteres erneutes Dekodieren. Größen-/Ressourcenlimits (u. a. Archiv- und Feldanzahllimits) dürfen nicht stillschweigend zu einem erfolgreichen Teilimport führen: ein Import, der seine Grenzen erreicht, muss als fehlgeschlagen/abgelehnt behandelt werden; die zuvor archivierte Originaldatei bleibt erhalten. Die konkrete Abdeckung und unbekannten Felder dokumentiert die datensparsame Feldinventur, nicht eine Behauptung vollständiger semantischer FIT-Profilunterstützung.

## Privatsphäre und Umfang

Die Speicherung erfolgt lokal. Originalbytes können private Inhalte enthalten und gehören nicht als reale Rohdaten ins Git. Repository-Fixtures müssen synthetisch und geprüft sein. Diese Entscheidung ändert weder BLE-/FileSync-Verhalten noch Sicherheits- und Geräte-Leases. Für Geräte-/Hardwaretests ist hier keine Freigabe erteilt.

## Implementierungsstatus

- [x] Rohdateiarchiv und versioniertes Backup (v9; bestehender Pfad)
- [ ] FIT-Rohfeldgeneration, Digest, Replay/Aktivierung/Rollback und SQLite-v11
- [ ] Backup-v10 samt Restore v1–v9 und Roundtrip-Regression
- [ ] Vollständige synthetische Feldmatrix, Migration und FULL/API-35-Abnahme

Status ist absichtlich getrennt von der Produktentscheidung: die Entscheidung gilt, auch solange einzelne Implementierungs- und CI-Gates noch offen sind.
