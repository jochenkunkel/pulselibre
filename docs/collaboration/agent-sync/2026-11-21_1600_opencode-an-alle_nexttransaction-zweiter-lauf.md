# Ergebnis: ein zusätzlicher nextTransaction-No-Pull-Versuch

**Von:** OpenCode · **An:** alle · **Datum:** 2026-11-21 16:00 UTC  
**Bezug:** Scope `2026-11-21_1517_opencode-an-alle_nexttransaction-gate-extension.md`, PR #378, Ergebnisbericht `docs/protocol/device-tests/CIRQA_NEXT_TRANSACTION_PROBE_RESULT_2026-11-21.md`.

## Ergebnis
- Nach Full-CI auf Branch und aktuellem Main wurde PR #378 gemergt (`b4e0ba2`); Full-CI auf dem Merge-Commit bestand.
- Installierte und freigegebene Main-APK verwendeten dasselbe Signaturzertifikat; Version wurde aktualisiert (`1667→1680`) ohne Deinstallation.
- Der eine zusätzliche Versuch startete mit 35 Item-List-Einträgen. Genau eine Transaktions-Folgeanfrage antwortete `status=0` mit 0 Einträgen; `session=false`, `nextTransaction=true`. Danach wurde gestoppt.
- `pullSent=false`, keine MLR-Chunks/Bytes, keine UID/ID/Rohdaten gespeichert; Gate jetzt 2/2 verbraucht. Gemeinsames 15/15-Gate unverändert.

## Nachweise und Grenzen
- PR-Full-CI `44919084584`, Dependency Review `44919084558`, Main-Full-CI `44919422926` (Vor-Merge-Head) und `44920440011` (Merge-Commit): erfolgreich.
- Lokale JVM-Unit-Tests und `./tools/ci-local.sh --quick`: Lauf erfolgreich. Die Quick-CI führte dabei auch lokale S25-UI-Akzeptanzchecks aus; zwei unabhängige UI-Prüfpunkte schlugen fehl, Systemeinstellungen wurden laut Bericht wiederhergestellt. Während dieser Checks keine BLE-Befehle.
- Die Status-0/0-Antwort liefert keine Evidenz für eine weitere befüllte Liste oder abrufbare Dateien. Sie rechtfertigt keine automatische Fortsetzung.
- Nutzerfreigabe bezog sich auf genau einen zusätzlichen Lauf. Keine weiteren Geräteversuche sind damit umfasst.

**Status: erledigt.** Keine Übergabe oder Antwort erforderlich.
