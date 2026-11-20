# S25-Sammelabnahme aller handygebundenen offenen Tickets (Plan und Ablauf)

Stand 2026-11-20 · Verantwortlich: Claude Code (GUI) · Werkzeug: [`tools/s25_acceptance.py`](../tools/s25_acceptance.py) (offline getestet: `tools/test_s25_acceptance.py`)

**Zweck:** Alles, was offene Tickets noch am Galaxy S25 brauchen, in **einem Zug** (ca. 12–15 Minuten Gerätezeit) prüfen, statt über den Tag verteilt Handyzeit zu verbrauchen. Das Skript erzeugt einen Bericht mit PASS/FAIL/INFO und lokale Screenshots; was nur der Nutzer beurteilen kann, steht als „MANUELL“ am Ende.

## Vorbereitung (vom Nutzer, ca. 2 Minuten)
1. **Handy frei:** OpenCode meldet über `agent-sync`, dass der FIT-Workflow-Test beendet ist. Währenddessen niemand sonst am adb.
2. USB-Kabel, USB-Debugging an, **Bildschirm entsperrt**, Display-Timeout auf mindestens 10 Minuten.
3. Akku ≥ 50 % (der Lauf macht keine Akkumessung, die Aussage „lädt“ wird nur als Info berichtet).
4. Aktuelle signierte `main`-App installiert (macht Claude Code vor dem Lauf mit `adb install -r`, Daten bleiben erhalten).
5. Falls ein Emulator mit am adb hängt: egal, das Skript arbeitet nur mit der angegebenen Seriennummer (`RFGL229VWAF`).

## Start
```bash
python3 tools/s25_acceptance.py --serial RFGL229VWAF          # Ausgabe landet in ~/s25-acceptance/<Zeitstempel>/
python3 tools/s25_acceptance.py --serial RFGL229VWAF --dry-run # nur den Plan anzeigen
python3 tools/s25_acceptance.py --serial RFGL229VWAF --phases navigation,history   # einzelne Phasen
```
Der Ausgabeordner muss **außerhalb des Repositorys** liegen (Screenshots enthalten echte Gesundheitswerte); das Skript verweigert sonst den Start.

## Sicherheitsregeln (technisch erzwungen)
- Nur Befehle einer **Erlaubnisliste** erreichen das Handy: Tippen/Wischen/Tasten, Screenshot, UI-Dump, `dumpsys` (nur lesen), `logcat -b crash`, App-Start, Nachtmodus/Schrift/Dichte. **Nie:** Installieren/Deinstallieren, Daten löschen, Instrumentationstests, Bluetooth-/Dienst-Befehle, Texteingabe.
- Das Skript **tippt nie** auf Beschriftungen mit Geräte-/Datenwirkung (Historie herunterladen, Synchronisieren, Löschen, Entkoppeln, Teilen/Export, Diagnosepaket, Inventur, Scan, Verbinden/Trennen). Es öffnet das Testlabor nur (3-s-Druck) und schließt es wieder, ohne etwas darin auszulösen.
- Nachtmodus, Schriftgröße und Display-Dichte werden **am Ende immer** zurückgesetzt (auch bei Fehlern) und im Bericht bestätigt.
- Rohbytes aus „Beispielbytes ansehen“ werden **nicht** gespeichert: das Skript prüft nur Warnung und „Abbrechen zeigt nichts“, tippt nie auf „Anzeigen“.

