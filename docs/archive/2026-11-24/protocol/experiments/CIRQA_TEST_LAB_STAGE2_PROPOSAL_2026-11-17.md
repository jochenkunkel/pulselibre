# CIRQA-Testlabor – Stufe-2-Vorschlag (nicht freigegeben)

Stand: 2026-11-17 · Scope gemäß Issue #20. Dieses Dokument ist ein Prüfplan, **keine Freigabe** und keine App-Funktion. Das Testlabor bietet keinen FileSync-Button.

## Stufe 1 im Testlabor

- Passiver, zeitlich begrenzter BLE-Scan; nur Zähler und begrenzte RSSI-Stichprobe, Adressen nur flüchtig zum Bond-Abgleich.
- Androids aktueller GATT-Status und begrenzte 30-Sekunden-Beobachtung, ohne selbst eine Verbindung herzustellen.
- Vorhandene `CirqaGattProbe`: temporäre Verbindung und Dienst-/Characteristic-Erkennung, ohne GATT-Lese-/Schreiboperationen oder Notification-Abonnement.
- Vorhandene Puls-/Schritte-Liveprobe kann nur nach einer eigenen Bestätigungsdialogaktion aufgerufen werden. Sie führt ihren bestehenden GFDI-/Live-Messwertpfad aus und speichert empfangene echte Messwerte wie bisher lokal. Es wurde kein neues Protokollkommando dafür implementiert.
- SAF-FIT-Prüfung verwendet `FitFileHeader.inspectVerified` und `FitHistoricalObservations.decode`; maximal 8 MiB Datei und 50.000 Beobachtungen. Kein Dateikopie-, Gesundheitsdatenbank- oder Cloud-Import.
- Der Standard-Testbericht enthält keine Adresse, Gerätenamen, Dateinamen, Rohpayloads oder Messwerte. Der Dateidialog warnt, dass Android auch Cloud-Ziele anbieten kann.
- Ein passiver CIRQA-Rohframe-Mitschnitt ist im aktuellen Code nicht vorhanden. Das Labor behauptet nicht, dass vorhandene SQLite-Livewerte passive Protokollframes seien.

## Stufe 2 – Kandidat, derzeit ausdrücklich NICHT ausführbar

**Fragestellung:** Unterstützt CIRQA den von Garmin Connect 5.31 nachgewiesenen Legacy-GFDI-Verzeichnisfilter 5007?

| Anforderung | Bekannter Stand |
|---|---|
| Kandidat | Ein einzelner GFDI-5007-Request mit Connect-Filter `PENDING_UPLOADS_ONLY`; Connect-Body ist exakt ein Byte `03`. |
| Semantik | Connect behandelt 5007 als Directory-Filter. Diese Semantik ist nicht für CIRQA bestätigt. |
| Erwartung | Connect erwartet mindestens ein Statusbyte; `00` bedeutet „unterstützt“, andere Werte „nicht unterstützt“. CIRQA-Antwortformat und Timeout sind unbekannt. |
| Mögliche Nebenwirkungen | Wechselwirkung mit einer laufenden Garmin-Synchronisation; unbekanntes CIRQA-Verhalten. Ein Filter kann weitere Dateisync-Pfade anstoßen. Ein späterer Connect-Uploadpfad archiviert Dateien nach Upload; das Verhalten darf nicht auf CIRQA übertragen oder ausgelöst werden. |
| Abbruchregel für einen später separat freizugebenden Versuch | Höchstens ein Request, keine Wiederholung. Keine Folgeanfrage 5031/5002/5004, keine Interpretation unbekannter Bytes, sofortiger Abbruch bei unerwarteter Antwort, Verbindungsfehler oder negativem Status. Keine Dateiübertragung und kein Archivieren. |
| Erforderliche Vorbereitung | CIRQA-spezifische passive Evidenz für 5007 und unterstützte Legacy-GFDI-Fähigkeit; exakte CIRQA-äußere GFDI/GATT-Requestserialisierung samt CRC; Review möglicher Sync-Seiteneffekte; dokumentierter Geräte-/Datenschutzplan. Garmin Connect beenden; nur ausdrücklicher Nutzerauftrag für genau diesen Plan. |
| Maximalversuche | Vorgeschlagener Grenzwert: **1**. Der Versuch wurde nicht genehmigt und nicht ausgeführt. |

**Blockierender Byte-Gap:** Der Body `03` ist aus Connect-Code rekonstruiert. Die exakten äußeren CIRQA-Frame-/GATT-Bytes und die CIRQA-Kompatibilität sind nicht belegt. Daher gibt es hier bewusst weder eine angeblich „exakte“ vollständige Requestfolge noch ausführbaren Code. Die Sicherheitsgrenze aus Issue #20 bleibt maßgeblich: 5007/5031/5002/5004 werden in PulsLibre nur dokumentiert/offline behandelt. Für einen späteren Gerätetest ist ein neuer Auftrag mit expliziter Freigabe des vervollständigten Plans erforderlich.

## Gesperrte Pfade

- Kein FileSync-/Historien-Download, keine 5037-Sync-Anfrage und keine Aufrufe von 5007/5031/5002/5004 aus der App.
- `0x8A39` wird weder implementiert noch wiederholt.
- Keine Archivierung, Löschung, Zurücksetzung, Firmware- oder Flagänderung.
- Kein automatischer Versuch, kein Capability-Fuzzing und keine geräteübergreifende Annahme.
