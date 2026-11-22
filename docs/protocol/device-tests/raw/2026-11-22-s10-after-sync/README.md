# Rohmitschnitt: CIRQA-/Garmin-Sync-Nachlauf am S10, Vormittag (2026-11-22)

## Freigabe und Umfang

Der Nutzer hat **am 2026-11-22** ausdrücklich verlangt, den `btsnoop_hci.log` von heute Vormittag **analog zur Nachmittags-Mitschrift** im Git-Repository bereitzustellen. Begründung: „die daten sind nicht vom menschen und es gibt keine probleme mit privatsphäre und co“. Diese Freigabe gilt **konkret für diesen Mitschnitt** und ergänzt die Projektentscheidung zur synthetischen Forschungsdaten-Freigabe in [`../../../../collaboration/CURRENT_BAND_RAW_DATA_POLICY.md`](../../../../collaboration/CURRENT_BAND_RAW_DATA_POLICY.md), insbesondere deren Regel 5: HCI-/BLE-Traces werden vor einer öffentlichen Veröffentlichung separat geprüft. Sie erweitert **keine** Geräte-Sicherheitsfreigabe. Die Nachmittags-Mitschrift (`../2026-11-22-s10-garmin-full-sync/`, PR #505) bleibt unverändert.

- Auslöser: Der Nutzer stellte einen bereits vorhandenen S10-Bugreport bereit; der Agent wertete ihn **offline** aus. Keine eigenen BLE-/GATT-/FileSync-Kommandos in diesem Mitschnitt.
- Quelle: `FS/data/log/bt/btsnoop_hci.log` aus dem privaten Bugreport `s10-after-sync-20261122.zip`. Der Bugreport und der extrahierte HCI-Log wurden lokal um 07:34 UTC gesichert; danach wurde HCI-Snoop am Gerät wieder ausgeschaltet und der Wert `0` ausgelesen.
- Gerät: Samsung Galaxy S10 (SM-G970F), Android 12 / API 31 (lokal verifiziert).
- Einschränkung der Herkunft: Der Snoop-Schalter war **vor** diesem Mitschnitt bereits `1`; die Datei enthält daher auch frühere Bluetooth-Aktivität und mehrere Verbindungen. Die Zuordnung der unten genannten CIRQA-/FileAccess-Nachrichten zur letzten beobachteten Verbindung ist eine chronologische Offline-Analyse, kein unabhängiger Beweis, welche App jedes Paket ausgelöst hat.

## Dateien

| Datei | Bytes | SHA-256 |
|---|---:|---|
| `btsnoop_hci.log` | 1.006.684 | `8f564f9ba9a1821165d26bd78fcc50c2f45ee651b6a9790dc423577e7b13a2c4` |

- Format: btsnoop, Version 1, Datalink 1002 (Android H4). Aus lokalem Bugreport extrahiert.
- Die Datei wurde **bewusst gegen die `.gitignore`-Regel `*.log` mit `git add -f` versioniert**, weil der Nutzer diesen Rohlog ausdrücklich im Git haben will. Dies ist eine gezielte Ausnahme für diesen Mitschnitt, keine globale Freischaltung von `*.log`.
- Der zugehörige Bugreport (42.985.879 Bytes, SHA-256 `5ff7c6e226851dc623e6cecc4385dd981bb54f32c8c6ea3b6f914568ad29cd17`) und die Analyse-Rohdateien bleiben lokal und sind **nicht** Teil dieser Veröffentlichung.

## Ehrlicher Hinweis zu möglichen sensitiven Inhalten

Vor der Veröffentlichung wurde **nur zählend** geprüft (keine Inhalte in diesem README):

- HCI-Log: 52 HTTP-URL-ähnliche Byte-Folgen, 0 E-Mail-ähnliche Folgen, 0 Treffer für `password`, 8 Treffer für `token` (Bezeichner, nicht zwingend Geheimnisse), 0 MAC-adressähnliche Muster, 162 seriennummernähnliche Byte-Folgen (nicht als verifizierte Identifikatoren geprüft).
- Der Nutzer bestätigt, dass es sich um Dummy-/synthetische Daten handelt. Wer die Datei weiterverwendet, sollte sie dennoch als potentiell identitätsrelevant behandeln und bei weiterer Weitergabe erneut prüfen. Es wurden **keine** Inhalte, Geräteadressen oder Tokens in Dokumentationsdateien übernommen.

## Auswertung

Metadatenbasierter Befund mit `tools/btsnoop_summary.py` (8/8 Python-Tests grün):

- 15.968 Records, 7.376 ACL, 6.940 vollständige ATT-Kandidaten, 2.229 Commands, 6.363 Events; `sufficient_for_att_analysis=true`.
- Aus der lokalen Offline-Analyse der letzten beobachteten Verbindung: GFDI-Dienstregistrierung und MLR-Dienst 8216 akzeptiert; CRC-valide `5043`/`5044`-Antworten; Item-Listen mit Seitengrößen 11, 1, 0, 0 und Status 0; sechs `PullItemDataResponse` mit `result=0`, sechs `SetFlags`-Antworten mit `result=0`. Details und Grenzen: [`../../CIRQA_S10_HISTORY_EVIDENCE_PLAN_2026-11-22.md`](../../CIRQA_S10_HISTORY_EVIDENCE_PLAN_2026-11-22.md) und die lokale `summary.md` des Mitschnitts.

**Grenzen:** Kein Beleg für MLR-Byte-Stream-Vollständigkeit, per-Datei-Bytezahlen, FIT-CRC, Datenbankimport oder vollständige Gerätehistorie. `coverage=device-history-unverified` bleibt unverändert. Diese Fixture ist kein Nachweis, dass Produktivdecoder oder Sync alle Felder korrekt importieren.
