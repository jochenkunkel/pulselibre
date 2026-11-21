# Agenten-Abstimmung über das Repository

**Verbindlich ab 2026-11-19.** Claude Code, OpenCode, Codex und andere Agenten stimmen sich **nicht über Chat-Texte, die der Nutzer weiterreicht**, sondern **über dieses Verzeichnis im Repository** ab. Der Nutzer muss nur noch sagen: „Lies `docs/collaboration/agent-sync/`.“

## Warum
Chats sind nicht synchronisiert; Nachrichten gehen verloren, werden doppelt bearbeitet oder zwei Agenten reparieren gleichzeitig dasselbe (19.11.2026: rote `main`, zwei parallele Fixes an den Icon-Dateien). Das Repository ist die gemeinsame, versionierte, nachlesbare Wahrheit.

## Ablauf
1. **Vor jeder Arbeit:** `git fetch`, von `origin/main` branchen, dann `INDEX.md` und alle Einträge mit Status `offen` lesen, die an dich gerichtet sind oder deinen Bereich betreffen.
2. **Nachricht schreiben:** neue Datei `YYYY-MM-DD_HHMM_<von>-an-<an>_<thema>.md` (z. B. `2026-11-19_1130_claude-an-opencode_ci-und-dependabot.md`), Absender/Empfänger klein, `an` kann `alle` sein. Eine Nachricht = ein Thema. Bestehende Nachrichten nicht umschreiben; Antworten sind **neue** Dateien (Verweis auf die Datei, auf die geantwortet wird).
3. **In `INDEX.md` eintragen:** eine Zeile pro Nachricht mit Datum, von → an, Thema, Status (`offen` / `beantwortet` / `erledigt` / `überholt`). Wer eine Nachricht beantwortet oder umsetzt, setzt den Status.
4. **Ein-Bearbeiter-Regel:** Bevor du an einem Thema arbeitest, das jemand anderem gehört oder an dem eine offene Nachricht hängt, schreibe kurz eine Nachricht „ich übernehme X“ (oder frage). Keine zweite Reparatur derselben Sache.
5. **Merge:** Abstimmungsnachrichten sind reine Dokumentation, laufen aber wie alles über einen PR und werden erst nach grünem vollem Android-CI-Lauf gemergt (siehe `AGENTS.md`).

## Inhalt einer Nachricht (Mindestformat)
```
# <Thema>
Von: <Agent> · An: <Agent/alle> · Datum/Uhrzeit · Bezug: <Datei/PR/Issue/CI-Lauf>
## Bestätigte Fakten      (mit Commit-/Lauf-/PR-Nummern)
## Empfehlung / Bitte     (konkret, wer macht was)
## Unsicherheiten         (ausdrücklich, nicht verstecken)
## Nicht anfassen         (Dateien/Bereiche in Bearbeitung)
```

## Regeln
- **Fakten und Empfehlungen trennen**; nur nachgewiesene Erfolge behaupten (Lauf-ID, Commit).
- **Aktuelle Testarmband-Rohdaten sind für die interne Zusammenarbeit freigegeben und erwünscht** (siehe [temporäre Forschungsdaten-Freigabe](../CURRENT_BAND_RAW_DATA_POLICY.md)). Agenten dürfen vollständige Rohdaten lokal/zugriffsbeschränkt austauschen und analysieren; in öffentlich sichtbaren Nachrichten weiterhin keine ungeprüft identifizierenden Kennungen, Geheimnisse oder vollständigen Rohdaten publizieren.
- Zuständigkeiten: siehe `AGENTS.md`. Fremde Bereiche nur nach Rückmeldung ändern.
- Dringend-rote `main`: Wer zuerst „ich übernehme den Fix“ als Nachricht pusht, bearbeitet ihn; alle anderen warten oder helfen nur lesend.
