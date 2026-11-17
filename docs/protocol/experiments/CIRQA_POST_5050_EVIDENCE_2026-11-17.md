# CIRQA: Beobachtung nach 5050-Schreibbestätigung (2026-11-17)

## Befund aus dem Gerätetest

Der vom Nutzer übermittelte Bericht enthält eine positive GFDI-5000-Statusantwort `request=5050/status=ACK`, eine bestätigte GATT-Schreibfolge für den aktiven 5050-Test und **keine** FileListResponse. Das Status-ACK beweist weder, dass die 15-Byte-Nullmaske vollständig akzeptiert wurde, noch dass Historienzugriff verfügbar ist. Der passive Decoder bezeichnet mit `5050-Antwort=NICHT_GESENDET` nur seinen eigenen Sendestatus.

## Implementierung

`Cirqa5050PostWriteEvidence` nimmt beim Abschluss der aktiven GATT-5050-Schreibfolge einen Snapshot der bisher empfangenen GFDI-Nachrichtentypen und zählt ausschließlich danach eingetroffene, CRC-validierte Frames. Das Ergebnis erscheint im bestehenden Diagnosebericht; keine weiteren GATT-Befehle, kein automatischer Dateizugriff. Unit-Tests decken die Abgrenzung von Frames vor/nach dem Snapshot und die fehlende Historienfreigabe ab.

## Offene Grenzen

- Die Reihenfolge von GATT-Schreibcallbacks und Notifications ist kein kryptografischer Kausalitätsnachweis. Auch ein späteres ACK kann sich auf frühere Vorgänge beziehen.
- Die neuen Zähler erlauben keinen Rückschluss auf die Bedeutung unbekannter GFDI-Typen.
- Ein weiterer Gerätetest ist erst sinnvoll, nachdem die Android-CI-Version mit diesem Beobachter erfolgreich gebaut wurde.