## Phasen, Tickets und Kriterien
| Phase | Prüft | Ticket | Bestanden, wenn |
| --- | --- | --- | --- |
| preflight | App installiert, Release nicht debuggbar, kein Backup-Flag, **keine** INTERNET-/Standort-/Speicher-Berechtigung, Bildschirm an/entsperrt, Akku-Info | #6 | alle Punkte ok; sonst Abbruch mit Hinweis |
| navigation | alle fünf Tabs erreichbar, kein Absturz | alle | 5/5, `logcat -b crash` ohne PulsLibre |
| overview | Kacheln Herzfrequenz, SpO₂, Schlaf, Nacht-Insights, Schritte; **Abdeckung in Kachel = Begründung** | #312 | alle sichtbar, Prozentwerte gleich |
| history | 24 h/7 T/30 T wählbar; Screenreader-Achse mit Datum; Verteilungsdiagramm mit Zusammenfassung | #309, #6 | Achse „dd.MM. HH:mm bis dd.MM. HH:mm“ |
| analysis | Analysen-Seite, Status „Noch nicht verfügbar“, Aktivitäts-Atlas auffindbar (Screenshot) | #41 | Texte da; Atlas-Screenshot zur Sichtprüfung |
| customize | Startseite anpassen: Dialog, SpO₂ aus → weg, Zurücksetzen → wieder da | #267 | alle 3 Schritte |
| device | Verbindungsstatus; Batteriestand (Info); 3-s-Druck öffnet Testlabor; unbekannte Felder: Liste, Warnung vor Beispielbytes, Abbrechen zeigt keine Bytes | #159, #172, #204 | Status da, Sheet öffnet, Warnung erscheint, keine Hex-Folge nach Abbrechen |
| variants | **Hell**, **Schrift 1,3**, **Schrift 2,0**, **320 dp** (Dichte 540) je Übersicht/Verlauf/Analysen/Gerät; Screenshots + Überlappungs-Heuristik | #276, #204 | kein Absturz; Heuristik-Treffer sind nur Hinweise → Screenshots ansehen |
| background | 90 s App im Hintergrund; Dienststatus (Vordergrunddienst ja/nein) und Absturzfreiheit | #171, #25, #3 | kein Absturz; Dienststatus wird als **Info** dokumentiert (Beleg für die Architekturtickets) |

## Manuell mit dem Nutzer (ca. 5 Minuten, am Ende des Laufs)
1. **TalkBack** durch Feldliste und Beispielbytes-Warnung (#204): vorlesen, Doppeltippen, Schließen. Keine Screenshots der Rohbytes.
2. Beispielbytes **einmal bewusst** anzeigen: es erscheint nur „Datei 1 …“, nie Dateiname oder Hash (#204).
3. Fitness-Ansicht/Ruhepuls-Trend inhaltlich plausibel (#41): nur der Nutzer kennt die Realität.
4. Gestaltungswunsch Geräteseite (Batterie/Status) (#159/#172).
5. Screenshots im Ausgabeordner durchsehen (Abschneiden, Kontrast).

## Auswertung und Ticketschluss
- **#204:** schließen, wenn `device`/`variants` PASS (Warnung, kein Leak) und die manuellen Punkte 1–2 ok sind. Phase C bleibt Follow-up bei OpenCode.
- **#41:** Abnahme der Fitness-Ansicht, wenn `analysis` PASS und Punkt 3 ok; sonst konkrete Mängel ins Ticket.
- **#159, #172:** nicht schließbar, solange die Batterie-Quelle (OpenCode) bzw. der Trace-Recorder fehlt; der Lauf liefert den aktuellen Stand als Beleg.
- **#171, #25, #3:** keine Schließung; der Hintergrundbefund (Dienststatus, keine Abstürze) wird als Evidenz ins Ticket geschrieben.
- **#6:** erneute Geräte-Evidenz (Flags/Berechtigungen) aus `preflight`.
- Jeder FAIL wird im Bericht mit Detail geführt und als Ticket-Kommentar bzw. Fehlerticket nachgezogen; der Bericht selbst wird **nicht** eingecheckt.

## Nicht im Lauf (bewusst)
Alles mit BLE-Aktion (Historien-Download, Sync, V2x-Versuche), Instrumentationstests, Akku-Messung (braucht mehrere Stunden ohne Ladekabel), Schlaf-/HRV-Analysen (keine Daten), Phase-C-Erfassung (OpenCode).
