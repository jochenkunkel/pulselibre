# CIRQA 5050 / 5011: Rohframe-Strukturanalyse (17.11.2026)

## Quelle und Datenschutz

Manuell vom Nutzer übermittelter, CRC-validierter Bootstrap-Rohframe-Test. Dieses Dokument enthält **keine** vollständigen Hex-Payloads, da mögliche gerätebezogene Kennungen im 5050-Body noch nicht ausgeschlossen sind.

## Verifizierte Struktur

- Insgesamt 5 GFDI-Frames, alle CRC OK; 5024 einmal, 5050 dreimal, 5011 einmal.
- Jedes 5050-Frame umfasst 25 Bytes: 2-Byte-Längenfeld (`0x0019`, little-endian = 25), 2-Byte-GFDI-Typ (`0x13ba` = 5050), 19-Byte-Body und 2-Byte-CRC. Die drei 5050-Frames sind bytegenau identisch.
- Jedes beobachtete 5011-Frame umfasst 27 Bytes: 2-Byte-Längenfeld (`0x001b`, little-endian = 27), 2-Byte-Typ (`0x1393` = 5011), 21-Byte-Body und 2-Byte-CRC.
- Relative Empfangszeiten: 5024 +708 ms; 5050 +767, +5764, +10775 ms; 5011 +15785 ms.
- 5050-Abstände: 4997 und 5011 ms; Abstand letzter 5050 bis 5011: 5010 ms.
- Minimaler 5024-ACK-Kandidat einmal gesendet, GATT bestätigt; **keine** nachgewiesene GFDI-Anwendungsbestätigung. Kein 5050-ACK gesendet.
- Keine Dateiliste, kein FileSync, kein bestätigter Historienzugriff.

## Interpretation

Der exakt gleiche 5050-Body mit ~5-s-Abständen ist mit Retry/Timeout vereinbar. Dass 5011 etwa fünf Sekunden nach der dritten 5050-Nachricht folgt, könnte auf einen weiteren Timeout-/Abbruchschritt hindeuten, ist aber **nicht** semantisch bewiesen. Ohne CIRQA-spezifische Referenz oder Vergleich mit einer nachweislich funktionierenden Gegenstelle dürfen weder die 19 Body-Bytes als Identität/Nonce/Challenge bezeichnet noch Antwortbytes abgeleitet werden.

## Nächste sichere Schritte

1. Offline CIRQA-/GFDI-Nachrichtentypen 5050 und 5011 mit dokumentierten bzw. bekannten Protokollimplementierungen vergleichen.
2. Die 5050-Body-Bytes **nur lokal** auf Feldgrenzen, mögliche Zähler und Kennungen untersuchen; keine ungeprüften vollständigen Hexframes in öffentlichen Commits.
3. Falls nötig, mit expliziter Nutzerzustimmung einen weiteren begrenzten Gerätetest durchführen; keine geratenen 5050-Antworten senden.
