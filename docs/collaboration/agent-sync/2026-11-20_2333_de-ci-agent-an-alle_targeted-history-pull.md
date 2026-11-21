# Gezielter CIRQA-Datei-Pull (#171 / #148)

- Von: de ci-agent (OpenCode)
- An: alle, insbesondere Bearbeiter von PR #345
- Status: offen – PR-Überschneidung erledigt, UID-Stabilitätsfrage offen
- Nutzerauftrag: gezielt nur CIRQA-Dateien pullen, deren Inhalt der lokalen Datenbank noch fehlt.

## Befund und Plan

- Der bestehende Produktpfad zieht derzeit die gelisteten Dateien; lokale FIT-Ablage/SQLite sind content-addressiert, aber es gibt keine persistente Zuordnung von FileAccess-UID/Metadaten zu lokal abgeschlossenem Import.
- `HISTORICAL_SYNC.md` dokumentiert ausdrücklich, dass UID-Stabilität über getrennte Sync-Sitzungen noch nicht nachgewiesen ist. Unbekannte IDs dürfen daher nicht optimistisch übersprungen werden.
- Ich implementiere auf `de-ci-agent/targeted-history-pull` zunächst eine additive DB-/Backup-Manifest-Schnittstelle und eine offline getestete fail-safe Auswahlpolicy. Bereits gespeicherte Dateien können nur dann übersprungen werden, wenn eine geprüfte Manifestidentität exakt passt; unbekannte/mehrdeutige Fälle bleiben zu pullen.
- Bereits übertragene Dateien ohne altes UID-Manifest lassen sich nicht sicher per Größe/Typ mit Remote-Items verknüpfen; erster Lauf nach Einführung kann daher noch Dateien erneut abrufen. Lokale content-hash-Dedupe und Import-Idempotenz verhindern doppelte Inhalte/Zeilen, sparen aber diesen ersten Funktransfer nicht.
- Es ist keine neue BLE-Sequenz geplant; der vorhandene `sync_request` + ItemList + Pull-Pfad bleibt bestehen. Er kann einen geräteseitigen Sync-Zustand beeinflussen; Datei-Pulls werden weiter nur nach Nutzerbestätigung gestartet.
- Kein Hardwaretest/Gate-Reset ist Teil dieses Codeauftrags. Das zuvor erschöpfte lokale No-Pull-Versuchslimit wurde auf ausdrückliche Nutzerfreigabe von 15 auf maximal 40 angehoben; der vorhandene Verbrauchszähler wird nicht zurückgesetzt. Ein neues Gerätelauf bleibt bis zur ausdrücklichen Gerätefreigabe ausgesetzt.

## Abstimmung

PR #345 ist inzwischen nach `origin/main` gemergt; der Branch wurde darauf fast-forward aktualisiert. Die vorherige Codeüberschneidung ist damit erledigt. Cross-session-UID-Stabilität ist weiterhin nicht belegt; bis zum Listen-only-Nachweis werden unbekannte Identitäten nicht übersprungen.

Der Nutzer hat das persistente No-Pull-Testlimit ausdrücklich von 15 auf 40 angehoben. Das ist eine Obergrenze; der vorhandene Versuchszähler wurde nicht zurückgesetzt. Gerätezugriff erfolgt erst nach separater ausdrücklicher Freigabe.
