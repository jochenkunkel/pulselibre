# S10 MLR-Transferprüfung (2026-11-22)

## Ergebnis

Die neue passive HCI-Aufzeichnung belegt FileAccess-List/Pull-Protokoll, aber
keinen bis zu FIT-Dateibytes rekonstruierbaren Transfer. Es wurde deshalb kein
Produktcode geändert.

## Offline-Prüfung

- Quelle: privater lokaler Mitschnitt `/tmp/opencode/private_s10_20261122/`;
  Rohdaten, Kennungen und Nutzdaten bleiben unveröffentlicht.
- HCI-Validator: 15.968 Records, 7.376 ACL-Records und 6.940 vollständige
  ATT-Kandidaten.
- Zeitachsenbasierte Auswertung der letzten CIRQA/FileAccess-Verbindung:
  sechs `PullItemDataResponse`-Ergebnisse mit `result=0` und sechs
  `SetFlags`-Ergebnisse mit `result=0`.
- Sequenzbewusste MLR-Reassemblierung ergibt CRC-valide GFDI-Streams mit
  FileAccess-Protobuf (`5043`/`5044`). **Korrektur nach Abgleich mit PR #413:**
  21 gzip-Streams in großen 5044-Frames dekomprimieren zu Wettervorhersage-JSON
  (u. a. Wetter-/Standortfelder), nicht zu FIT-Dateien. Damit sind
  gzip-komprimierte JSON-Anwendungsdaten belegt; eine Zuordnung dieser Daten zu
  einzelnen Item-Pulls oder FIT-Dateien ist es nicht.
- Weder in den GFDI-Frames noch nach gzip-Dekompression wurde eine FIT-Signatur
  gefunden. Für eine FIT-Datei bleiben Größe, FIT-CRC und Import unbelegt.
- Die beobachteten großen ATT-Notifications sind segmentierte MLR-Werte und
  für sich kein Beleg für FIT-Bytes. Eine frühe COBS-Routebyte-Hypothese wurde
  bei der kanal-/sequenzbewussten Reassemblierung nicht bestätigt.
- In der Aufzeichnung wurde kein Classic/RFCOMM-Datenstrom identifiziert.

## Konsequenz

`result=0` bleibt nur ein erfolgreicher FileAccess-Anwendungsstatus. Die
beobachteten gzip-JSON-Daten sind kein Nachweis für vollständige FIT-Bytes,
FIT-Integrität oder Import.
Keine reproduzierbare Produktabweichung gegenüber der beobachteten
Garmin-Connect-Sequenz ist belegt. Ein aktiver Folgetest ist nicht Teil dieser
Offline-Aufgabe und benötigt gegebenenfalls eine gesonderte Freigabe.

`coverage=device-history-unverified` bleibt unverändert. Die lokale
Ergebniszusammenfassung und Rohaufzeichnung sind nicht für Git bestimmt.
