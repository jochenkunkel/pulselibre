# Generalfreigabe des Owners (24.11.2026)

**Entscheidung der Projektleitung:** Agenten sollen nicht mehr für jede Einzelmaßnahme beim Owner nachfragen. Eine **Generalfreigabe ist erteilt.** Sie gilt für alle aktiven Agenten (FIT/OpenCode, GUI, CI im Rahmen seiner Monitor-Rolle) und bleibt bis zum ausdrücklichen Widerruf des Owners in Kraft.

## Was das bedeutet
- **Keine Einzelrückfrage** für fachliche, technische und organisatorische Entscheidungen innerhalb der zugewiesenen Zuständigkeit: Umsetzung, Refactoring, Limit-/Timeout-/Budgetänderungen, Tests, Merges nach erfüllten Qualitätsgates, Geräteläufe im Rahmen der Lease-Regeln, Anpassung von Dokumentation und Prozessen.
- Der Agent entscheidet, setzt um und **meldet danach** (START/ENDE im Agenten-Chat, `agent-sync`, PR-Beschreibung). Der Owner wird nur bei den unten genannten Ausnahmen oder bei einer echten, nicht selbst lösbaren Entscheidung gefragt.
- Frühere Formulierungen in Dokumenten wie „nur mit ausdrücklicher Freigabe des Owners", „Einzelentscheidung bleibt erforderlich" oder „Freigabe abwarten" gelten **für alles außerhalb der Ausnahmen** nicht mehr. Historische Berichte bleiben als datierte Evidenz unverändert.

## Was unverändert bleibt (keine Freigabe, sondern Qualitäts- und Koordinationsregeln)
- FULL/API-35, Dependency Review und die weiteren Qualitätsgates; **niemand gibt eigene Änderungen frei**, ein unabhängiger Reviewer prüft (Rollenvertrag [AGENT_ROLES_AND_CI_HANDOVER_2026-11-24.md](AGENT_ROLES_AND_CI_HANDOVER_2026-11-24.md)).
- Geräte-Leases ([DEVICE_LEASE.md](DEVICE_LEASE.md)): ein Agent je Gerät, Wrapper nutzen, nach dem Lauf `FREI`. Das ist Koordination, keine Genehmigung.
- Signierte Builds nur mit geprüfter Zertifikatskontinuität; keine Instrumentationstests auf dem S25.

## Ausnahmen (hier fragt der Agent weiterhin den Owner)
Diese Auslegung der Generalfreigabe gilt, bis der Owner sie ausdrücklich erweitert oder einschränkt:
1. **Irreversibles oder destruktives**: Löschen oder Überschreiben realer Nutzerdaten/Backups, Reset, Entkoppeln, Firmwareaktionen, schreibende/löschende Band-Kommandos (z. B. SetFlags, Archive, Delete, `CLOSE_ALL`).
2. **Recht und Sicherheit**: Gesetze, Datenschutz, Lizenzfragen; Veröffentlichung privater Gesundheits-/Geräte-Rohdaten, Seriennummern, Tokens oder Schlüssel.
3. **Geld/Außenwirkung**: Ausgaben, externe Dienste, Veröffentlichungen im Namen des Projekts.
4. **Produktentscheidungen über das Nutzererlebnis**, die der Owner sich vorbehalten hat (z. B. Häufigkeit oder Abschaltbarkeit des Auto-Syncs), solange er nicht entschieden hat.

Im Zweifel, ob etwas unter eine Ausnahme fällt: kurz mit Begründung beim Owner nachfragen, sonst handeln.
