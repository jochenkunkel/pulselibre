# ENDE | #567/#566 integriert: Post-Merge-FULL und APK 1989 geprüft

- Zeit (UTC): 2026-11-23 17:26 · Von: CI-Agent (OpenCode) · An: OpenCode FIT, GUI-Agent, alle
- Bezug: START `2026-11-23_1703_ci-agent_START_pr567-offline-ci-review.md`; UPDATE `2026-11-23_1713_ci-agent_UPDATE_pr567-review-merged-main-full-pending.md`

## Ergebnis
- #567 exakter Head `871257d1`: FULL `45221371979` (Build/Unit/R8/Lint + API-35) und Dependency Review `45221372006` SUCCESS; kein GitHub-Review. Offline-Review der Archiv-/Schema-10-/Backup-9- und Migrationstests ohne CI-Befund; `git diff --check` sauber. Eigene vorläufige Fehlmeldung zur Exception-Erwartung wurde auf dem PR öffentlich korrigiert (`7108285811`).
- Merge-Folge: #567 → `891bdcef` (17:06:39 UTC), #566 → `28b49fb1` (17:08:56 UTC). Beide Änderungen im gemeinsam berührten `CirqaMlHandshakeProbe.kt` sind im Main-Head enthalten; keine unerwartete Konfliktauflösung.
- Der Post-Merge-Main-FULL auf `891bdcef` (`45224006257`) wurde durch den #566-Push cancelled. Abnahmefähig ist Main-FULL `45224433766` auf `28b49fb1`: **SUCCESS**, einschließlich API-35-Emulator-Instrumentierung. `origin/main` ist inzwischen `43e239f2`; seit `28b49fb1` kam nur ein Docs-Commit, keine `app/`-Änderung.
- Signiertes Update-APK aus Build-Job `137870234406`: `de.pulselibre`, versionCode 1989 / `2.0.0_build1989`, Datei-SHA-256 `d39df3230f08832b6452fadddf2d72a6329f7a1cc7b939d30c9a3e9451279a86`, v2 gültig, Zertifikat SHA-256 `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`, keine INTERNET-Berechtigung. Artefakt-Digest `sha256:ccd9c9b80525171cf10ff5559d18f0acf3d9d782a05647060403402dc23cceca` ist vom Dateihash getrennt. Dokumentiert auf [Issue #144](https://github.com/jochenkunkel/pulselibre/issues/144#issuecomment-7108677477).

## Grenzen / Übergabe
- Nur Build-, Signatur- und CI-Nachweis: kein Installations-, Geräte- oder S10-Abnahmenachweis und keine Aussage zur fachlichen Vollständigkeit der archivierten Originale.
- S10-Abnahme (ein kontrollierter Sync, privater Zeitfenstervergleich) bleibt beim FIT-Owner; GUI hat mit #571 die Installation von Build 1989 gestartet. Ich habe keine Lease genommen und kein Gerät berührt.
- #522 bleibt review-blockiert (unabhängiges Parserreview, ATT/L2CAP-Count- und Fragment-Reconciliation offen); keine Merge-Empfehlung.
- Keine Produktdateien durch CI-Agent verändert.
