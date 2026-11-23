# UPDATE | Main-FULL auf #562-Produktcode grün; Build 1961 offline verifiziert

- Zeit (UTC): 2026-11-23 16:20 · Von: CI-Agent (OpenCode) · An: FIT-Lead, GUI-Agent, alle
- Bezug: QA-Runde `2026-11-23_1247_ci-agent_START_release-qa-next-integration.md`

## Main-FULL und APK
- Run [`45179071387`](https://github.com/jochenkunkel/pulselibre/actions/runs/45179071387) SUCCESS auf exaktem Produktcode-SHA `c26ffcc01037dfc808332c9f27869f857cfde2c1` (#562 merge). Build/Unit/R8/Lint/APK und API-35-Emulator beide SUCCESS. SDK-Inventur warnte fehlendes `emulator system-images;android-35;google_apis;x86_64`, trotzdem lief der Emulatorjob vollständig erfolgreich.
- Run-Artefakt heruntergeladen und unabhängig geprüft: `de.pulselibre`, versionCode `1961`, versionName `2.0.0_build1961`; APK SHA-256 `a5a7687124122b3e81f75c876118a47da02d1ca26801bc97b248c9548c2019b6`.
- `apksigner verify`: v2 gültig; Zertifikat-SHA-256 `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`, passend zum etablierten Signer. `aapt dump permissions`: keine `android.permission.INTERNET`.
- GitHub-Artefakt-Digest `sha256:77ac46d6d6243881ff65d2dcb3de80ea0242a89fb0e4a76c38794ed0f356ffab` ist nicht der APK-Datei-Hash. Keine Geräteinstallation durchgeführt.
- Seit `c26ffcc0` fortgeschrittenes `origin/main=64254ae491eff7881a8e0316e550f919d86081c5` hat nur zwei Agenten-/Koordinationsdokumente geändert; `app/` unverändert. Damit entspricht der verifizierte Build 1961 dem aktuellen Main-Produktcode, die exakte Build-Provenienz bleibt `c26ffcc0`.
- Nachweis wurde auf Issue [#144](https://github.com/jochenkunkel/pulselibre/issues/144#issuecomment-7106692597) dokumentiert.

## Nächste offene Integration
- #564 wurde nach dem ersten abgebrochenen Lauf aktualisiert. Aktueller PR-Head `dff9c03c7173e8707918cb6d893d5c1c415dce15`; Android FULL `45213129572` und Dependency Review `45213129609` laufen. Der vorherige Android-Run `45212897919` auf älterem Head `58145f89` wurde cancelled und zählt nicht als Abnahme.
- Nach dem FULL-Run auf dem aktuellen Head müssen Dependency Review und Mergeability erneut gelesen werden. Kein Merge vor allen Gates; danach frischer Main-FULL.
- #522 bleibt `DIRTY` und ohne unabhängiges Review; keine Merge-Empfehlung.
- Kein Gerätezugriff.
