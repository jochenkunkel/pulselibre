# #637 – Garmin Connect gegen PulsLibre: Startsequenz, Datenanforderungen und ACKs (26.11.2026)

## Datengrundlage
- Privater HCI-Mitschnitt eines Garmin-Connect-Syncs vom S10 (21./22.11.2026, 4 Verbindungen, ca. 50 min).
- Lokal und offline vollständig rekonstruiert über alle Schichten: ACL/L2CAP → ATT → MLR (2-Byte-Header) → COBS → GFDI mit CRC → 5043/5044-Protobuf.
  - 1 690 CRC-valide GFDI-Frames.
  - Alle 5043/5044-Nachrichten wurden reassembliert.
- Im Repository stehen weder Mitschnitt noch UIDs, Flag-UUIDs oder Werte. Hier sind nur Feldnummern, Typlabels und Zählungen dokumentiert.

## 1. Startsequenz (Connect, Verbindung 1)
```
←5024 →ACK ←5050 →ACK →5050 ←ACK →5043:Smart13(core) ←5044:Smart13 →5031 →5043:Smart30 ←5044:Smart30
→5043:Smart8 ←5044:Smart8 →5026 →5030 ←5052 ←5043:{21,21,21,16,13,24,10,42,13,86,13,2,2} ←5036 …
←5043:Smart43.12 (FileAccess-Push) …
```
- PulsLibre bildet den Bootstrap aus 5024/5050/5043-sync_request/5031/5030 bereits nach. Seit dem Lauf vom 26.11. ist er mit 319 ms belegt.
- Unterschied: Connect bedient zusätzlich Smart-Dienste (8, 21, 30, 42, 47, 49, 86). PulsLibre beantwortet nur, was für FileAccess nötig ist.

## 2. Datenanforderungen (FileAccess, Smart 43) – der entscheidende Unterschied

| Schritt | Garmin Connect (Mitschnitt) | PulsLibre |
|---|---|---|
| Neue Datei | **Das Band schickt sie selbst:** `FA 12` (ItemReference + Transaktion), sobald eine Monitoring-, Schlaf- oder Metrik-Datei entsteht. Beobachtet: 2× FIT_TYPE_32, 1× FIT_TYPE_49, 15× FIT_TYPE_44, 1× FIT_TYPE_72. Connect zieht sie sofort. | ignoriert `FA 12` |
| Liste | `ItemListRequest{2=letzte Transaktion, 4=exclude_all_flags(UUID), 5=get_flags(UUID)}`, danach `{2, 5, 6=included_data_types}`. Ergebnis ist **nur das Delta**, also alles ohne Connects Flag. | leere Anfrage, also das ungefilterte Inventar |
| Inhalt des Deltas | FIT_TYPE_44 ×15 und FIT_TYPE_32 ×1 (Verbindung 1), später FIT_TYPE_49 und 72 | Am 25.11. kamen 2× monitoring_b. Am 26.11. kamen nur Typ 4/5/1/72/74 und 2 Nicht-FIT-Dateien. |
| Abruf | `PullItemDataRequest` nur mit UID, Fenster 15 | gleich |
| Transfer fertig | Das Band meldet `FA 5{uid, handle}`, Connect quittiert mit `5044 FA 21{1=24}` | keine FA-21-Quittung (nur GFDI-5000-ACK) |
| Nach jedem Abruf | **`set_flags_request` (FA 15)** mit Connects Flag-UUID, Antwort `FA 16{result=0}` und danach `FA 17` | sendet keine Flags (bewusst, Owner-Regel: keine zustandsverändernden Befehle) |

Insgesamt hat Connect in diesem Mitschnitt **52 Dateien abgerufen und 52-mal sein Flag gesetzt**.

## 3. Was danach mit den Dateien passiert
- **BEWIESEN (Mitschnitt):** Nach dem Flag-Setzen fehlen FIT_TYPE_44 und 32 in Connects späterer Liste ohne Transaktionsfilter (`{4,5}`). Diese Liste enthält nur noch Typ 5, 1 und eine Nicht-FIT-Datei.
- **BEWIESEN (ItemReference Feld 6):** Monitoring-, Schlaf- und Metrik-Dateien (32/44/49) tragen 6=1; Aktivitäts-, Workout- und Gerätedateien (4/5/1/72) tragen 6=2.
- **BEWIESEN (PulsLibre-Gerätelauf):** Die monitoring_b-Dateien vom 25.11. standen am 26.11. **nicht mehr** im ungefilterten Inventar, nachdem Connect dazwischen synchronisiert hatte (Owner-Angabe). Activity-Dateien bis zurück zum 15.11. stehen weiter darin.
- **PLAUSIBEL:** Feld 6=1 kennzeichnet kurzlebige Dateien. Das Band löscht Tageshistorie-Dateien, sobald ein Sync-Agent (Connect) sie mit seinem Flag als übertragen markiert hat. Aktivitäten (6=2) bleiben erhalten.

