# S10-Abnahme: signierter Main-Build 1939

Stand: 2026-11-23. Der geprüfte Kandidat stammt aus Android-FULL-Lauf
[`45132518466`](https://github.com/jochenkunkel/pulselibre/actions/runs/45132518466)
auf `b784e56cb774ff87d0b382ed5ec6c7355d5b8006`. `main` steht inzwischen auf
`55e9484a4b14d54762487d7ab57ea789cc7f54ff`; der dazwischenliegende Merge #550
enthält ausschließlich Chat-/Übergabedokumentation, keine App- oder Workflowdateien.
Das signierte APK dieses FULL-Laufs ist somit für den unveränderten App-Code des
aktuellen `main`.

## Artefakt

- Paket: `de.pulselibre`
- `versionCode=1939`, `versionName=2.0.0_build1939`, target SDK 35
- APK-SHA-256: `1ef5613657ca2d4c2aa2921bf73301d138d63a09c971aeddbc5cc39e6d019ff5`
- APK-Signatur: `apksigner verify` erfolgreich, APK Signature Scheme v2
- Zertifikat-SHA-256: `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`
- Download: [signiertes APK-Artefakt 14797629394](https://github.com/jochenkunkel/pulselibre/actions/artifacts/14797629394)
- Lokal gesichert außerhalb des Repositories: `/tmp/opencode/pulselibre-s10-acceptance-20261123/pulselibre-main-b784e56-build1939.apk`

## Reproduzierbarer Ablauf

1. GUI-Agent veröffentlicht ENDE/Übergabe; `python3 tools/s25_lease.py status --device s10` muss FREI melden.
2. Gesamte Sitzung in einem Lease-Wrapper halten:
   `python3 tools/s25_lease.py run --device s10 --agent CI-Agent --minutes 25 --task main-build1939-smoke --test-class s10-acceptance -- <Befehle>`.
3. Im Wrapper Geräteinventar lesen und exakt ein S10-Familiengerät auswählen (`SM-G970*` S10e oder `SM-G973*` S10). Das bestätigte Testgerät hier ist ein S10e (`SM-G970F`, Produkt `beyond0lte`). Vorherige Version und Paketdaten **nicht löschen**.
4. Das signierte Artefakt mit `adb install -r <APK>` als Update installieren; Package-Dumpsys auf Version 1939, Startzeit und erhaltene App-Daten prüfen. Danach PulsLibre starten.
5. Vor der UI-Abnahme `dumpsys activity services de.pulselibre` erfassen und sichtbaren Status auf der Geräteseite notieren. UI „Aufzeichnung läuft“ gilt nur dann als korrekt, wenn der Aufzeichnungsdienst aktiv ist; andernfalls reproduzierbarer Fehler, nicht manuell kaschieren.
6. Ohne Start-/Stop-, Sync-, Download- oder Testlabor-Aktionen nur die Bereiche ansehen: Start/Dashboard mit vorhandenen Messwerten oder plausiblen Leerzuständen; Verlauf/FIT-Historie; Gerätereiter und passiver bestehender BLE-Verbindungsstatus. Keine FIT-Vollständigkeit aus einer UI-Sicht ableiten.
7. Für einen gezielten Smoke-Check `tools/s25_acceptance.py` nur mit explizitem S10-Serial und Phasen `preflight,navigation,overview,history,device,background` ausführen. Ausgabe (`--out-dir`) muss außerhalb des Git-Repositories liegen; Screenshots können Gesundheitsdaten enthalten.
8. Aufzeichnungsstart und kontinuierliche Aufnahme separat mit FIT-/GUI-Agent koordinieren. Vor einem Dauerlauf Band-/Garmin-Connect-Auswirkung und Nutzerfreigabe bestätigen. Nach regulärem Stop UI-Status und `dumpsys activity services` erneut prüfen.
9. Nach Testende Lease-Status FREI kontrollieren. Im Bericht nur Version, Build-Commit, Hash, Signatur, Check-Ergebnisse und redigierte Dienstzustände festhalten; keine Screenshots oder Gesundheitswerte committen.

## Ergebnis dieses Durchlaufs

- `main`-FULL 45132518466: Build/Unit/Lint/APK und API-35-Emulator erfolgreich; 118 Python-Regressionstests bestanden.
- Artefakt lokal heruntergeladen; Zertifikat und Paketmetadaten geprüft.
- Der erste Erkennungsversuch filterte irrtümlich nur `SM-G973*` und übersah dadurch das angeschlossene S10e `SM-G970F`; anschließend wurde die Erkennung korrigiert.
- Update und App-Start auf dem S10e wurden unter Lease ausgeführt. Die UI-/Dienstabfrage ist im Nachtrag unten festgehalten.
- Die frühere Beobachtung „Aufzeichnung läuft“ ohne Dienst wurde im geprüften Nach-Update-Zustand nicht reproduziert.

## Nachtrag nach gezieltem Read-only-Check

- S10e `SM-G970F`, Build 1939: Startseite **„Keine Verbindung · Aufzeichnung aus“**; Geräteseite zeigt denselben Zustand; `CirqaContinuousRecordingService` in `dumpsys activity services de.pulselibre` nicht gelistet.
- Der frühere „läuft“-Status wurde in diesem Nach-Update-Check nicht reproduziert. Keine Aussage zu einem Zustand mit aktiver Verbindung/aufzeichnendem Dienst.
- Der vollständige Sammelskript-Lauf war teilweise erfolgreich (Navigation/Dashboard/Zeitraumwahl); zwei Fehler: fehlende Achsen-Zugänglichkeitsbeschreibung und UI-Dump-Fallback in der Geräteseite. Der gezielte, separat lease-geschützte Dump mit korrekter Bounds-Auswertung lieferte danach den Geräte-/Recorder-Status. Der Skriptfehler bleibt Harness-/Abnahme-Follow-up, nicht als Produktfehler klassifiziert.
- Gerät blieb nach allen Wrapper-Läufen ungesperrt. Kein Start der Aufnahme, keine BLE-Aktion.
