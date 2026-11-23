# ENDE | #564 integriert und Post-Merge-QA abgeschlossen

- Zeit (UTC): 2026-11-23 16:58 · Von: CI-Agent (OpenCode) · An: FIT-Lead, GUI-Agent, alle
- Bezug: QA-START `2026-11-23_1247_ci-agent_START_release-qa-next-integration.md`

## Ergebnis und Nachweise
- PR #564 exakter Head `626e101ebdbfba4478a2d6435a96036cdaf70eff`; Android FULL `45216352874` inklusive API-35-Emulator SUCCESS; Dependency Review `45216352825` SUCCESS (mit Scorecard-Warnung `org.jetbrains:annotations` 2.5 < 3, aber bestanden). PR gemergt zu `95c75f1b1f6ac5bb8ab2174d20472ee1c6488cc2`.
- Post-Merge Main-FULL `45219612825` auf exakt `95c75f1b` SUCCESS, einschließlich API-35-Instrumentierung.
- APK aus dem Main-Run offline geprüft: `de.pulselibre`, versionCode 1979 / `2.0.0_build1979`, Datei-SHA `e12073c0a217dc2d2f7d7592d3691d367af7c0b50f601873bf4f41fd7294260d`, v2 gültig, etabliertes Zertifikat SHA-256 `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`, keine INTERNET-Berechtigung. Kein Gerät installiert. Nachweis auf [Issue #144](https://github.com/jochenkunkel/pulselibre/issues/144#issuecomment-7107925240).
- Neue synthetische Freshness-Reproduktion mit JDK 21 gegen gebaute Kotlin-Produktklassen ausgeführt; sie reproduziert den unsicheren Skip bei geänderten Remote-Bytes gleicher Größe. Das ist kein Beweis, dass dieser Defekt die reale fehlende Historie verursacht hat.
- `origin/main` ist inzwischen `4e30cc77290e35750305e22ef0452369363dc17f`; seit Merge-Commit `95c75f1b` kam nur ein Agenten-Chat-Dokument dazu, keine `app/`-Dateien.

## Offen / Übergabe
- #522 bleibt offen/Review-blockiert: keine unabhängige Parserreview-Antwort; die bekannte Reconciliation von ATT/L2CAP-Zählern und Fragmentverhalten ist nicht gelöst. Keine Merge-Empfehlung.
- Kein Gerätezugriff in dieser QA-Runde.
