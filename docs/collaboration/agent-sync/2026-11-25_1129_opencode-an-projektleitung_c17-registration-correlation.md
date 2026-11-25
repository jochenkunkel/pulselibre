# OpenCode FIT → Projektleitung: c17 Registrierungsantworten weiter eingegrenzt

- **Datum:** 2026-11-25 11:29 UTC
- **Von:** OpenCode FIT
- **An:** Projektleitung
- **Bezug:** c17 Offline-Replay; PR #653
- **Status:** Kontrollkanal-Fakten präzisiert; FileAccess-/ItemList-Lücke bleibt.

## Bestätigte Fakten

- 29 13-Byte-Hostnachrichten im abgeleiteten c17-Write-Handle treffen das beobachtete Connect-Client-ID-1-Registrierungspräfix.
- Dazu passen in Capture-Reihenfolge 29 16-Byte-Antworten nach Service-ID; 16 unterschiedliche Service-IDs, alle Antwortstatusbytes `0`.
- Das Antwortbyte an der Handle-Kandidatenposition liegt 21-mal in `0x80..0x87` und 8-mal in `1..127`. Die gemischte Form ist semantisch ungeklärt.
- Die Dokumentation korrigiert damit die ältere pauschale Aussage, alle Antwort-Handlebytes lägen in `0x80..0x87`. Es wird keine Handle-Normalisierung vorgenommen.

## Unsicherheiten / Grenzen

Die Paarung beruht auf beobachteter Nachrichtenreihenfolge plus Service-ID, nicht auf einer GFDI-Request-ID. Die c17-ATT-Handlezuordnung selbst bleibt peer-korreliert/abgeleitet. Status 0 und diese Antwortbytes belegen weder einen 5043/5044-History-Request noch Route-4-zu-Service-1-Zuordnung oder einen Dateiabschluss. Keine Geräteaktion.

Der präzisierte Befund ist in `CIRQA_CONNECT_NORMAL_HISTORY_COMPARISON_2026-11-25.md` ergänzt. PR #653 ist weiterhin docs-only ohne PR-Statuschecks; kein Merge.
