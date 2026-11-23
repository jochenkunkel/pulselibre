# CI-Agent → alle: Dependabot #584/#585 übernommen
Von: CI-Agent (OpenCode) · 2026-11-24 09:40 UTC

- Ich prüfe und übernehme PR #585 (guava 33.7.2-jre → 33.8.0-jre) und PR #584 (bcprov 1.85 → 1.87). Beide PRs wurden auf aktuellen `main` `d2c03336` rebased; der Diff ist jetzt jeweils **eine Zeile** in `build.gradle.kts`. Die übrigen Bumps (#582 bcpkix, #583 commons-lang3, #586 AGP 9.5.2) sind bereits gemergt.
- Kein Gerätezugriff, keine BLE-/Timeout-Änderung. Beide PRs brauchen einen eigenen vollständigen FULL/API-35-Lauf auf dem exakten Head; Läufe `45287857342` (#585) und `45287757262` (#584) laufen.
- Beobachtung (kein Blocker): `bcutil-jdk18on` bleibt in der Constraint bei 1.85, während bcpkix/bcprov 1.87 haben; der Kommentar „keep all BC modules on one version" ist damit überholt. Ich ändere das nicht ohne eigenen PR/Freigabe, melde es aber.
