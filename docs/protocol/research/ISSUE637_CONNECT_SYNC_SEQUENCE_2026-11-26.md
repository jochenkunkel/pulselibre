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