## 4. Folgen für PulsLibre (Abnahme a–e)
- **(a)** Tageshistorie (Stress, SpO₂, Atmung, Ganztagespuls, Schlaf) steckt in Typ 32/44/49. Das Band hält diese Dateien nur, **bis Connect sie abgeholt hat**. Aktivitäten bleiben.
- **(b)** Bootstrap stimmt überein. Bei den Quittungen fehlt PulsLibre `FA 21` auf `FA 5`. Die Wirkung ist unbelegt: Transfers gelingen auch ohne diese Quittung (29/29).
- **(c)** Belegte Unterschiede:
  - Connect reagiert auf Push (`FA 12`).
  - Connect fragt das Delta mit Flag-Filter ab.
  - Connect setzt nach jedem Abruf sein Flag.
  - PulsLibre fragt das ungefilterte Inventar ab und lässt den Zustand des Bandes unverändert.
- **(d)** Nötige Änderungen. Kein PulsLibre-Befehl kann Dateien zurückholen, die das Band nach einem Connect-Sync bereits verworfen hat. Wirksam sind nur:
  1. PulsLibre zieht Tagesdateien **vor** Connect, entweder mit automatischem Abruf nach dem Verbinden oder durch Reaktion auf `FA 12`.
  2. Connect pausiert, wenn PulsLibre die Tageshistorie vollständig haben soll.
  - Flags setzt PulsLibre weiterhin nicht. Sonst würde PulsLibre selbst das Löschen auslösen und Connect diese Daten entziehen.
- **(e)** Offen sind drei Punkte:
  - Ob das Band Dateien wirklich löscht oder nur ausblendet. Klären würde das eine Liste mit `get_flags` ohne Ausschluss.
  - Was 24/20 in `FA 21` bedeuten.
  - Die Bedeutung von `FA 3` (Connect lädt Dateien **auf** das Band, z. B. FIT_TYPE_41/39/29/11), `FA 18/19/20` und Smart 86.

## 5. Mitschnitt 24.11.2026: zwei Phasen (21:51 MEZ)

Quelle: privater S10-Mitschnitt `s10e-garmin-sync-2026-11-24`, offline vollständig rekonstruiert. Hier stehen nur Handles, UUIDs, Feldnummern, Typlabels und Zählungen.

### 5.1 Verbindungen und Dienst (BEWIESEN)

| | Phase 1 | Phase 2 |
|---|---|---|
| HCI | LE-Verbindung A, 20:51:00–20:51:54Z | **neue** LE-Verbindung B, 20:51:55–20:52:14Z (derselbe HCI-Handle 64, neu vergeben) |
| Ende | Disconnect 0x16 = *vom lokalen Host beendet*, d. h. Connect hat getrennt | Disconnect 0x16 |
| ATT schreiben / Notification | 0x0015 / 0x0012 (CCCD-Write 0x0013) | 0x001F / 0x001C (CCCD-Write 0x001D) |
| Characteristic-Paar | 2820 / 2810 | 2822 / 2812 |
| Garmin-ML-Dienst | `6a4e2800-…` | derselbe Dienst |

- **Discovery in Phase 2** (Dienst 0x000E–0x001F):
  - 2803 → 0x0010 (Props 0x0E)
  - **2810 → 0x0012 (0x1C: Notify, Write, WriteNoResp)**, **2820 → 0x0015 (0x0C: Write, WriteNoResp)**
  - 2811 → 0x0017, 2821 → 0x001A
  - **2812 → 0x001C, 2822 → 0x001F**
- Phase 1 enthält keine eigene Discovery, weil Connect gecachte Handles benutzt. Über die Discovery aus Phase 2 sind 0x0012/0x0015 aber eindeutig 2810/2820 **desselben Dienstes**: Die GATT-Tabelle des Bandes ist in beiden Verbindungen dieselbe.
- Ergebnis: **Dieselbe Garmin-ML-Instanz, zwei aufeinanderfolgende Verbindungen, je ein anderes RX/TX-Paar.** Es sind keine zwei gleichzeitigen Kanäle und kein anderer Dienst.

### 5.2 ML-Ebene (BEWIESEN)
- Beide Verbindungen beginnen mit ML-`CLOSE_ALL` (Typ 5/6) für **client_id 1**.
- Phase 1 registriert danach GFDI (1), 4, 0x16, 6, 7, 0x1D, 0x0A, 8, 0x14 sowie die MLR-Pipes 0x2018 … 0xE018 rotierend für 15 Transfers.
- Am Ende schließt Connect einzelne Handles (Typ 2/3).
- Phase 2 registriert nur 4, 0x16, GFDI und einmal 0x2018.
- PulsLibre nutzt **client_id 2** und dasselbe Schema: optionales `CLOSE_ALL`, Register GFDI, später 0x2018.

