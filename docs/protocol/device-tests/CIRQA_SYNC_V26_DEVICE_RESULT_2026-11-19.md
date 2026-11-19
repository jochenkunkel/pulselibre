# CIRQA Sync V26 – Geräteergebnis (19.11.2026)

## Umfang und Grenzen

- Gerät: Galaxy S25; PulsLibre `2.0.0_build1158`.
- Ein vom Nutzer ausdrücklich freigegebener Lauf über den bestehenden
  `startSyncRequestTest`-Pfad. Das persistente Gate
  `cirqaSyncAttemptedV25` wurde vor dem Verbinden verbraucht; kein Reset und
  kein Retry.
- Vor dem Lauf war laut Recorder-Heartbeat-Regel keine aktive Recorder-Sitzung
  vorhanden; Android meldete keinen laufenden Recorder-Service und keine
  aktive GATT-Verbindung.
- Ein Connect-kompatibler `sync_request` nach frischem Bootstrap. Grenzen:
  120 Sekunden, 256 Dateien, 64 MiB. Keine Archiv-, Flag-, Lösch-, Reset- oder
  `CLOSE_ALL`-Befehle.
- Das Gerät kann bei diesem Pfad gepufferte Daten als übertragen markieren;
  diese dokumentierte Protokollnebenwirkung wurde bei der Nutzerfreigabe
  berücksichtigt.

## Ergebnis

- Sync angenommen; die beobachtete Item-Liste wurde vollständig abgearbeitet.
- 42 Dateien vollständig empfangen: 40 FIT-Dateien und 2 Nicht-FIT-Dateien,
  insgesamt 218.951 Bytes. 1.520 MLR-Chunks; 0 fehlgeschlagene Transfers.
- Alle 40 FIT-Dateien bestanden die lokale FIT-Hüllen-/CRC-Prüfung; die
  content-addressierten Dateinamen stimmten für alle 42 Dateien mit SHA-256
  überein. Die unabhängige Offline-Inventur klassifizierte die 2 übrigen
  Dateien als XML und unbekanntes Protobuf-Schema; unbekannte Inhalte wurden
  nicht interpretiert.
- Die redigierte Offline-Inventur wurde mit dem Garmin-FIT-Profil 21.222.0
  erstellt. Keine Sample-Werte, Gerätekennungen, Dateinamen oder Hashes werden
  hier veröffentlicht.
- Die read-only geprüfte kanonische Datenbank hatte
  `integrity_check=ok`, 40 FIT-Quellen und weiterhin 294 normalisierte
  Beobachtungen (Herzfrequenz 129, Distanz 129, Geschwindigkeit 36).
  Die Beobachtungszahl stieg nicht; der erneute Import war hinsichtlich der
  normalisierten Messwerte idempotent. Die Event-Tabelle enthielt 117.746
  Zeilen.

## Abgleich mit dem privaten V24/V25-Archiv

- 30 der zuvor archivierten FIT-Inhalte und eine der zwei zuvor archivierten
  Rohdateien stimmen per SHA-256 mit dem aktuellen Abruf überein.
- Der aktuelle Abruf enthält 10 FIT-Inhalte und eine Rohdatei, die im
  vorherigen Offline-Archiv nicht enthalten waren. Umgekehrt wurde ein FIT
  aus dem vorherigen Archiv in dieser Item-Liste nicht wieder beobachtet.
- Damit ist auch V26 kein Nachweis eines vollständigen historischen
  Gerätearchivs. Die nicht wieder beobachtete Datei und die begrenzte
  Item-List-Seite bleiben offene Abweichungen.

## Datenschutz und Reproduzierbarkeit

Die Originale, vollständige SHA-256-Werte, FIT-Feldmatrix und
Inventurartefakte verbleiben ausschließlich privat unter
`/tmp/opencode/issue-150-private/s25-sync-followup/`. Dieser Bericht enthält
nur aggregierte, redigierte Ergebnisse. Es wurden keine Instrumentierungstests
auf dem S25 ausgeführt.
