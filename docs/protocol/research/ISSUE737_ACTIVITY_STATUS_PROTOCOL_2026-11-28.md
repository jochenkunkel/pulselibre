# #737 – Aktivitätsstatus und Beenden per BLE (Referenzversuch 28.11.2026)

Gerät: Samsung S10 (Kennung nicht dokumentiert), CIRQA Firmware 4.2, HCI-Snoop. Der Mitschnitt bleibt lokal. Gezeigt
werden hier nur Nachrichtentypen und Protokollbytes, keine Gesundheitswerte, MACs oder Seriennummern.

## Versuch A – PulsLibre-Live-Verbindung (passiv)

Ruhe 10:32–10:34, Aktivität am Band 10:34–ca. 10:45, danach Ruhe.

- GFDI: Das Band sendet in allen Phasen nur 5024 (54 Byte, alle 5–8 s), mit identischem Inhalt (gleicher Hash).
- ML-Live-Dienste 6/7/8/10/12/13/19/20/21: Nur Schritte (7) ändern sich durch Bewegung.
- **Folgerung:** Ohne Anfrage meldet das Band keinen Aktivitätsstatus. PulsLibre beantwortet 5024 im Live-Modus nicht.

## Versuch B – Garmin Connect, Aktivität läuft

Aktivität am Band gestartet, dann Connect geöffnet. Connect zeigte „Aktivität läuft“ und bot „Aktivität speichern und
beenden“ an; der Knopf wurde um 11:48:14 betätigt.

| Zeit | Richtung | Nachricht (Smart-Feld.Unterfeld) | Inhalt (protobuf) | Deutung |
|---|---|---|---|---|
| 11:47:21 | Host → Band | 30.1 | `0a 02 08 13` | Aktivitätsstatus abonnieren (Wert 0x13) |
| 11:47:21 | Band → Host | 30.2 | `0a 06 08 00 12 02 08 13` | Bestätigung |
| 11:47:21 | Band → Host | 30.3 | `0a 01 13 a2 3f 04 12 02 08 03` | Zustand **3** |
| ab 11:47:23 | Band → Host | 30.3, jede Sekunde | `0a 01 13 a2 3f 5d 1a 5b …` | Echtzeitwerte der laufenden Aktivität |
| 11:48:14 | Host → Band | **56.17** | `10 02` | Befehl 2 |
| 11:48:14 | Band → Host | 56.18 | leer | Quittung |
| 11:48:14 | Band → Host | 30.3 | `… 12 02 08 01` | Zustand **1** |
| 11:48:15 | Host → Band | **56.17** | `10 03` | Befehl 3 |
| 11:48:15 | Band → Host | 56.18 | leer | Quittung |
| 11:48:15 | Band → Host | 30.3 | `… 12 02 08 05` | Zustand **5** |
| 11:48:16 | Band → Host | 30.3 | `… 06 12 04 08 04 10 00` | Zustand **4** |

Der Zustand steht in 30.3 → Feld 1012 (Tag `a2 3f`) → Feld 2 → Feld 1.

Vorläufige Zuordnung, aus **einem** Lauf und deshalb noch nicht bestätigt:
- Zustände: 3 = läuft, 1 = angehalten, 5 = wird gespeichert, 4 = gespeichert/bereit.
- Befehle in 56.17 Feld 2: 2 = anhalten, 3 = speichern.

Ergebnis am Gerät: Die Aktivität wurde gespeichert. Connect hat sie danach als FIT-Typ 4 abgeholt.

## Nebenwirkungen von Connect in diesem Lauf (2½ min)

- Connect hat 8 × FIT-Typ 4 und 1 × Activity_AutoDetect abgeholt und mit set_flags markiert.
- **Keine** Tagesdateien (Typ 32) wurden abgeholt oder markiert.
- Vorher wurde ein geprüftes PulsLibre-Backup erstellt; Connect wurde sofort danach gestoppt.

## Nebenbefund – Pull nach Aktivitätsende

Direkt nach dem Ende der Aktivität um 10:45 lehnte das Band alle Datei-Pulls mit result=3 ab. Rund 15 Minuten später
liefen 20/20 Dateien. PR #781 wiederholt den Abruf deshalb einmal nach 5 Minuten.

## Offen vor der Umsetzung in PulsLibre

1. Zustandscodes und Befehle in einem zweiten Lauf bestätigen. Dabei auch „verwerfen“ beobachten (in Connect vermutlich ein anderer Wert).
2. Klären, ob 30.1 ohne die übrige Connect-Startsequenz (5050/5026/56.1) beantwortet wird.
3. Erst danach: Status anzeigen (nur Abonnement, nichtdestruktiv) und Beenden mit Bestätigung durch den Nutzer.