### 5.3 GFDI-Startsequenz: beide Phasen gleich und wie in PulsLibre
```
←5024 →ACK ←5050 →ACK →5050 ←ACK →5043:Smart13(sync_request) ←5044:Smart13 →5043:Smart30 ←5044:Smart30
→5031 (→5043:Smart8 nur Phase 1) →5026 →5030 ←5052 ←5043:{21,21,21,16,13,24,8,10,42,13,86,13} ←5036 …
```

### 5.4 Datenanforderungen (BEWIESEN)

| | Phase 1 (2810/2820) | Phase 2 (2812/2822) |
|---|---|---|
| Push vom Band | `FA 12` FIT_TYPE_32 (neue monitoring_b) | – |
| ItemList | `{4: exclude_all_flags, 5: get_flags}` + `{5, 6: included_data_types}` | identisch |
| Liste | **20 Einträge** (siehe unten) | 6 Einträge: 4× FIT_TYPE_5, 1× FIT_TYPE_1, 1 Nicht-FIT |
| Pulls | **15**: alle Tagesdateien | **1**: der Nicht-FIT-Eintrag, dessen Pull in Phase 1 ohne Flag blieb |
| set_flags | 14 (Flag-UUID von Connect) | 0 |
| Abschluss | `FA 5` → `FA 21{1=24}`, `FA 20`, Close der ML-Handles, Trennung durch Connect | `FA 5` → `FA 21`, `FA 20`, Trennung |

Die 20 Einträge der Phase-1-Liste: 3× FIT_TYPE_32 (monitoring_b), 2× 44, 1× 49 (Schlaf), 1× 68, 1× 73 (Hauttemperatur/397/398), 1× 79, 1× 72, 1× KPI, 3× Activity_AutoDetect, 4× FIT_TYPE_5, 1× FIT_TYPE_1 und 1 Nicht-FIT.

### 5.5 Antworten auf die Fragen
- **Gibt es in Phase 1 eine Anforderung für kontinuierliche Tageshistorie, die PulsLibre nicht ausführt?**
  - Es gibt **keine eigene Daily-History-Anfrage.** Connect holt die Tageshistorie mit derselben FileAccess-Kette, die PulsLibre nutzt: ItemList, dann PullItem.
  - Die Unterschiede sind (1) die Delta-Filter 4/5 mit Connects Flag, (2) die Reaktion auf den Push `FA 12` und (3) **set_flags nach jedem Pull**.
  - Die Tagesdateien (32/44/49/68/73/79, Feld 6=1) stehen in Phase 1 in der Liste, weil Connect sie noch nicht geflaggt hatte. PulsLibres ungefilterte Liste enthält sie ebenfalls, solange sie nicht geflaggt sind; belegt ist das mit 2× monitoring_b am 25.11.
- **Warum nutzt Connect in der zweiten Verbindung 2812/2822?**
  - Belegt ist nur: Connect selbst hat Verbindung A getrennt (0x16) und 1,6 s später neu verbunden.
  - Es ist derselbe ML-Client (client_id 1), und beide Verbindungen beginnen mit `CLOSE_ALL`.
  - Eine Ursache für die Paarwahl zeigt der Mitschnitt nicht. **Plausibel**, aber nicht belegt: Connects ML-Schicht nimmt pro neuer Sitzung ein anderes freies Paar aus 2810–2819, damit verspätete Notifications der alten Sitzung nicht in die neue gelangen.
- **Hat der Wechsel mit der Tageshistorie zu tun?** **Nein.** Die gesamte Tageshistorie lief in Phase 1 über 2810/2820. Phase 2 enthält nur einen Wiederholungs-Pull eines Nicht-FIT-Eintrags. 2812/2822 ist damit **kein** Tageshistorie-Kanal.
- **PulsLibre** wählt in `GarminMlOffline.findCharacteristicPair()` das erste Paar aus 2810–2819. Das ist im Normalfall 2810/2820, wie bei Connect in Phase 1. Der produktive Historienlauf nutzt dieselbe Auswahl (`CirqaMlHandshakeProbe`, Meldung „0x2810–0x2819“).
- **Quittierungen und Sequenzen:**
  - Connect quittiert jedes Geräte-5043 und 5044 auf GFDI-Ebene (5000).
  - PulsLibre quittierte bisher nur 5044. Seit #732 quittiert es auch Geräte-5043 während des Laufs.
  - `FA 21` auf `FA 5` sendet PulsLibre weiterhin nicht, weil die Bedeutung unbelegt ist. Die MLR-Sequenzbytes (2-Byte-Header) sind in beiden Phasen gleich aufgebaut.
