# Rohmitschnitt: vollständiger Garmin-Connect-Sync am S10 (2026-11-22)

## Freigabe und Umfang

Der Nutzer hat **am 2026-11-22** ausdrücklich freigegeben, die Rohinhalte dieser Aufzeichnung im Git-Repository bereitzustellen, mit der Begründung: „es sind dummydate!!!!! ich gebe es frei". Diese Freigabe gilt **konkret für diesen Test** und ersetzt die sonstige Regel „keine Roh-BLE-Payloads / Geräteadressen / Gesundheitsrohdaten in GitHub" ausschließlich für die hier abgelegten Dateien. Sie erweitert **keine** Geräte-Sicherheitsfreigabe und gilt nicht für andere Mitschnitte.

- Auslöser: Nutzer bat um einen kompletten Garmin-Connect-Sync mit lückenlosem Logging, aktivierte Bluetooth am S10 und löste den Sync selbst in der App aus. Agent war rein passiv (HCI-Snoop + Logcat), keine eigenen BLE-/GATT-/FileSync-Kommandos.
- Aufzeichnung (UTC): 2026-11-22 16:50:16 – 16:53:04.
- Gerät: Samsung Galaxy S10 (SM-G970F, Android 12), Garmin Connect 5.31.1.

## Dateien

| Datei | Bytes | SHA-256 |
|---|---:|---|
| `btsnoop_hci.log` | 5.522.737 | `6cc31e79112f94dd3ea1c146de8bc48d74fd2252cf6290805cb83e4226d8adf8` |
| `logcat-all.txt` | 1.489.144 | `f6109178543b6edd39e983298c30672d15f2375c92e1243ab6d335e6e421231b` |

- `btsnoop_hci.log`: aus lokalem Bugreport extrahiert (`FS/data/log/bt/btsnoop_hci.log`), Datalink 1002.
- `logcat-all.txt`: `adb logcat -b all -v threadtime`, 11.562 Zeilen.

## Ehrlicher Hinweis zu möglichen sensitiven Inhalten

Vor der Veröffentlichung wurde **nur zählend** geprüft (keine Inhalte in diesem README):
- Logcat: 3 Zeilen mit `password`, 78 Zeilen mit `token` (Bezeichner, nicht zwingend Geheimnisse).
- HCI-Log: 178 HTTP-URL-ähnliche Byte-Folgen; 0 E-Mail-ähnliche Folgen.

Der Nutzer bestätigt, dass es sich um Dummy-Daten handelt. Wer die Dateien weiterverwendet, sollte sie dennoch als potentiellidenteitätsrelevant behandeln. Es wurden **keine** Inhalte, Geräteadressen oder Tokens in Dokumentationsdateien übernommen.

## Auswertung

Metadatenbasierter Befund mit `tools/btsnoop_summary.py` (8/8 Python-Tests grün):
85.154 Records, 69.470 ACL, 36.714 vollständige ATT-Kandidaten, 4.618 Commands, 11.066 Events.
Zuordnung und Protokollsemantik sind daraus **nicht** belegt; `coverage=device-history-unverified` bleibt unverändert. Siehe `../CIRQA_S10_GARMIN_FULL_SYNC_LOG_2026-11-22.md`.
