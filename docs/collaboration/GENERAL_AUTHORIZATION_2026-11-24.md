# Generalfreigabe des Owners (24.11.2026)

**Entscheidung der Projektleitung:** Agenten sollen nicht mehr für jede Einzelmaßnahme beim Owner nachfragen. Eine **Generalfreigabe ist erteilt.** Sie gilt für alle aktiven Agenten (FIT/OpenCode, GUI, CI im Rahmen seiner Monitor-Rolle) und bleibt bis zum ausdrücklichen Widerruf des Owners in Kraft.

## Ergänzung des Owners vom 25.11.2026 – FIT-Selbstfreigabe und App-Start
- FIT/OpenCode darf den automatischen History-Download beim App-Start abschalten. Ziel ist ein kontrollierter, explizit ausgelöster manueller Download statt unbeabsichtigter BLE-Synchronisierung bei Installation, Start und Tests. Manueller Download muss erhalten bleiben und wird bei kontrollierten Tests **von FIT selbst über UI/ADB unter S10-Lease** ausgelöst; keine einzelne Rückfrage an den Owner für jeden manuellen Lauf. Änderungen sind mit zielgerichteten Tests zu sichern.
- Für FIT-eigene Änderungen entfällt die verpflichtende Freigabe durch einen zweiten Agenten. FIT führt ein dokumentiertes Self-Review durch und darf nach **grünen verpflichtenden GitHub-CI- und Dependency-Gates** eigenständig nach `main` mergen. Unabhängiges Review bleibt optional und kann bei konkreten Risiken angefordert werden; keine Pflichtwartezeit.
- Technisch durchgesetzte GitHub-Branch-Protection darf nicht umgangen werden. Falls sie ein fremdes Review zwingend fordert, muss die Repository-Regel auf legitimem Weg angepasst oder das Gate erfüllt werden; keine Umgehung durch Admin-Bypass.
- Unverändert: S10/S25-Leases, Gerätesicherheit, Backup/Restore-Schutz, APK-Signaturkontinuität, Datenschutz, Grenzen neuer aktiver BLE-Kommandos sowie ausdrückliche Freigaben für destruktive Aktionen.
- Diese Owner-Entscheidung ersetzt widersprechende ältere Vorgaben über FIT-Zweitreview und vorbehaltene Entscheidungen zur Deaktivierung des App-Start-Auto-Syncs.

## Was das bedeutet
- **Keine Einzelrückfrage** für fachliche, technische und organisatorische Entscheidungen innerhalb der zugewiesenen Zuständigkeit: Umsetzung, Refactoring, Limit-/Timeout-/Budgetänderungen, Tests, Merges nach erfüllten Qualitätsgates, Geräteläufe im Rahmen der Lease-Regeln, Anpassung von Dokumentation und Prozessen.
- Der Agent entscheidet, setzt um und **meldet danach** (START/ENDE im Agenten-Chat, `agent-sync`, PR-Beschreibung). Der Owner wird nur bei den unten genannten Ausnahmen oder bei einer echten, nicht selbst lösbaren Entscheidung gefragt.
- Frühere Formulierungen in Dokumenten wie „nur mit ausdrücklicher Freigabe des Owners", „Einzelentscheidung bleibt erforderlich" oder „Freigabe abwarten" gelten **für alles außerhalb der Ausnahmen** nicht mehr. Historische Berichte bleiben als datierte Evidenz unverändert.

## Was unverändert bleibt (keine Freigabe, sondern Qualitäts- und Koordinationsregeln)
- FULL/API-35, Dependency Review und die weiteren Qualitätsgates; **Ausnahme FIT gemäß Owner-Ergänzung vom 25.11.2026: dokumentiertes Self-Review und eigenständiger Merge nach grünen Gates**; für andere Agenten prüft weiterhin ein unabhängiger Reviewer (Rollenvertrag [AGENT_ROLES_AND_CI_HANDOVER_2026-11-24.md](AGENT_ROLES_AND_CI_HANDOVER_2026-11-24.md)).
- Geräte-Leases ([DEVICE_LEASE.md](DEVICE_LEASE.md)): ein Agent je Gerät, Wrapper nutzen, nach dem Lauf `FREI`. Das ist Koordination, keine Genehmigung.
- Signierte Builds nur mit geprüfter Zertifikatskontinuität; keine Instrumentationstests auf dem S25.

## Ausnahmen (hier fragt der Agent weiterhin den Owner)
Diese Auslegung der Generalfreigabe gilt, bis der Owner sie ausdrücklich erweitert oder einschränkt:
1. **Irreversibles oder destruktives**: Löschen oder Überschreiben realer Nutzerdaten/Backups, Reset, Entkoppeln, Firmwareaktionen, schreibende/löschende Band-Kommandos (z. B. SetFlags, Archive, Delete, `CLOSE_ALL`).
2. **Recht und Sicherheit**: Gesetze, Datenschutz, Lizenzfragen; Veröffentlichung privater Gesundheits-/Geräte-Rohdaten, Seriennummern, Tokens oder Schlüssel.
3. **Geld/Außenwirkung**: Ausgaben, externe Dienste, Veröffentlichungen im Namen des Projekts.
4. **Produktentscheidungen über das Nutzererlebnis**, die der Owner sich vorbehalten hat (z. B. Häufigkeit oder Abschaltbarkeit des Auto-Syncs), solange er nicht entschieden hat.

Im Zweifel, ob etwas unter eine Ausnahme fällt: kurz mit Begründung beim Owner nachfragen, sonst handeln.
