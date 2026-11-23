# Rohmitschnitt: Garmin-Connect-Sync am S10 (2026-11-23)

## Freigabe und Herkunft

Der Nutzer hat am 2026-11-23 ausdrücklich verlangt, diesen Mitschnitt **im Original analog den vorhandenen Fixtures** im Git bereitzustellen („kannst du den mittschnitt im original analog der anderen im git bereitstellen"). Ergänzend gilt die Projektentscheidung in [`../../../../collaboration/CURRENT_BAND_RAW_DATA_POLICY.md`](../../../../collaboration/CURRENT_BAND_RAW_DATA_POLICY.md): das Testarmband ist nach Nutzererklärung synthetisch/nicht personenbezogen; die Veröffentlichung erfolgt nach der dort beschriebenen separaten HCI-Prüfung. Die Freigabe gilt **konkret für diesen Mitschnitt**, nicht für andere Aufnahmen, und erweitert keine Geräte-Sicherheitsfreigabe.

- Herkunftskennzeichnung: `synthetic` (Nutzererklärung); agentseitig **nicht** unabhängig verifiziert, daher für die Veröffentlichungsprüfung wie `source-unverified` behandelt.
- Auslöser: Nutzer bat um einen Garmin-Connect-Sync, um zu prüfen, ob das Band den ganzen Tag gespeichert hat. Der Nutzer löste den Sync **selbst** in Garmin Connect aus. Der Agent war passiv (HCI-Snoop + Logcat) und hat **keine** BLE-/GATT-/FileSync-Kommandos gesendet.
- Gerät: Samsung Galaxy S10 (`SM-G970F`, Android 12) über USB; HCI-Snoop `settings put secure bluetooth_hci_log 1` verifiziert, nach dem Sync wieder `0` gelesen. Alle Geräteaktionen liefen im gehaltenen `--device s10`-Lease; danach Status `FREI`.
- Logcat-Fenster: 2026-11-23 22:37:40 UTC bis ca. 22:39 UTC (der Wrapper wurde durch Signal beendet; der vom Nutzer gemeldete Sync lag innerhalb dieses Fensters). Das HCI-Log stammt aus dem anschließend lokal gezogenen Bugreport und ist fortlaufend – es enthält auch Bluetooth-Aktivität außerhalb des Sync-Fensters.

## Dateien

| Datei | Bytes | SHA-256 |
|---|---:|---|
| `btsnoop_hci.log` | 5.102.870 | `dbc473cc54410f3a67cabfebafa374f5b616677e71f117e2864d7e9c97cfa2ba` |

- Format: btsnoop Version 1, Datalink 1002 (Android H4), extrahiert aus `FS/data/log/bt/btsnoop_hci.log` eines privaten Bugreports.
- **Byte-identisch** mit der lokalen Aufnahme; bewusst gegen die `.gitignore`-Regel `*.log` mit `git add -f` versioniert (einmalige Ausnahme für diesen Mitschnitt, keine globale Freischaltung).
- Der zugehörige Bugreport (43.608.334 Bytes, SHA `a5f68b6a453da7eb1555defd59b0bc4dbf16f7ed8dec2f8151fc31d17ae2b0ce`) und `logcat-all.txt` (52.840.295 Bytes, SHA `4ed0c5578d20c07305ace45005158144fb2505e95562d10e99dff017822d78e4`) bleiben aus Datenschutzgründen lokal im 0700/0600-Privatverzeichnis und sind **nicht** Teil dieser Veröffentlichung.

## Ehrlicher Hinweis zur Sensitivitätsprüfung

Vor der Veröffentlichung wurde **nur zählend** geprüft; keine Fundstellen, Inhalte, Geräteadressen oder Tokens sind in diesem README dokumentiert. Verwendete reguläre Ausdrücke: URL `https?://|www\.`; E-Mail `[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}`; `password`; `token`; MAC `(?:[0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}`; seriennummernähnlich `(?<![A-Za-z0-9])[A-Z0-9]{10,}(?![A-Za-z0-9])`.

| Datei | URL-ähnlich | E-Mail-ähnlich | `password` | `token` | MAC-ähnlich | seriennummernähnlich |
|---|---:|---:|---:|---:|---:|---:|
| `btsnoop_hci.log` | 67 | 0 | 0 | 0 | 0 | 67 |
| `logcat-all.txt` (nicht veröffentlicht) | 238 | 321 | 375 | 2.274 | 543 | 114.767 |

Die Zähler für den Logcat sind deutlich höher, weil dieser auch System- und App-Logs aus allen Puffern enthält; deshalb wurde er **ausgelassen**. Auch die veröffentlichte HCI-Datei sollte als potentiell identitätsrelevant behandelt werden; bei weiterer Weitergabe erneut prüfen.

## Auswertung

- `tools/btsnoop_summary.py`: 88.915 Records, 84.563 ACL, 3.091 vollständige ATT-Kandidaten, 222 Commands, 4.130 Events; `sufficient_for_att_analysis=true` (8/8 Python-Tests grün).
- Logcat nur zählend: 2.160 `onWriteCharacteristic`, 34 `GATTC_Discover`.
- Offline-Replay (`tools/garmin_hci_replay.py`, nur Metadaten): 17.119 L2CAP-PDUs, 17.093 ATT-PDUs, 5 Connection-Epochen (6 mit ATT), 21 Garmin-ML-Characteristics, 4.752 Notifications auf `6a4e2810`, 41 unmatched ML-Kandidaten, 18 Ambiguitäten; keine strict ML-Registrierungspaare.
- **Grenzen:** Der Mitschnitt belegt umfangreichen Garmin-Connect-BLE-Verkehr, aber **keine** vollständigen FIT-Dateien, keine FIT-CRC und keinen Datenbankimport. Er beweist insbesondere **nicht**, dass das Band „den ganzen Tag" gespeichert hat; dafür wären FIT-Dateien/Zeitstempel aus Garmin Connect nötig. `coverage=device-history-unverified` bleibt unverändert. Diese Fixture ist kein Nachweis für Produktivdecoder oder Sync-Vollständigkeit.

## Nutzungsgrenze

Nur für die lokale/reproduzierbare Forschung im Projektrahmen; keine Weitergabe an Dritte ohne erneute Prüfung. Es werden keine Rechte an fremden Daten übertragen.
