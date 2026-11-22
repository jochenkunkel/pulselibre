# S10-P0: Evidenzplan für den fehlenden historischen Download-Nachweis (2026-11-22)

**Status: Plan, nicht ausgeführt. Keine Geräteaktion, kein Mitschnitt und keine Freigabe in diesem Dokument.**

Dieser Plan hält fest, welche Evidenz für das P0-Ziel „vollständiger, überprüfbarer historischer Datendownload vom CIRQA-Band“ fehlt und wie sie – nach ausdrücklicher Freigabe – datenschutzkonform beschafft und geprüft werden kann. Er ersetzt keine Nutzerfreigabe und ändert keine S25-Gates.

## 1. Gesicherter Stand (offline, 2026-11-22)

- Drei S10-HCI-Mitschnitte und ein isoliertes reines Garmin-Connect-Sync-Fenster wurden offline dekodiert. Richtung über ATT-Opcodes belegt: `0x52` = App → Band, `0x1b` = Band → App.
- Garmin-Connect-Folge: 5024/5050 vor den FileAccess-Frames; fünf `item_list`-Seiten mit `items=1/0/0/0/0`, `status=0`, drei anschließende Folgeseiten mit unverändertem `next_transaction_id`; danach Stopp des Referenzclients.
- Protobuf-Pulls sind vorhanden: `5043` / Service-Feld 1 = `pull_item_request` (App → Band), `5044` / Service-Feld 2 = `pull_item_response` (Band → App), dekodierte Responses mit `resultCode=0`, `transport=0`. Legacy-`5002/5004/5011/5012` fehlen auf allen GFDI-Kanälen 0–7.
- Produktcode: PR #410 (gemergt als `2d2a0b1`) begrenzt die Folgeseiten auf die drei beobachteten Token-Wiederholungen; Fail-closed-Stopp und `coverage=device-history-unverified` bleiben. PR #411 (gemergt als `f095909`) ergänzt einen synthetischen Offline-End-to-End-Test. Beide Branch-Heads und die anschließenden `main`-Heads hatten grüne volle CI inkl. API-35-Emulator (`44961826510`, `44963010456`, `44962840860`, `44963900865`).
- Siehe [Gap-Matrix](CIRQA_S10_P0_GAP_MATRIX_2026-11-22.md), [GFDI-Reihenfolge](CIRQA_S10_GARMIN_GFDI_SEQUENCE_2026-11-22.md) und [redaktierte Sync-Mitschrift](CIRQA_S10_GARMIN_PURE_SYNC_HCI_2026-11-22.md).

## 2. Präzise Evidenzlücke

1. Die FIT-Bytes selbst sind in den vorliegenden HCI-Logs **nicht als eigener Kanal rekonstruierbar**; `resultCode=0` belegt nur die successful beantwortete Pull-Anfrage, nicht den vollständigen Dateiinhalt.
2. Die klassische/`RFCOMM`-Transportseite wurde in den S10-Aufzeichnungen **nicht mitgeschrieben**. Der historische Befund vom 18.11.2026 lautet außerdem, dass RFCOMM für dieses Gerät nicht gangbar ist (erster begrenzter RFCOMM-V1-Versuch erhielt keine Gegenverbindung). Ein RFCOMM-Mitschnitt darf daher **nicht** als bereits verfügbar oder ohne Weiteres machbar angenommen werden.
3. Es gibt keinen Nachweis, dass PulsLibre – nicht nur Garmin Connect – die fehlenden historischen Daten vollständig ladet, dedupliziert, zeitlich korrekt zuordnet, integritätsgeprüft und mit Provenienz speichert.

Solange diese Punkte offen sind, bleibt `coverage=device-history-unverified` verbindlich. Aus den leeren Folgeseiten darf keine vollständige Historie abgeleitet werden.

## 3. Kandidaten für den nächsten Schritt (nach Freigabe)

### Variante A – passive, redaktierte Erweiterung der Mitschnitte (bevorzugt, wenn technisch verfügbar)

- **Ziel:** Eine während eines bereits stattfindenden, regulären Garmin-Connect-Syncs entstehende Aufzeichnung, die zusätzlich die klassische Transport-/Dateitransportseite oder eine gleichwertige, die FIT-Bytes verknüpfende Schicht enthält.
- **Voraussetzung:** Ausdrückliche, konkret auf diesen Mitschnitt bezogene Nutzerfreigabe. Keine neuen BLE-Kommandos, kein Scan, kein Pull, kein Reset, kein Entkoppeln.
- **Vorab offline zu klären (ohne Gerät):** Ob unter den bereits vorhandenen privaten Artefakten eine Aufzeichnung mit Classic-/L2CAP-/RFCOMM-Anteil existiert, die bisher nicht ausgewertet wurde. Falls nein, ist Variante A ohne Freigabe nicht ausführbar.
- **Akzeptanz:** Für mindestens eine Datei ist die Kette `ItemReference` → `pull_item_request` → `pull_item_response` → übertragene FIT-Bytes → CRC/Größe → lokaler Import nachvollziehbar; keine identifizierenden Werte in GitHub.

### Variante B – explizit autorisierter, begrenzter PulsLibre-Gerätetest

- **Ziel:** Dieselbe Kette im Produktcode nachweisen, inklusive Datei-/Datensatzanzahl, Dedupe, Zeitstempeln, Integrität, Vollständigkeit und Provenienz.
- **Voraussetzung:** Gesonderte Nutzerfreigabe für genau definierte aktive Befehle/Sequenzen; unveränderte No-Pull-/S25-Gates; exklusive Gerätesperre nach `AGENTS.md`; keine Instrumentationstests auf dem S25.
- **Stop-Kriterien:** Jeder Schreibzugriff aufs Band, Reset, Entkoppeln, `CLOSE_ALL`, Firmwareaktion, destruktives FileSync-Kommando, Löschen/Überschreiben Nutzerdaten oder jede unerwartete Geräteantwort → sofortiger Abbruch und Dokumentation.

## 4. Datenschutz und Beweisführung

- Rohmitschnitte, Geräteadressen, Tokens, UIDs, Roh-Payloads und Gesundheitswerte bleiben ausschließlich lokal in einem 0700/0600-Bereich; öffentlich werden nur Zählungen, Protokollcodes, Hashes und synthetische Fixtures.
- Jede neue Erkenntnis wird als Beobachtung → Codepfad → Ursache/Hypothese → Test → Nachweis dokumentiert; Hypothesen werden nicht als Fakten ausgegeben.
- Ein erfolgreich dekodierter Pull-Result-Code allein ist **kein** Beleg für einen vollständigen Download. Ein grüner Parser-Test allein ist **kein** Beleg für End-to-End-Complete.

## 5. Aktueller nächster Schritt ohne Freigabe

Weiterhin offline: vorhandene private Artefakte auf bislang unausgewertete Classic-/RFCOMM-Anteile prüfen (nur Inventur/Hashes/Recordtypen, keine Geräteaktion). Ergibt sich daraus nichts, bleibt der nächste Schritt nach Variante B erst nach ausdrücklicher Freigabe ausführbar und wird nicht eigenmächtig gestartet.
