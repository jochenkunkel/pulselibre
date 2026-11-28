# Positionsspur zur Band-Aktivität (#788)

Stand 28.11.2026, Build 2639 und Nachfolger.

## Zweck
Während am CIRQA-Band eine Aktivität läuft, speichert PulsLibre die Position des Telefons. Pro Aktivität lässt sich eine GeoJSON-Datei mit Spur und Gesundheitswerten exportieren. Der Vergleich gleicher Strecken ist ein eigenes, späteres Thema.

## Bedienung
- **Einstellungen → Aufzeichnung → „GPS Spur zu aktiver Tätigkeit“.** Standard: aus. Die Standortfreigabe wird erst nach dem Einschalten angefragt; „ungefähr“ wird akzeptiert.
- **Start:** automatisch, sobald PulsLibre eine laufende Band-Aktivität kennt. Erkannt wird sie bei jedem Historien-Download; der automatische Download beim Öffnen der App genügt.
- **Ende:**
  - sofort, wenn die Aktivität per App beendet wird („Aktivität beenden und speichern“);
  - sonst beim nächsten Download bzw. Öffnen der App, sobald das Band keine laufende Aktivität mehr meldet;
  - spätestens 6 h nach der letzten Meldung „läuft“.
- **Export:** Gerät-Tab → „Spur exportieren (GeoJSON)“ → Aktivität nach Start/Ende auswählen → Speicherort über Android (SAF).

## Technik
- **Dienst:** eigener Foreground-Service `PositionTrackingService`, Typ `location`, mit sichtbarer Benachrichtigung. Der CIRQA-Recorder bleibt `connectedDevice`.
- **Quellen:** `LocationManager` mit GPS, Netzwerk und (ab API 31) fused, je 1 s angefragt.
- **Speicherung:** alle 5 s der genaueste Punkt des Fensters (Owner-Vorgabe 28.11.2026). Dazu Genauigkeit, Provider, Höhe, Tempo und Richtung, soweit Android sie liefert.
- **Datenhaltung:** eigene App-private Datenbank `position_tracks.db` (Tabellen `sessions`, `points`). Ungültige Koordinaten (NaN, außerhalb des Wertebereichs, 0/0) werden verworfen, Duplikate (Zeit + Provider) ignoriert. Nach einem Prozessabbruch wird eine offene Sitzung beim nächsten Start am letzten Punkt geschlossen.
- **Bandzustand:** Smart 30.3 (siehe `docs/protocol/research/ISSUE737_ACTIVITY_STATUS_PROTOCOL_2026-11-28.md`). Als laufend gilt jeder Code außer 4 (gespeichert) und „kein Zustand“. Beobachtet: 3 = läuft, 9 = läuft (Geh-Aktivität, S10 28.11.2026), 1 = angehalten, 5 = wird gespeichert.

## GeoJSON-Format
`FeatureCollection` nach RFC 7946, Koordinaten `[Länge, Breite]` (WGS84).

- **Spur-Feature** (`kind: track`):
  - Geometrie: `LineString`; bei genau einem Punkt `Point`, ohne Punkte entfällt das Feature.
  - Eigenschaften:
    - `sessionId`, `startMs`, `endMs`, `points`
    - `distanceMeters` (Haversine), `durationSeconds`, `averageSpeedMps`
    - `averageHeartRateBpm`, `maxHeartRateBpm`
- **Ein Punkt-Feature je Messung** (`kind: position`), Eigenschaften:
  - `timeMs`, `time`
  - `accuracyMeters`, `provider`, `altitudeMeters`, `speedMps`, `bearingDeg`
  - `heartRateBpm`, `spo2Percent`
- **Gesundheitswerte:** Es ist der jeweils nächste Messwert aus `unified_measurements` (live und Band), höchstens 10 s entfernt. Es wird nichts interpoliert. Fehlende Werte fehlen auch im Export.

## Datenschutz
- Keine INTERNET-Berechtigung, kein Upload.
- Standortdaten erscheinen weder in Logs noch in Tests oder im Repository; Tests verwenden synthetische Koordinaten.
- Export nur auf ausdrücklichen Wunsch.

## Grenzen
- **Erkennung nur beim Download:** Die laufende Aktivität wird bisher nur bei einem Historien-Download erkannt. Ein Abonnement des Status in der Live-Verbindung erfordert den vollständigen GFDI-Aufbau (5024/5050/Sync) und wird erst nach einem Gerätetest umgesetzt.
- **Display aus:** Android kann bei ausgeschaltetem Display weniger Positionen liefern (Energiesparregeln des Herstellers).
