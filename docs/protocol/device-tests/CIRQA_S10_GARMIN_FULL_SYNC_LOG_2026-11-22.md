# S10: vollständiger Garmin-Connect-Sync, lückenlose Mitschrift (2026-11-22)

- Analyst: CI-Agent (OpenCode)
- Auslöser: Nutzer meldete Daten, die noch nie synchronisiert wurden, und bat um einen **kompletten Garmin-Connect-Sync mit lückenlosem Logging**. Der Nutzer aktivierte Bluetooth und löste den Sync **selbst** in Garmin Connect aus; CI-Agent gab vorher ein explizites Go-Signal nach fertiger Vorbereitung.
- Gerät: Samsung Galaxy S10 (SM-G970F, Android 12), Garmin Connect 5.31.1. Gerätekennung absichtlich nicht in diesem Dokument; Rohdateien liegen ausschließlich im geschützten 0700/0600-Ordner außerhalb des Git-Repositories.
- Rolle des Agenten: **rein passiv**. HCI-Snoop-Systemschalter gesetzt und Logcat-Mitschrift gestartet; **keine** BLE-/GATT-/FileSync-/Pairing-/Reset-/Entkopplungs-Kommandos, kein `CLOSE_ALL`, keine Änderung am Transportverhalten.
- Abstimmung: agent-sync-Nachricht an OpenCode/FIT-Agenten wegen dessen aktiver Session „Garmin Connect CIRQA synchronization" (`2026-11-22_1651_ci-agent-an-fit-agent_garmin-sync-passive-mitschrift.md`).

## 1. Aufnahme-Fenster

| Größe | Wert |
|---|---|
| Aufzeichnung Start (UTC) | 2026-11-22 16:50:16 |
| Aufzeichnung Ende (UTC) | 2026-11-22 16:53:04 |
| HCI-Snoop vor / nach | `0` → `1` (verifiziert vor Sync) |
| Bluetooth bei Vorbereitung | aus (`bluetooth_on=0`) |
| Logcat-Zeilen / -Bytes | 11.562 / 1.489.144 |
| Logcat-Puffer | alle (`-b all`), Format `threadtime` |

## 2. Logcat-Befunde (nur Zählungen und Zeitpunkte)

- Bluetooth-/GATT-bezogene Zeilen: **1.947**, davon `BtGatt.GattService` **763**, `BluetoothAdapter` 112, `bt_stack` 71, `BluetoothAdapterService` 46, `BluetoothRemoteDevices` 46, `BluetoothManagerService` 43.
- Garmin-bezogene Zeilen: **338** (u. a. `WindowManager`, `SurfaceFlinger`, `PowerManagerService` wake-state, `ConnectivityService`).
- Aktivitätskonzentration: 1.913 Bluetooth-/GATT-Zeilen und 322 Garmin-Zeilen in der **Minute 17:52** lokaler Gerätezeit (= 16:52 UTC), damit mitten im Aufzeichnungsfenster.
- Häufigste `BtGatt`-Signalnamen (nur Bezeichner, keine Argumente, keine Adressen): `onWriteCharacteristic` 726, `isCongested` 363, `configureFilterParamter` 20, `onFoundTimeout`/`onLostTimeout` je 10, `onFoundCount` 10, `numOfTrackingEntries` 10.
- Auswertung ausschließlich über Bezeichnerhäufigkeiten; **keine** Paketinhalte, Geräteadressen, Pairing-Tokens, Transaktions-Tokens oder Gesundheitswerte dokumentiert.

## 3. HCI-Snoop-Auswertung (metadatenbasiert)

- Quelle: Bugreport-Extrakt `FS/data/log/bt/btsnoop_hci.log`, damals 5.522.737 Bytes, Datalink 1002. Die Rohdatei wurde auf Nutzeranweisung vom 24.11.2026 aus dem aktuellen Dokumentationsbaum entfernt.
- Validator `tools/btsnoop_summary.py` (8/8 Python-Tests grün): **85.154 Records** (69.470 ACL, 11.066 Events, 4.618 Commands), **36.714 vollständige ATT-Kandidaten**.
- Das Log ist – wie im Präzedenzfall – eine fortlaufende Aufzeichnung; der Nutzer-Sync liegt am Ende. Die zeitliche Eingrenzung des exakten Sync-Fensters aus dem HCI-Log allein ist eine Heuristik, kein Beleg.
- Der Validator weist ausdrücklich darauf hin, dass Gerätezuordnung und Protokollsemantik aus den Metadaten **nicht** belegt sind.

## 3a. Rohdatenbereitstellung (ausdrückliche Nutzerfreigabe)

Der Nutzer hatte die Rohinhalte dieses Tests **ausdrücklich** zur Veröffentlichung im Git freigegeben („es sind dummydate!!!!! ich gebe es frei"). Die damaligen Capture-Artefakte wurden auf spätere Nutzeranweisung vom 24.11.2026 aus dem aktuellen docs-Baum entfernt; die hier dokumentierten aggregierten Befunde bleiben erhalten. Die historische Freigabe ändert diese Entfernung nicht und gilt nicht als generelle Regeländerung.

## 4. Einordnung und Grenzen

- Die Logcat-Zählungen belegen rege GATT-Schreibaktivität im erwarteten Zeitfenster, **keinen** vollständigen FIT-Transfer und **keinen** PulsLibre-Importbeleg.
- Wie im Präzedenzfall `CIRQA_S10_GARMIN_PURE_SYNC_HCI_2026-11-22.md` bleibt die FIT-Ebene in HCI-Logs nicht als eigener Kanal sichtbar; belastbare Aussagen zur Gerätehistorie setzen die HCI-Auswertung voraus.
- `coverage=device-history-unverified` bleibt unverändert. Keine Produktänderung aus diesem Mitschnitt abgeleitet.
- Rein offline; keine aktiven Geräteaktionen über die passive Mitschrift hinaus. Keine Rohdaten, Gerätekennungen, Tokens, Secrets oder Gesundheitswerte in GitHub.
