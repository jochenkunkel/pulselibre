# REVIEW #595 (P0 Datenerfassung) – GUI-Agent

Von: GUI-Agent (Claude) · An: Owner, CI-Agent, FIT-Agent · Stand: 2026-11-24 06:27 UTC

Auftrag aus `DATA_ACQUISITION_PRIORITY.md`: „GUI-Agent trennt Erfassungsstatus von medizinischer Aussage". Geprüft ist nur die GUI (keine Hardware, keine BLE-Änderung).

## Was schon passt (Code-Stand main)
- **Keine stillen Lücken (Regel 3):** `HistorySyncProgress` zeigt nie 100 % und nie „vollständig" ohne `coverageProven`; ein geleerter Lauf (`runDrained=true`) heißt „abgeschlossen – Abdeckung nicht belegt". Dateizähler „n von m Dateien" kommt aus dem Terminalbericht (`successful=`/`selected=`), Abbrüche erscheinen als „unvollständig". Das deckt sich mit Regel 3 und bleibt unverändert.
- **Keine Löschung in der GUI:** Die App sendet keine Delete-/Archive-/SetFlags-/Reset-Kommandos; der Bestätigungsdialog sagt das ausdrücklich.
- **Darstellung ≠ Persistenz:** Die Wertebereiche im Dashboard (z. B. Herzfrequenz 1–240) filtern nur die Anzeige. Außerhalb liegende Werte werden als „ungültig/Status" markiert, nicht gelöscht (Regel 5).

## Lücken (GUI-seitig)
1. **Kein sichtbarer Erfassungsstatus.** Die App zeigt nur, was als geprüfte FIT-Reihe darstellbar ist (Herzfrequenz, Distanz, Geschwindigkeit). Was erfasst, aber (noch) nicht darstellbar ist – unbekannte Dateitypen, Developer Fields, Rohdateien –, ist für Nutzer unsichtbar. Das kann wie „nicht erfasst" wirken.
2. **Wortwahl.** „ungültig" (`sd_only_invalid`) klingt, als wären Werte verworfen. Besser: „außerhalb des Darstellungsbereichs – gespeichert".
3. **Karten „nicht verfügbar"** (Schlaf, Aktivität, HRV, Temperatur) sagen nicht, ob Rohdaten dazu schon im Archiv liegen.
4. **Lauf-Identität** (FIT-Frage): Lauf-ID und Auslöser (automatisch/manuell) stehen nicht im sichtbaren Status.

## Vorschlag (nur nach Freigabe, GUI-Anteil)
Neue Karte „Erfassung" im Dashboard, strikt getrennt von jeder medizinischen Aussage:
- Dateien: abgerufen / archiviert / importiert / offen (vier getrennte Zähler), je letzter Lauf und gesamt.
- Unbekannte/nicht dekodierte Inhalte: Anzahl, ausdrücklich „gespeichert, nicht ausgewertet".
- Letzter Lauf: Zeit, Auslöser (auto/manuell), Lauf-ID, Ergebnis; Hinweis „Abdeckung nicht belegt", solange kein Beweis vorliegt.
- Keine Gesundheitswerte und keine Bewertung auf der Karte.

## Abhängigkeiten / Bitte an FIT und CI
- **FIT:** Die Karte braucht eine lesbare Quelle für Archiv-/Import-Zähler (Tabelle oder Repository-Methode mit den vier Zählern und Rohdatei-Inventar). Bitte nennen, wo die liegt – ohne die baue ich nichts und erfinde keine Zahlen.
- **CI:** Wenn der Wortlaut „ungültig" geändert wird, betrifft das de/fr/es-Strings und ggf. Tests (`sd_only_invalid`).

## Nicht getan
Keine Codeänderung, keine Hardware, kein Merge von #595 (Owner-PR, Pflichtreview liegt beim CI-Agenten).
