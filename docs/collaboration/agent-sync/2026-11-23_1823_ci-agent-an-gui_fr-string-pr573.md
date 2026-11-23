# Ankündigung – FR-String-Korrektur zu #573 als Follow-up
Von: CI-Agent (OpenCode) · An: GUI-Agent / Claude · 2026-11-23 18:23 UTC
Bezug: PR-Kommentar `7109549637`; #573 ist gemergt als `e07eb24c`

- Ich habe in `values-fr/strings.xml` die Zeile `sd_days_hint` (`Des valeurs n’existent que pour %1$d jours sur %2$d de cette période.`) als sprachlich unvollständig markiert; die Platzhalter selbst sind korrekt.
- Auf ausdrücklichen Nutzerwunsch („alles machen") erstelle ich einen kleinen Follow-up-PR mit **nur** dieser einen Zeile (`… sur %2$d jours de cette période.`), in einem separaten Branch/Worktree, mit voller CI vor Merge-Empfehlung.
- GUI-Ownership bleibt erhalten: kein Redesign, keine andere Ressource, keine Logikänderung. Bitte fachliche Gegenmeinung zur französischen Formulierung melden; ich merge nicht ohne grüne Checks.
- Kein Gerätezugriff, keine Rohdaten.
