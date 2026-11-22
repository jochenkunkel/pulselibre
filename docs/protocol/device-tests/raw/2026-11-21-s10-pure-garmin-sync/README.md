# Rohdaten: reiner Garmin-Connect-Sync am S10, 21.11./22.11.2026

## Freigabe und Umfang

Der Nutzer hat am **2026-11-22 22:14 UTC** ausdrücklich verlangt, die drei im Chat genannten `.hci`-Dateien „gerne bitte auch ins git" zu stellen. Diese Freigabe gilt **konkret für die hier abgelegten drei Dateien** und ergänzt die Projektentscheidung zur synthetischen Forschungsdaten-Freigabe in [`../../../../collaboration/CURRENT_BAND_RAW_DATA_POLICY.md`](../../../../collaboration/CURRENT_BAND_RAW_DATA_POLICY.md). Sie erweitert **keine** Geräte-Sicherheitsfreigabe. Die bereits veröffentlichten Mitschnitte [`../2026-11-22-s10-garmin-full-sync/`](../2026-11-22-s10-garmin-full-sync/) und [`../2026-11-22-s10-after-sync/`](../2026-11-22-s10-after-sync/) bleiben unverändert.

- **Wichtig zur Datierung:** Dies ist **kein neuer Mitschnitt vom 22.11.** Die Aufnahme stammt vom **21.11., 22:56 UTC** (Sync durch Garmin Connect, vom Nutzer ausgelöst); der Bugreport wurde um 23:58 UTC lokal gesichert. Die drei Extraktdateien wurden lediglich um 00:00 UTC (01:00 MEZ) aus diesem Bugreport erzeugt.
- Quelle: `FS/data/log/bt/btsnoop_hci.log` aus dem lokalen Bugreport `bugreport.zip` (21.11., 23:58 UTC). Der Agent war rein passiv und hat keine BLE-/Pairing-/Sync-Kommandos gesendet; HCI-Snoop war bereits aktiv (`bluetooth_hci_log=1`).
- Gerät: Samsung Galaxy S10 (SM-G970F), Android 12; Gerätekennung wird absichtlich nicht dokumentiert.
- Format: BTSnoop Version 1, Datalink 1002 (HCI UART/H4).

## Dateien

| Datei | Bytes | SHA-256 | Records / ACL / ATT-Kandidaten |
|---|---:|---|---:|
| `s10-garmin-sync-20261122.hci` | 833.857 | `bc714a9a834eeb546e92b3959207ba8df607220cd5c56afdaabe61aded45054c` | 13.223 / 6.253 / 5.898 |
| `s10-garmin-sync-delta.hci` | 120.566 | `c8badc795cb5694f8491f6b48949a0f85fe57f01dbb7c46d6025ca99078fc086` | 2.102 / 931 / 866 |
| `s10-garmin-sync-window.hci` | 48.146 | `aa819b79524584c69fbc80554b328c4fa2d77cad8773a50633438a0f4ef2f537` | 714 / 492 / 453 |

- **Gesamt-Log:** vollständiger Extrakt aus dem Bugreport; erweitert den vorherigen Mitschnitt (713.307 Bytes) um 120.550 Bytes, ohne Abweichung im alten Teil.
- **Delta:** der neue Anteil gegenüber dem vorherigen Mitschnitt; enthält auch Bluetooth-Verkehr in den Pausen vor dem Sync.
- **Sync-Fenster:** der letzte Aktivitätsblock ab +940 s relativ zum Delta-Beginn; nur dieses Fenster beschreibt den reinen Garmin-Connect-Sync. Die Abgrenzung ist eine zeitliche Heuristik aus Record-Lücken, kein belegter App-Aktionszeitpunkt.
- Die drei Dateien sind **logisch aus dem Gesamt-Log abgeleitete Teilmengen**, aber keine byte-identischen Prefixe/Suffixe davon: Jeder Extrakt trägt einen eigenen BTSnoop-Header. Sie sind nicht Additions-relevant, sondern erleichtern die gezielte Auswertung des Sync-Fensters.
- `*.hci` wird von der `.gitignore`-Regel `*.log` **nicht** erfasst; die Dateien werden bewusst einzeln versioniert. Es wurde keine globale `.gitignore`-Ausnahme eingeführt.

## Ehrlicher Hinweis zu möglichen sensitiven Inhalten

Vor der Veröffentlichung wurde **nur zählend** geprüft (keine Inhalte in diesem README):

| Datei | URL-ähnliche Folgen | E-Mail-ähnlich | `password` | `token` | MAC-ähnlich |
|---|---:|---:|---:|---:|---:|
| Gesamt | 35 | 0 | 0 | 8 | 0 |
| Delta | 5 | 0 | 0 | 0 | 0 |
| Fenster | 4 | 0 | 0 | 0 | 0 |

Der Nutzer bestätigt, dass die Testdaten nicht von einem Menschen stammen. Wer die Dateien weiterverwendet, sollte sie dennoch als potentiell identitätsrelevant behandeln. Es wurden **keine** Inhalte, Geräteadressen, Tokens oder Roh-Payloads in Dokumentationsdateien übernommen; der zugehörige Bugreport bleibt lokal und ist nicht Teil dieser Veröffentlichung.

## Auswertung

Metadatenbasierter Befund mit `tools/btsnoop_summary.py` (8/8 Python-Tests grün), alle drei Dateien `sufficient_for_att_analysis=true`. Fachliche Auswertung: [`../../CIRQA_S10_GARMIN_PURE_SYNC_HCI_2026-11-22.md`](../../CIRQA_S10_GARMIN_PURE_SYNC_HCI_2026-11-22.md), mit der Korrektur der HCI-Richtung in [`../../CIRQA_S10_GARMIN_GFDI_SEQUENCE_2026-11-22.md`](../../CIRQA_S10_GARMIN_GFDI_SEQUENCE_2026-11-22.md).

Kurzbefund des Sync-Fensters (714 Records): keine SMP-Pairing-Operationen, 5 Connection-Complete, 108 Encryption-Change, 0 Disconnection-Complete, einmal `GARMIN_ML_SERVICE` in der Discovery; ATT 192 Write-Command, 215 Handle-Value-Notification, 17 Read-By-Type-Request, 7 Find-Information-Request. 127 GFDI-Rahmen im ML-Stream, FileAccess-Listen mit `next_transaction=true` und `status=0`. Details und Grenzen stehen in der verlinkten Auswertung.

**Grenzen:** Kein Legacy-FIT-Transfer (`5002`/`5004`/`5011`/`5012`), kein PulsLibre-Importbeleg, keine vollständige Gerätehistorie, kein Beleg für Decoder-Korrektheit. `coverage=device-history-unverified` bleibt unverändert.
