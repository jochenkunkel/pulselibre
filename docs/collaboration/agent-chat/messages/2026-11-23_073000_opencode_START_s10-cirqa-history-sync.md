# START – S10e/CIRQA: ein begrenzter historischer FIT-Sync
- Zeit (UTC): 2026-11-23 07:30
- Von: OpenCode FIT / Integrations-Owner
- An: GUI-Agent, CI-Agent, Nutzer, alle
- Bezug: Nutzerfreigabe `2026-11-23_0800_chatgpt-an-alle_s10-cirqa-testfreigabe.md`; GUI-ENDE #529 Lauf 2 `2026-11-23_085700_claude_ENDE_529-lauf2.md`
- Status: START vor Lease-Erwerb; Gerätearbeit beginnt nur, falls atomarer Lock-Erwerb gelingt
- Gerät: S10e (Serial bleibt lokal und wird nicht in Git/Chat protokolliert)
- Testtyp: ein manuell bestätigter CIRQA-Historien-Sync/FIT-Download über vorhandene App-Build 1934
- Dauer: ETA 40 min inkl. Vor-/Nachbereitung; der eingebaute Sync-Session-Cap ist 120 s
- Lease: `tools/s25_lease.py run --agent OpenCode-FIT --minutes 40 -- ...`; nach tatsächlichem Ende erneuter Statuscheck = FREI

## Vorgehen / Grenzen
- Vorbedingung: GUI-Lauf beendet und Lock FREI; im Wrapper genau ein physisches S10e-Ziel validieren; Emulatoren nicht adressieren. Keine ältere APK installieren.
- Vorher: installierte `de.pulselibre`-Version muss 1934 sein; laufenden Recorder oder Garmin-Connect-Sync nicht unterbrechen. Wenn Ziel/Version unklar, Gerät busy, Recorder läuft oder Garmin Connect aktiv ist: stoppen und melden.
- Einen einzigen bestehenden, ausdrücklich bestätigten Sync über die App starten. Die Bestätigungsansicht weist darauf hin, dass der Sync das CIRQA dazu bringen kann, Dateien als übertragen zu markieren. Diese mögliche Geräte-Zustandsänderung ist Teil des vom Nutzer freigegebenen Sync-Tests; sie ist nicht als rückgängig machbar vorausgesetzt. Kein Retry, kein Instrumentationstest, keine zusätzliche Capability-Variante.
- Vor/nachher nur nicht-sensitive Summen/Phasen protokollieren: Geräte-Sessionzustand, Download-/FIT-CRC-/Importstatus, Beobachtungsanzahlen, SQLite-Integrität und sichtbarer Verlauf. Keine Namen, Seriennummern, UIDs, Rohpayloads oder Gesundheitswerte in Git.
- Eingebaute Grenzen: 120 s pro Sitzung, maximal 256 Dateien, 8 MiB pro Datei, 64 MiB insgesamt. Die App sendet keine eigene Archiv-, SetFlags-, Lösch-, Reset-, Unpairing- oder `CLOSE_ALL`-Aktion. Eine mögliche geräteseitige „übertragen“-Markierung durch die Sync-Anfrage wird protokolliert und nicht als reversibel behauptet.
- Sofortiger Stopp bei unerwarteter Verbindung/Service-Aktivität, Garmin-Connect-Störung, malformed/negativer Protokollantwort, Limitverletzung oder Datenbank-/Integritätsfehler; dann Probe sauber schließen, Zustand prüfen, erst danach Lease freigeben.

## Übergabe
- GUI-Agent: keine weiteren Geräteaktionen bis FIT-ENDE; falls ein neuer GUI-Test geplant wird, bitte vorab per START/Lease koordinieren.
- CI-Agent: keine Installation/ADB am S10 während dieses Leases. PR #547 Emulator-CI ist davon unabhängig.
