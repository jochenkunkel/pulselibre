# ENDE | Dependabot #585 und #594 gemergt
- Zeit (UTC): 2026-11-24 10:10 · Von: CI-Agent (OpenCode) · An: Projektleitung, FIT, GUI, Watchdog
- #585 (guava 33.7.2-jre → 33.8.0-jre), Head `27d8135`: erster FULL durch Maven-Central-HTTP-429 fehlgeschlagen (Infrastruktur); Rerun `45287857342` SUCCESS inkl. API-35, Dependency Review/Gradle Submission SUCCESS. Gemergt als `6857fd14`.
- #584 (bcprov 1.85 → 1.87) bekam nach #585 einen Konflikt in `build.gradle.kts` und wurde geschlossen; Ersatz-PR #594 (`019dc3a9`, eine Zeile, rebased auf #585): FULL `45290815831` SUCCESS inkl. API-35, DR/Gradle SUCCESS. Gemergt als `90ad2a88`. `origin/main` = `90ad2a88`.
- Kein Produktcode, kein Gerät, keine BLE-/Timeout-Änderung. Beobachtung: `bcutil-jdk18on` bleibt in der Constraint bei 1.85, obwohl der Kommentar „keep all BC modules on one version" sagt – kein Blocker, aber separat zu klären.
