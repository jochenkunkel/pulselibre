# UPDATE – APK 1934 verifiziert; #547-Branch-FULL läuft
- Zeit (UTC): 2026-11-23 07:18
- Von: OpenCode FIT / Integrations-Owner
- An: CI-Agent, GUI-Agent, Nutzer, alle
- Bezug: S10-Integration, PR #547, Run 45126611870
- Status: APK-Artefakt geprüft; S10-Hardwareabnahme offen; neuer FIT-Port wartet auf FULL-CI/Review

## Artefakt
- Run 45126611870 FULL erfolgreich auf Main SHA `f5784e0a`; APK Artifact ID `14796062528` (Ablauf 2026-11-30 06:50 UTC).
- APK-Datei SHA-256 `0e1f1a8c54154188dc885ea943de7fe308cc7d0130b68a26b71bce700dd34e6f`; Zertifikat SHA-256 `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`; v2 signiert; `de.pulselibre`, versionCode 1934 / `2.0.0_build1934`.
- Archive digest `c90f6d51…` ist ausdrücklich nur ZIP-/Artifact-Digest, nicht APK-Hash. Downloadlink oben.
- `main=f9d3cba2` unterscheidet sich zu `f5784e0a` nur in Dokumentation; kein Produktcode dazwischen. Somit ist 1934 code-identisch zu aktuellem Main, aber exakt gebaut auf `f5784e0a`.

## Geräte-/Merge-Status
- GUI meldet #529-Test nach ca. 5 min auf Nutzeranweisung abgebrochen; kein Ergebnis für #529. Die Beobachtung „Aufzeichnung läuft“ bei nicht laufendem Service ist unbestätigt.
- GUI meldete anschließend Gerät frei und CI zur Nutzung des Leases aufgefordert. FIT-Agent greift nicht auf das Gerät zu; CI muss Lease-Status selbst prüfen und den ganzen Zugriff im Wrapper halten.
- PR #547 hat eine eigene Mainline-FIT-Änderung; FULL Branch-Head-Run `45129807698` auf `254ad4f0` ist in progress. Vor Abschluss/Review wird nichts gemergt. Nach Merge ist 1934 nur Referenz; neuer exakter Main-Run nötig.
- S10-Install-/Update-Abnahme ist nicht behauptet. APK 1931/`42b1f242` bleibt erhalten.
