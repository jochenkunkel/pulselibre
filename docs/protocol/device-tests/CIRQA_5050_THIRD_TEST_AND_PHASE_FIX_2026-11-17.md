# CIRQA 5050: dritter Gerätetest und Diagnosezustand (2026-11-17)

## Beobachtung

Der dritte Test bestätigte 5024-ACK und die 5050-Antwortfolge auf GATT-Ebene. Nach deren Bestätigung traf ein einziges GFDI-5000-Statusframe `request=5050/status=ACK` ein. In der verlängerten 10-Sekunden-Beobachtung wurden weder weitere 5050 noch 5011, Dateiliste oder Protobuf erfasst. Der ML-Handle war diesmal 6; er wird pro Verbindung dynamisch vergeben und ist kein Fehler.

## Diagnosefehler

Trotz positivem 5050-ACK blieb `Letzte Phase` auf `GFDI registriert; warte auf DeviceInformation`. Ursache: `GarminMlHandshake.Stage.COMPLETE` wurde bei jeder nachfolgenden Notification erneut behandelt und setzte den Phasentext zurück. Die COMPLETE-Initialisierung wird jetzt für Historien-Diagnosen nur einmal ausgeführt. Dieser Fix ändert keine BLE-Nachrichten.

## Entscheidung

- Kein Nachweis für Historienfreigabe oder einen gestarteten FileSync.
- Die 15-Byte-Nullmaske bewirbt keine Fähigkeiten. Die Hypothese, dass dadurch Sync ausbleibt, ist plausibel, aber **nicht bewiesen**.
- Das Offline-Modul `Cirqa5050OfflineHostCapabilities` kann SYNC-Bit 3 zum Vergleich setzen; diese Option bleibt ausdrücklich **offline** und wird nicht automatisch über BLE gesendet.
- Kein unvalidiertes Aktivieren von SYNC, kein 18-Byte-Versuch, kein ungefragter 5037- oder FileSync-Befehl.
- Nächster Entwicklungsschritt: Capability-Semantik und Garmin-Host-Initiierung anhand unabhängiger Implementierungen klären; danach einen expliziten, begrenzten Test entwerfen.
