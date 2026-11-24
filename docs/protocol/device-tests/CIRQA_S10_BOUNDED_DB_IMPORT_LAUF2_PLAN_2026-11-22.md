# S10 – neuer, separat freigegebener Lauf 2 des begrenzten DB-Import-Tests

Stand: 2026-11-22 12:22 UTC · Freigabe: Nutzer hat am 12:19 UTC „Freigabe für den neuen Lauf erteilt".

## Warum ein neuer Lauf

Lauf 1 erreichte die 5-Minuten-Frist ohne nachgewiesenen Abschluss und wurde bei
12:15:34 UTC per `am force-stop de.pulselibre` beendet. Der ursprüngliche Plan verbietet
einen Retry. Der neue Lauf ist daher ein **eigener, separat freigegebener Test** mit
diesem Dokument als Umfang und Stop-Bedingungen.

## Freigabe (Nutzerangaben)

- 12:19 UTC: „freigabe für den neuen lauf erteilt".
- 12:19 UTC: Garmin Connect soll **nur temporär** gestoppt werden:
  `am force-stop com.garmin.android.apps.connectmobile`. Kein `pm disable-user`,
  keine Entkopplung, kein Reset, kein `CLOSE_ALL`, keine dauerhafte Deaktivierung.
- Der Nutzer hatte zusätzlich darauf hingewiesen, dass früher schon einmal ein
  Garmin-Connect-Dienst deaktiviert war. Ergebnis der Prüfung siehe Nachtrag unten.

## Nachtrag: Dokumentationsprüfung zum früheren Garmin-Zustand (12:24 UTC)

Eine gründliche, rein lesende Suche über alle lokalen Repositories und Worktrees ergab:

- **Kein** Dokument nennt einen konkreten Garmin-Connect-Dienst, eine Komponente oder
  eine Einstellung, die deaktiviert wurde.
- Zwei Stellen berichten app-weit „deaktiviert", ohne Servicename:
  - `docs/archive/2026-11-24/protocol/research/GARMIN_PROTOCOL_RESEARCH.md:134` – „User reports Garmin
    Connect is disabled and cannot restart." (15.11.2026)
  - `docs/collaboration/agent-sync/2026-11-21_chatgpt-an-alle_garmin-connect-historie-nachgeladen.md:8`
    – „Garmin Connect war nach Aussage des Nutzers den ganzen Tag deaktiviert und
    zwischenzeitlich deinstalliert …" (21.11.2026)
- `pm disable-user` kommt in den Notizen nur als **abgelehnte** Option vor, nie als
  ausgeführter Befehl. Kein Agent hat Garmin deaktiviert.
- Read-only Geräteprüfung 12:19 UTC: `com.garmin.android.apps.connectmobile` steht in
  `pm list packages -e`, nicht in `pm list packages -d`; `pm dump` zeigt `enabled=0`
  (= Default-Zustand, nicht deaktiviert), `lastDisabledCaller: com.android.vending`
  (historische Metadaten).
- Ein Treffer „Garmin Connect reportedly warned and disabled it" bezieht sich auf die
  **Taste am Armband**, nicht auf einen Connect-Dienst.

**Folge:** Es gibt keinen dokumentierten Dienst, der wiederhergestellt oder erneut
deaktiviert werden müsste. Ausgeführt wird ausschließlich die vom Nutzer gewählte
temporäre Maßnahme: `am force-stop com.garmin.android.apps.connectmobile` unmittelbar
vor dem Download. Danach keine weitere Garmin-Änderung.

## Umfang (exakt)

1. Vor dem Lauf: lesende Prüfung von Gerät, installierter Version und Garmin-Paketstatus.
2. Garmin Connect nur temporär per `am force-stop` beenden, wenn die Nutzerentscheidung
   und die Dokumentationsprüfung das zulassen. Kein dauerhafter Eingriff.
3. Vorhandene UI: Reiter `Gerät` → `Historie herunterladen` → `Herunterladen`.
   Kein Testlabor, keine neuen BLE-/GATT-/FileSync-Kommandos.
4. Höchstens 5 Minuten ab der Meldung „gestartet" des Nutzers. Kein Retry, keine Schleife.
5. Danach SAF-Export durch den Nutzer; offline Vergleich mit der Baseline
   (alle 15 Tabellen hatten 0 Datensätze) ausschließlich anhand redaktierter Anzahlen
   und Prüfsummen.

## Stop-Bedingungen

Sofortiger Abbruch bei unerwarteter Geräteantwort, Pairing-/Reset-Aufforderung,
unklarem Verbindungsabbruch, destruktiver Aufforderung, Überschreitung von 5 Minuten
oder jeder neuen Unklarheit. Kein „einfach weiterprobieren". Beim Abbruch:
`am force-stop de.pulselibre`, danach Dokumentation ohne neue Aktion.

## Datenschutz / Grenzen

Keine Roh-BLE-Payloads, Gerätekennungen, Tokens, UIDs, Gesundheitswerte oder privaten
Exportdaten in GitHub. Öffentlich nur Anzahlen, Größen, CRC-/Prüfsummenstatus,
Buildnummern und öffentliche CI-IDs.

Auch ein positiver Befund beweist nur den DB-Import, nicht die Vollständigkeit der
Historie. `coverage=device-history-unverified` bleibt bis zu beiden Nachweisen gültig.
Keine Instrumentationstests auf physischen Geräten.
